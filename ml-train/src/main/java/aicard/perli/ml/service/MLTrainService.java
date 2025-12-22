package aicard.perli.ml.service;

import hex.tree.gbm.GBM;
import hex.tree.gbm.GBMModel;
import hex.tree.gbm.GBMModel.GBMParameters;
import lombok.extern.slf4j.Slf4j;
import water.H2O;
import water.Key;
import water.Scope;
import water.fvec.Frame;
import water.fvec.NFSFileVec;
import water.parser.ParseDataset;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipOutputStream;

/**
 * Gradient Boosting Machine(GBM) 모델의 학습 및 MOJO 생성을 담당하는 서비스 클래스입니다.
 * <p>
 * H2O 엔진의 내장 MOJO 내보내기 기능에서 발생하는 SchemaServer 관련 결함을 우회하기 위해
 * 저수준 리플렉션(Low-level Reflection)을 통한 수동 ZIP 아카이브 조립 방식을 사용합니다.
 * </p>
 */
@Slf4j
public class MLTrainService {

    /**
     * 지정된 데이터 경로의 CSV 파일을 사용하여 AI 모델을 학습시키고 MOJO 파일을 생성합니다.
     * @param dataPath 전처리된 피처 데이터 세트의 물리적 경로 (.csv)
     */
    public void train(String dataPath) {
        // JDK 버전 체크 우회 및 로그 디렉토리 설정
        System.setProperty("h2o.ignore.jdk.version", "true");
        String logDirPath = new File("C:/Coding/perli-ai/resources/output/logs").getAbsolutePath();
        H2O.main(new String[]{"-log_dir", logDirPath});

        try {
            Scope.enter();
            File f = new File(dataPath);
            NFSFileVec nfs = NFSFileVec.make(f);
            Frame fr = ParseDataset.parse(Key.make("train_frame"), nfs._key);

            // GBM 알고리즘 파라미터 설정
            GBMParameters params = new GBMParameters();
            params._train = fr._key;
            params._response_column = "target";
            params._ignored_columns = new String[]{"card_id"};
            params._ntrees = 100;
            params._max_depth = 10;
            params._seed = 1234L;
            params._learn_rate = 0.01;

            log.info("AI 모델 학습 중...");
            GBM job = new GBM(params);
            GBMModel model = job.trainModel().get();
            log.info("학습 완료");

            // 학습 완료 후 수동 MOJO 조립
            forceManualMojoAssembly(model);

        } catch (Exception e) {
            log.error("학습 프로세스 오류: {}", e.getMessage());
        } finally {
            Scope.exit();
        }
    }

    /**
     * H2O 스키마 서버를 우회하여 추론 엔진이 인식 가능한 표준 MOJO ZIP 파일을 수동으로 조립합니다.
     * <p>
     * {@code targetdir} 필드 초기화를 통해 파일명 앞에 'null'이 붙는 현상을 방지하고,
     * {@code AbstractMojoWriter}의 핵심 메서드들을 순차적으로 실행하여 데이터 무결성을 보장합니다.
     * </p>
     *
     * @param model 학습이 완료된 GBM 모델 객체
     */
    private void forceManualMojoAssembly(GBMModel model) {
        String modelPath = "C:/Coding/perli-ai/resources/output/models/gbm_loyalty_model.zip";
        try {
            File modelFile = new File(modelPath);
            if (modelFile.getParentFile() != null) modelFile.getParentFile().mkdirs();

            Object mojoWriter = model.getMojo();
            log.info("MOJO 정밀 조립 시작 (targetdir 필드 수정)");

            try (FileOutputStream fos = new FileOutputStream(modelFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // 필드명이 null일 경우 파일 시스템 상에 "nullmodel.ini" 등으로 생성되는 것을 방지함
                Field dirField = findField(mojoWriter.getClass(), "targetdir");
                dirField.setAccessible(true);
                dirField.set(mojoWriter, "");
                log.info("파일 경로 접두사 초기화 완료.");

                // 부모 클래스의 zos(ZipOutputStream) 필드에 현재 파일 스트림 주입
                Field zosField = findField(mojoWriter.getClass(), "zos");
                zosField.setAccessible(true);
                zosField.set(mojoWriter, zos);

                // MOJO 규격에 맞는 필수 메타데이터 및 바이너리 데이터 기록
                invokeMethod(mojoWriter, "addCommonModelInfo"); // 공통 모델 정보 수집
                invokeMethod(mojoWriter, "writeModelData");     // 핵심 트리 구조 바이너리 기록
                invokeMethod(mojoWriter, "writeModelInfo");     // 수집된 정보 기반 model.ini 파일 생성
                invokeMethod(mojoWriter, "writeDomains");       // 범주형 데이터 도메인 텍스트 기록

                zos.finish();
                fos.flush();
            }
            log.info("MOJO가 생성되었습니다.");

        } catch (Exception e) {
            log.error("MOJO 조립 최종 실패: {}", e.getMessage());

            // 최후의 수단으로 H2O 바이너리 형식(zip 아님) 내보내기 시도
            try {
                model.exportBinaryModel(modelPath.replace(".zip", ""), true);
            } catch (Exception ignored) {}
        }
    }

    /**
     * 클래스 상속 계층을 역추적하여 지정된 이름의 필드를 찾아 반환합니다.
     * * @param clazz 탐색을 시작할 클래스 타입
     * @param fieldName 찾고자 하는 필드명
     * @return 검색된 {@link Field} 객체
     * @throws NoSuchFieldException 최상위 클래스까지 탐색했으나 필드를 찾지 못한 경우
     */
    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try { return current.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { current = current.getSuperclass(); }
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * 상속 계층에 숨겨진 보호된(protected) 또는 비공개(private) 메서드를 찾아 강제로 실행합니다.
     * * @param obj 메서드를 실행할 대상 객체 인스턴스
     * @param methodName 실행할 메서드 이름
     * @throws Exception 메서드를 찾지 못하거나 실행 권한이 없는 경우 발생하는 모든 예외
     */
    private void invokeMethod(Object obj, String methodName) throws Exception {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            try {
                Method m = clazz.getDeclaredMethod(methodName);
                m.setAccessible(true);
                m.invoke(obj);
                return;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
    }
}