package aicard.perli.ml.h2o.service.v1;

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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipOutputStream;

/**
 * H2O GBM 기반 T-Learner 학습 서비스.
 * is_recommended=1/0 데이터를 분리해 treatment/control 모델을 각각 학습합니다.
 */
@Slf4j
public class H2oTrainServiceTLearnerV1 {

    public void train(String sourceDataPath, String treatmentModelPath, String controlModelPath) {
        train(sourceDataPath, treatmentModelPath, controlModelPath, Integer.MAX_VALUE);
    }

    public void train(String sourceDataPath, String treatmentModelPath, String controlModelPath, int maxRows) {
        System.setProperty("h2o.ignore.jdk.version", "true");
        String logDirPath = new File("C:/Coding/perli-ai/resources/output/logs").getAbsolutePath();
        H2O.main(new String[]{"-log_dir", logDirPath});

        File treatmentCsv = new File("resources/processed/h2o/v1/train_treatment_v1.csv");
        File controlCsv = new File("resources/processed/h2o/v1/train_control_v1.csv");
        boolean scopeEntered = false;

        try {
            splitByTreatment(sourceDataPath, treatmentCsv, controlCsv, maxRows);

            Scope.enter();
            scopeEntered = true;
            trainSingle(treatmentCsv.getAbsolutePath(), treatmentModelPath, "treatment_frame_v1");
            trainSingle(controlCsv.getAbsolutePath(), controlModelPath, "control_frame_v1");
            log.info("T-Learner 학습 완료");
        } catch (Exception e) {
            log.error("T-Learner 학습 실패: {}", e.getMessage());
        } finally {
            if (scopeEntered) {
                Scope.exit();
            }
        }
    }

    private void splitByTreatment(String sourcePath, File treatmentCsv, File controlCsv) throws Exception {
        splitByTreatment(sourcePath, treatmentCsv, controlCsv, Integer.MAX_VALUE);
    }

    private void splitByTreatment(String sourcePath, File treatmentCsv, File controlCsv, int maxRows) throws Exception {
        sourcePath = resolvePath(sourcePath).toString();
        if (treatmentCsv.getParentFile() != null) {
            treatmentCsv.getParentFile().mkdirs();
        }

        try (BufferedReader br = new BufferedReader(new FileReader(sourcePath));
             BufferedWriter treatmentBw = new BufferedWriter(new FileWriter(treatmentCsv));
             BufferedWriter controlBw = new BufferedWriter(new FileWriter(controlCsv))) {

            String header = br.readLine();
            if (header == null) {
                throw new IllegalStateException("학습 데이터가 비어 있습니다: " + sourcePath);
            }

            treatmentBw.write(header);
            treatmentBw.newLine();
            controlBw.write(header);
            controlBw.newLine();

            String[] headers = header.split(",");
            int treatmentIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                if ("is_recommended".equals(headers[i])) {
                    treatmentIndex = i;
                    break;
                }
            }
            if (treatmentIndex < 0) {
                throw new IllegalStateException("is_recommended 컬럼이 없습니다.");
            }

            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (count >= maxRows) {
                    break;
                }
                String[] cols = line.split(",");
                if (cols.length <= treatmentIndex) {
                    continue;
                }
                if ("1".equals(cols[treatmentIndex])) {
                    treatmentBw.write(line);
                    treatmentBw.newLine();
                } else if ("0".equals(cols[treatmentIndex])) {
                    controlBw.write(line);
                    controlBw.newLine();
                }
                count++;
            }
        }
    }

    private Path resolvePath(String candidate) {
        Path p1 = Paths.get(candidate);
        if (p1.toFile().exists()) {
            return p1;
        }
        Path p2 = Paths.get("..", candidate);
        if (p2.toFile().exists()) {
            return p2.normalize();
        }
        return p1;
    }

    private void trainSingle(String dataPath, String outputModelPath, String frameKey) throws Exception {
        File dataFile = new File(dataPath);
        NFSFileVec nfs = NFSFileVec.make(dataFile);
        Frame frame = ParseDataset.parse(Key.make(frameKey), nfs._key);

        GBMParameters params = new GBMParameters();
        params._train = frame._key;
        params._response_column = "target";
        params._ignored_columns = new String[]{"card_id", "is_recommended"};
        params._ntrees = 40;
        params._max_depth = 6;
        params._learn_rate = 0.05;
        params._seed = 777L;

        GBM job = new GBM(params);
        GBMModel model = job.trainModel().get();
        saveMojoManually(model, outputModelPath);
    }

    private void saveMojoManually(GBMModel model, String outputModelPath) throws Exception {
        File file = new File(outputModelPath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        Object mojoWriter = model.getMojo();
        try (FileOutputStream fos = new FileOutputStream(file);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            setFieldValue(mojoWriter, "targetdir", "");
            setFieldValue(mojoWriter, "zos", zos);

            invokeMethod(mojoWriter, "addCommonModelInfo");
            invokeMethod(mojoWriter, "writeModelData");
            invokeMethod(mojoWriter, "writeModelInfo");
            invokeMethod(mojoWriter, "writeDomains");
            zos.finish();
        }
    }

    private void setFieldValue(Object obj, String fieldName, Object value) throws Exception {
        Field field = null;
        Class<?> current = obj.getClass();
        while (current != null) {
            try {
                field = current.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        if (field != null) {
            field.setAccessible(true);
            field.set(obj, value);
        }
    }

    private void invokeMethod(Object obj, String methodName) throws Exception {
        Method method = null;
        Class<?> current = obj.getClass();
        while (current != null) {
            try {
                method = current.getDeclaredMethod(methodName);
                break;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        if (method != null) {
            method.setAccessible(true);
            method.invoke(obj);
        }
    }
}
