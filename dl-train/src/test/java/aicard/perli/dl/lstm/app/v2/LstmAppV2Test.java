package aicard.perli.dl.lstm.app.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import aicard.perli.dl.lstm.service.v2.LstmPredictorV2;
import aicard.perli.dl.lstm.util.converter.v2.LstmDataConverterV2;
import aicard.perli.dl.lstm.util.loader.v2.LstmDataLoaderV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.deeplearning4j.nn.conf.layers.FeedForwardLayer;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LSTM V2 모델 및 데이터 파이프라인 검증
 */
class LstmAppV2Test {

    private LstmDataLoaderV2 dataLoader;
    private LstmDataConverterV2 converter;
    private LstmPredictorV2 predictor;
    private final String testCsvPath = "C:/Coding/perli-ai/resources/processed/lstm/v2/train_lstm_v2.csv";

    @BeforeEach
    void setUp() {
        dataLoader = new LstmDataLoaderV2();
        converter = new LstmDataConverterV2();
        predictor = new LstmPredictorV2();
    }

    @Test
    @DisplayName("V2 데이터 로드 및 DTO 매핑 검증")
    void testDataLoaderV2() throws Exception {
        assertTrue(new File(testCsvPath).exists());

        List<LstmAdvancedRequestV2> dataList = dataLoader.loadTrainingData(testCsvPath);

        assertNotNull(dataList);
        assertEquals(30000, dataList.size());

        LstmAdvancedRequestV2 sample = dataList.get(0);
        assertTrue(sample.getAge() > 0);
        assertTrue(sample.getSex() == 1 || sample.getSex() == 2);
    }

    @Test
    @DisplayName("V2 텐서 변환 차원 및 스케일링 검증")
    void testTensorConversionV2() throws Exception {
        List<LstmAdvancedRequestV2> dataList = dataLoader.loadTrainingData(testCsvPath);
        INDArray features = converter.toTrainingTensor(dataList);

        // Shape: [Batch, Feature(16), Time(6)]
        assertEquals(dataList.size(), features.size(0));
        assertEquals(16, features.size(1));
        assertEquals(6, features.size(2));

        double sampleValue = features.getDouble(0, 0, 0);
        assertTrue(sampleValue >= -2.0 && sampleValue <= 10.0);
    }

    @Test
    @DisplayName("V2 모델 입력 레이어 설정 검증")
    void testModelInitialization() {
        predictor.initModel(16, 1);
        assertNotNull(predictor.getModel());

        var layerConf = predictor.getModel().getLayer(0).conf().getLayer();

        if (layerConf instanceof FeedForwardLayer) {
            long nIn = ((FeedForwardLayer) layerConf).getNIn();
            assertEquals(16, nIn);
        } else {
            fail("Invalid layer type");
        }
    }
}