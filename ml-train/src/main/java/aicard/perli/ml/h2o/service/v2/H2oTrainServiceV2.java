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
 * V2 고도화 데이터 기반 Uplift 모델 학습 및 MOJO 아카이브 생성을 담당하는 서비스 클래스입니다.
 * <p>
 * 주요 고도화 특징:
 * <ul>
 * <li>UpliftDRF: H2O 전용 업리프트 알고리즘을 사용하여 증분 효과를 직접 최적화합니다.</li>
 * <li>Data Binarization: 연속형 타겟 점수를 이진 분류 형태로 변환하여 모델 학습 호환성을 확보합니다.</li>
 * <li>Distributed Computing: 전용 MRTask 클래스를 활용하여 대용량 데이터의 병렬 전처리를 수행합니다.</li>
 * <li>Manual MOJO Assembly: 리플렉션을 통해 프레임워크 내부 결함을 우회하여 안정적으로 모델을 저장합니다.</li>
 * </ul>
 * </p>
 */
@Slf4j
public class H2oTrainServiceV2 {

    /**
     * 데이터 전처리를 위한 분산 처리용 내부 클래스입니다.
     * <p>
     * H2O 클러스터의 각 노드에 코드를 전송하여 실행하기 위해 정적(static) 클래스로 정의되었으며,
     * 로열티 점수(target)가 0보다 큰 경우를 긍정 반응(1)으로 정의하여 이진화 작업을 수행합니다.
     * </p>
     */
    public static class TargetBinarizer extends MRTask<TargetBinarizer> {
        /**
         * H2O 프레임의 각 청크(Chunk)를 순회하며 데이터를 변환합니다.
         * <p>MapReduce 아키텍처를 기반으로 노드별 로컬 데이터 청크를 실시간으로 가공합니다.</p>
         * @param cs  입력 데이터 청크 배열 (index 0: 원본 target 값)
         * @param ncs 출력 데이터 청크 배열 (index 0: 0 또는 1로 변환된 값)
         */
        @Override
        public void map(Chunk[] cs, NewChunk[] ncs) {
            Chunk c = cs[0];
            NewChunk nc = ncs[0];
            for (int i = 0; i < c._len; i++) {
                // target > 0 이면 1 (구매/반응), 아니면 0 (미반응) - 임계값 기반 이진화
                nc.addNum(c.atd(i) > 0.0 ? 1 : 0);
            }
        }
    }

    /**
     * V2 고도화 데이터셋을 학습하여 최적의 Uplift 모델을 생성하고 물리 파일로 저장합니다.
     * <p>
     * 데이터를 파싱하고 MRTask를 통한 분산 이진화를 거친 뒤, KL Divergence 메트릭을 적용한
     * UpliftDRF 알고리즘을 가동하여 인과 효과가 극대화된 모델을 빌드합니다.
     * </p>
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

            // target 컬럼 이진화 (MRTask 활용 - 분산 병렬 연산)
            Vec targetVec = fr.vec("target");
            Frame inputFrame = new Frame(targetVec);

            // 전용 Task를 호출하여 클러스터 환경에서 병렬 변환 수행
            Frame resultFrame = new TargetBinarizer().doAll(new byte[]{Vec.T_NUM}, inputFrame).outputFrame();
            Vec binarizedTarget = resultFrame.anyVec();

            // 데이터 구조 재구성 및 타입 캐스팅
            fr.replace(fr.find("target"), binarizedTarget);
            fr.replace(fr.find("target"), fr.vec("target").toCategoricalVec());
            fr.replace(fr.find("is_recommended"), fr.vec("is_recommended").toCategoricalVec());

            // 변경 사항을 H2O 전역 키 저장소(DKV)에 업데이트하여 영속화
            DKV.put(fr);
            log.info("데이터 구조 최적화 완료");

            // 인과 추론 고도화 설정
            UpliftDRFParameters params = new UpliftDRFParameters();
            params._train = fr._key;
            params._response_column = "target";           // 반응 여부 (0/1)
            params._treatment_column = "is_recommended";   // 추천 여부 (0/1)
            params._ignored_columns = new String[]{"card_id", "first_active_month"};

            // 고도화 피처의 복합적 관계 학습을 위한 하이퍼파라미터 튜닝
            params._ntrees = 150;    // 앙상블 트리 개수 확대
            params._max_depth = 15;  // 트리 깊이 고도화
            params._seed = 777;      // 재현성을 위한 시드값

            // Kullback-Leibler Divergence 적용: 처치군과 통제군 간의 확률 분포 차이를 극대화
            params._uplift_metric = UpliftDRFParameters.UpliftMetricType.KL;

            log.info("UpliftDRF 모델 학습 가동");
            UpliftDRF job = new UpliftDRF(params);
            UpliftDRFModel model = job.trainModel().get();
            log.info("모델 빌드 성공. 아카이빙 단계로 진입합니다.");

            // 모델 저장 (리플렉션을 통한 수동 MOJO 조립)
            saveMojoManually(model);

        } catch (Exception e) {
            log.error("학습 도중 치명적 예외 발생: {}", e.getMessage());
            e.printStackTrace();
        } finally {
            Scope.exit();
        }
    }

    /**
     * H2O 프레임워크의 MOJO 저장 경로 결함을 우회하여 ZIP 아카이브를 수동으로 조립합니다.
     * <p>
     * Reflection 기술을 사용하여 비공개 필드에 직접 접근하며, 모델 데이터와 메타데이터를
     * ZIP 스트림에 순차적으로 기록하는 로우레벨 직렬화를 수행합니다.
     * </p>
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

            // MOJO 생성에 필요한 내부 객체 의존성 강제 주입 (캡슐화 우회)
            setFieldValue(mojoWriter, "targetdir", "");
            setFieldValue(mojoWriter, "zos", zos);

            // 내부 비공개 직렬화 메서드 순차 실행
            invokeMethod(mojoWriter, "addCommonModelInfo");
            invokeMethod(mojoWriter, "writeModelData");
            invokeMethod(mojoWriter, "writeModelInfo");
            invokeMethod(mojoWriter, "writeDomains");

            zos.finish();
        }
        log.info("고도화 모델 파일 생성 완료: {}", savePath);
    }

    /**
     * 객체의 캡슐화된 필드에 접근하여 강제로 값을 설정합니다.
     * <p>상속 계층 구조를 순회하며 타겟 필드를 탐색하여 접근 제어자를 무력화합니다.</p>
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
     * 객체의 캡슐화된 비공개 메서드를 강제로 실행합니다.
     * <p>MOJO Writer 내부의 핵심 기록 로직을 수동으로 호출하기 위해 사용합니다.</p>
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