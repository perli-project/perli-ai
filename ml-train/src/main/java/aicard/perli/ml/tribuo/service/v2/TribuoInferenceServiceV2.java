package aicard.perli.ml.tribuo.service.v2;

import aicard.perli.ml.tribuo.dto.request.v2.TribuoRequestV2;
import lombok.extern.slf4j.Slf4j;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.Regressor;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * V2 XGBoost 모델을 활용한 고도화 실시간 추론 서비스 클래스입니다.
 * <p>
 * 수혈된 최신 행동 지표 3종(활동성, 금액, 프리미엄 비중)과 기본 지표 3종을 결합하여
 * 카드별 랭킹 점수를 산출하며, 비선형 앙상블 알고리즘을 통해 복합적인 유저 패턴을 해석합니다.
 * </p>
 */
@Slf4j
public class TribuoInferenceServiceV2 {

    /** 메모리에 로드된 Tribuo 기반 XGBoost 회귀 모델 */
    private Model<Regressor> model;

    /**
     * 지정된 경로에서 V2 고도화 모델 파일(.gdpc)을 로드하여 추론 엔진을 초기화합니다.
     * @param modelPath .gdpc 확장자의 학습 완료된 XGBoost 모델 파일 경로
     */
    @SuppressWarnings("unchecked")
    public TribuoInferenceServiceV2(String modelPath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath))) {
            // 직렬화된 XGBoost 모델 객체 복원
            this.model = (Model<Regressor>) ois.readObject();
            log.info("고도화 엔진 로드 성공");
        } catch (Exception e) {
            log.error("모델 로드 실패: {}", e.getMessage());
        }
    }

    /**
     * V2 전용 요청 객체를 분석하여 수혈 피처가 반영된 추천 점수를 계산합니다.
     * <p>
     * 기본 금융 지표와 최근 행동 데이터를 매핑하여 {@link ArrayExample}을 생성하고,
     * XGBoost 모델의 앙상블 연산을 통해 최적의 랭킹 점수를 도출합니다.
     * </p>
     * @param request 최신 행동 데이터(수혈 피처)가 포함된 V2 카드 정보 DTO
     * @return 예측된 랭킹 점수 (Regressor 출력값)
     */
    public double predictScoreV2(TribuoRequestV2 request) {
        if (model == null) {
            log.warn("모델이 로드되지 않아 기본 점수(0.0)를 반환합니다.");
            return 0.0;
        }

        // 학습 시 정의한 고도화 피처 명칭 리스트
        String[] fNames = {"feature_1", "feature_2", "feature_3", "new_tx_count", "new_total_amt", "premium_ratio"};

        // 피처 값 매핑: 기본 속성(승인율, 평균액, 총액) + 수혈 속성(최신 건수, 최근 금액, 프리미엄 비중)
        double[] fValues = {
                request.getAuthorizedRatio(),
                request.getAvgAmount(),
                request.getTotalAmount(),
                request.getNewTxCount(),
                request.getNewTotalAmt(),
                request.getPremiumRatio()
        };

        // 정답(Target)은 모르므로 NaN으로 설정하여 예제 생성
        ArrayExample<Regressor> example = new ArrayExample<>(new Regressor("Score", Double.NaN), fNames, fValues);

        // 모델 추론(Forward Pass) 수행
        Prediction<Regressor> prediction = model.predict(example);

        // 결과 텐서에서 최종 회귀 점수 추출
        return prediction.getOutput().getValues()[0];
    }
}