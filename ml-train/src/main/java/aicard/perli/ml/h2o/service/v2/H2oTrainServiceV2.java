package aicard.perli.ml.h2o.service.v2;

import hex.tree.uplift.UpliftDRF;
import hex.tree.uplift.UpliftDRFModel;
import hex.tree.uplift.UpliftDRFModel.UpliftDRFParameters;
import lombok.extern.slf4j.Slf4j;
import water.DKV;
import water.Key;
import water.Scope;
import water.fvec.*;
import water.parser.ParseDataset;
import water.MRTask;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipOutputStream;

/**
 * <p>[V2] 고도화 데이터 기반 Uplift 모델 학습 및 MOJO 아카이브 생성을 담당하는 서비스 클래스입니다.</p>
 * <p>주요 고도화 특징:
 * <ul>
 * <li><b>UpliftDRF:</b> H2O 전용 업리프트 알고리즘을 사용하여 증분 효과를 직접 최적화합니다.</li>
 * <li><b>Data Binarization:</b> 연속형 타겟 점수를 이진 분류 형태로 변환하여 모델 학습 호환성을 확보합니다.</li>
 * <li><b>Distributed Computing:</b> 전용 MRTask 클래스를 활용하여 대용량 데이터의 병렬 전처리를 수행합니다.</li>
 * <li><b>Manual MOJO Assembly:</b> 리플렉션을 통해 H2O 프레임워크의 내부 결함을 우회하여 안정적으로 모델을 저장합니다.</li>
 * </ul>
 * </p>
 */
@Slf4j
public class H2oTrainServiceV2 {

    /**
     * <p>데이터 전처리를 위한 분산 처리용 내부 클래스입니다.</p>
     * <p>H2O 클러스터의 각 노드에 코드를 전송하기 위해 정적(static) 클래스로 정의되었으며,
     * 로열티 점수(target)가 0보다 큰 경우를 긍정 반응(1)으로 정의하여 이진화 작업을 수행합니다.</p>
     */
    public static class TargetBinarizer extends MRTask<TargetBinarizer> {
        /**
         * H2O 프레임의 각 청크(Chunk)를 순회하며 데이터를 변환합니다.
         * * @param cs  입력 데이터 청크 배열 (index 0: 원본 target 값)
         * @param ncs 출력 데이터 청크 배열 (index 0: 0 또는 1로 변환된 값)
         */
        @Override
        public void map(Chunk[] cs, NewChunk[] ncs) {
            Chunk c = cs[0];
            NewChunk nc = ncs[0];
            for (int i = 0; i < c._len; i++) {
                // target > 0 이면 1 (구매/반응), 아니면 0 (미반응)
                nc.addNum(c.atd(i) > 0.0 ? 1 : 0);
            }
        }
    }

    /**
     * <p>V2 고도화 데이터셋을 학습하여 최적의 Uplift 모델을 생성하고 물리 파일로 저장합니다.</p>
     * @param dataPath 전처리된 고도화 CSV 파일의 절대 경로
     */
    public void trainV2(String dataPath) {
        log.info("고도화 학습 프로세스 시작. 데이터 소스: {}", dataPath);
        System.setProperty("h2o.ignore.jdk.version", "true");

        try {
            Scope.enter();
            File f = new File(dataPath);
            log.info("H2O Distributed Parsing 실행 중");
            NFSFileVec nfs = NFSFileVec.make(f);
            Frame fr = ParseDataset.parse(Key.make("train_v2_frame"), nfs._key);

            log.info("병렬 전처리: Target 컬럼 이진화(Binarization) 착수");

            // target 컬럼 이진화 (MRTask 활용)
            Vec targetVec = fr.vec("target");
            Frame inputFrame = new Frame(targetVec);

            // 전용 Task를 호출하여 분산 환경에서 병렬 변환 수행
            Frame resultFrame = new TargetBinarizer().doAll(new byte[]{Vec.T_NUM}, inputFrame).outputFrame();
            Vec binarizedTarget = resultFrame.anyVec();

            // 데이터 구조 재구성 및 타입 캐스팅
            // UpliftDRF는 처치(Treatment)와 반응(Response) 변수가 반드시 Categorical(Enum) 타입
            fr.replace(fr.find("target"), binarizedTarget);
            fr.replace(fr.find("target"), fr.vec("target").toCategoricalVec());
            fr.replace(fr.find("is_recommended"), fr.vec("is_recommended").toCategoricalVec());

            // 변경 사항을 H2O 전역 키 저장소(DKV)에 업데이트
            DKV.put(fr);
            log.info("데이터 구조 최적화 완료");

            // UpliftDRF 알고리즘 하이퍼파라미터 설정
            UpliftDRFParameters params = new UpliftDRFParameters();
            params._train = fr._key;
            params._response_column = "target";           // 반응 여부 (0/1)
            params._treatment_column = "is_recommended";   // 추천 여부 (0/1)
            params._ignored_columns = new String[]{"card_id", "first_active_month"};

            // 고도화 피처의 복합적 관계 학습을 위한 파라미터 튜닝
            params._ntrees = 150;    // 앙상블 트리 개수
            params._max_depth = 15;  // 결정 트리 최대 깊이
            params._seed = 777;      // 재현성을 위한 시드값

            // Kullback-Leibler Divergence: 처치군과 통제군 간의 분포 차이를 극대화하는 메트릭
            params._uplift_metric = UpliftDRFParameters.UpliftMetricType.KL;

            log.info("UpliftDRF 모델 학습 가동");
            UpliftDRF job = new UpliftDRF(params);
            UpliftDRFModel model = job.trainModel().get();
            log.info("모델 빌드 성공 아카이빙 단계로 진입합니다.");

            // 모델 저장 (수동 MOJO 조립)
            saveMojoManually(model);

        } catch (Exception e) {
            log.error("학습 도중 치명적 예외 발생: {}", e.getMessage());
            e.printStackTrace();
        } finally {
            Scope.exit();
        }
    }

    /**
     * <p>H2O 프레임워크의 MOJO 저장 경로 결함을 우회하여 ZIP 아카이브를 수동으로 조립합니다.</p>
     * <p>Reflection 기술을 사용하여 비공개 필드에 직접 접근하며, 모델 데이터와 메타데이터를 순차적으로 기록합니다.</p>
     * @param model 학습이 완료된 UpliftDRFModel 인스턴스
     * @throws Exception 리플렉션 및 스트림 처리 중 발생하는 예외
     */
    private void saveMojoManually(UpliftDRFModel model) throws Exception {
        String savePath = "C:/Coding/perli-ai/resources/output/models/h2o/v2/uplift_drf_model_v2.zip";
        File file = new File(savePath);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        Object mojoWriter = model.getMojo();
        log.info("리플렉션 기반 MOJO 조립 절차 개시");

        try (FileOutputStream fos = new FileOutputStream(file);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // MOJO 생성에 필요한 내부 객체 의존성 강제 주입
            setFieldValue(mojoWriter, "targetdir", "");
            setFieldValue(mojoWriter, "zos", zos);

            // 저장 명세(addCommonModelInfo -> writeModelData -> writeModelInfo -> writeDomains) 준수
            invokeMethod(mojoWriter, "addCommonModelInfo");
            invokeMethod(mojoWriter, "writeModelData");
            invokeMethod(mojoWriter, "writeModelInfo");
            invokeMethod(mojoWriter, "writeDomains");

            zos.finish();
        }
        log.info("고도화 모델 파일 생성 완료: {}", savePath);
    }

    /**
     * <p>객체의 캡슐화된 필드에 접근하여 강제로 값을 설정합니다.</p>
     *
     * @param obj       대상 객체
     * @param fieldName 필드명
     * @param value     설정할 값
     * @throws Exception 필드를 찾을 수 없거나 접근이 불가능할 경우
     */
    private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> current = obj.getClass();
        while (current != null) {
            try { field = current.getDeclaredField(fieldName); break; }
            catch (NoSuchFieldException e) { current = current.getSuperclass(); }
        }
        if (field != null) { field.setAccessible(true); field.set(obj, value); }
    }

    /**
     * <p>객체의 캡슐화된 비공개 메서드를 강제로 실행합니다.</p>
     *
     * @param obj        대상 객체
     * @param methodName 메서드명
     * @throws Exception 메서드 실행 중 발생한 예외
     */
    private void invokeMethod(Object obj, String methodName) throws Exception {
        Method m = null;
        Class<?> current = obj.getClass();
        while (current != null) {
            try { m = current.getDeclaredMethod(methodName); break; }
            catch (NoSuchMethodException e) { current = current.getSuperclass(); }
        }
        if (m != null) { m.setAccessible(true); m.invoke(obj); }
    }
}