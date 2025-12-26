package aicard.perli.ml.tribuo.service.v2;

import aicard.perli.ml.tribuo.dto.request.v2.CardRequestV2;
import lombok.extern.slf4j.Slf4j;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.Regressor;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * <p>[V2] XGBoost 모델을 활용한 고도화 실시간 추론 서비스입니다.</p>
 *
 * <p>수혈된 최신 행동 지표 3종과 기본 지표 3종을 결합하여 카드별 랭킹 점수를 산출합니다.</p>
 */
@Slf4j
public class TribuoInferenceServiceV2 {

    /** 메모리에 로드된 XGBoost 모델 */
    private Model<Regressor> model;

    /**
     * @param modelPath .gdpc 모델 파일 경로
     */
    @SuppressWarnings("unchecked")
    public TribuoInferenceServiceV2(String modelPath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath))) {
            this.model = (Model<Regressor>) ois.readObject();
            log.info("고도화 엔진 로드 성공");
        } catch (Exception e) {
            log.error("모델 로드 실패: {}", e.getMessage());
        }
    }

    /**
     * <p>V2 전용 요청 객체를 분석하여 추천 점수를 계산합니다.</p>
     *
     * @param request 최신 행동 데이터가 포함된 카드 정보
     * @return 예측된 랭킹 점수 (double)
     */
    public double predictScoreV2(CardRequestV2 request) {
        if (model == null) return 0.0;

        String[] fNames = {"feature_1", "feature_2", "feature_3", "new_tx_count", "new_total_amt", "premium_ratio"};
        double[] fValues = {
                request.getAuthorizedRatio(),
                request.getAvgAmount(),
                request.getTotalAmount(),
                request.getNewTxCount(),
                request.getNewTotalAmt(),
                request.getPremiumRatio()
        };

        ArrayExample<Regressor> example = new ArrayExample<>(new Regressor("Score", Double.NaN), fNames, fValues);
        Prediction<Regressor> prediction = model.predict(example);

        return prediction.getOutput().getValues()[0];
    }
}