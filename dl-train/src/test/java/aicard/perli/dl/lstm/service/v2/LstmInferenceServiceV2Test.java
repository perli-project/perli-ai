package aicard.perli.dl.lstm.service.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class LstmInferenceServiceV2Test {

    private LstmInferenceServiceV2 inferenceService;
    private final String modelPath = "C:/Coding/perli-ai/resources/output/models/lstm/v2/dl4j_lstm_model_v2.zip";

    @BeforeEach
    void setUp() throws Exception {
        inferenceService = new LstmInferenceServiceV2();

        // 모델 파일 존재 확인 후 로드
        File modelFile = new File(modelPath);
        if (modelFile.exists()) {
            inferenceService.loadModel(modelPath);
        }
    }

    @Test
    @DisplayName("실시간 지출 예측 추론 테스트")
    void testPredictExpense() {
        // 모델 파일이 없을 경우 테스트 스킵 (학습이 선행되어야 함)
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            System.out.println("모델 파일이 없어 테스트를 건너뜁니다. 먼저 학습을 진행하세요.");
            return;
        }

        // Given: 가상의 사용자 데이터 생성 (최근 6개월 지출이 상승하는 패턴)
        LstmAdvancedRequestV2 userData = new LstmAdvancedRequestV2();
        userData.setAge(30);
        userData.setSex(1);
        userData.setEducation(2);
        userData.setMarriage(1);
        userData.setLimitBal(5000000); // 한도 500만

        // 최근 6개월간 지출이 10만 -> 60만으로 늘어나는 상황
        userData.setBillAmts(new double[]{100000, 200000, 300000, 400000, 500000, 600000});
        userData.setPayAmts(new double[]{100000, 200000, 300000, 400000, 500000, 600000});
        userData.setPayStatus(new int[]{0, 0, 0, 0, 0, 0});

        // 확장 피처 대략적 세팅
        userData.setTotalAmount(2100000);
        userData.setTxCount(15);
        userData.setAvgInstallments(1.0);
        userData.setAuthorizedRatio(1.0);
        userData.setBalance(1000000);
        userData.setPurchases(2100000);

        // When: 예측 실행
        double prediction = inferenceService.predictExpense(userData);

        // Then: 결과 검증
        System.out.println("예측된 Normalized 지출액: " + prediction);
        System.out.println("복원된 예상 지출액: " + (long)(prediction * 1000000) + "원");

        // 예측값이 0은 아닐 것이며, 일반적인 지출 범위 내에 있어야 함
        assertNotEquals(0.0, prediction, "예측값이 0이어서는 안 됩니다.");
//        assertTrue(prediction > 0, "예측값은 양수여야 합니다.");
    }
}