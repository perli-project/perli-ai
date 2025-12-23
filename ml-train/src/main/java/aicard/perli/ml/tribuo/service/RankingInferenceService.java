package aicard.perli.ml.tribuo.service;

import org.tribuo.Feature;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.*;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>학습된 Tribuo 랭킹 모델을 사용하여 실시간 카드 점수를 산출하는 서비스입니다.</p>
 * <p>Regressor 소스 규격에 맞춰 예측 결과에서 수치 데이터를 추출합니다.</p>
 */
public class RankingInferenceService {

    private final Model<Regressor> model;
    /** Regressor 출력을 생성하기 위한 표준 팩토리입니다. */
    private final RegressionFactory factory = new RegressionFactory();

    /**
     * @param modelPath .gdpc 확장자의 학습 완료된 모델 파일 경로
     */
    @SuppressWarnings("unchecked")
    public RankingInferenceService(String modelPath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath))) {
            this.model = (Model<Regressor>) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("모델 파일을 읽을 수 없습니다: " + modelPath, e);
        }
    }

    /**
     * 카드별 통계 지표를 입력하여 모델이 계산한 랭킹 점수를 반환합니다.
     *
     * @param totalAmount 총 결제액
     * @param txCount 거래 횟수
     * @param authRatio 승인율
     * @param avgAmount 평균 결제액
     * @return 랭킹 점수 (Regressor.values[0])
     */
    public double predictScore(double totalAmount, double txCount, double authRatio, double avgAmount) {
        // 학습 시 정의한 피처 명칭과 순서에 맞게 리스트 생성
        List<Feature> features = new ArrayList<>();
        features.add(new Feature("totalAmount", totalAmount));
        features.add(new Feature("txCount", txCount));
        features.add(new Feature("authRatio", authRatio));
        features.add(new Feature("avgAmount", avgAmount));

        // Regressor 소스 코드의 Unknown 규격을 사용하여 예제 객체 생성
        // RegressionFactory는 내부적으로 NaN 값을 가진 Regressor를 생성하여 반환합니다.
        Regressor unknownOutput = factory.getUnknownOutput();
        ArrayExample<Regressor> example = new ArrayExample<>(unknownOutput, features);

        // 모델 추론 수행
        Prediction<Regressor> prediction = model.predict(example);

        // Regressor.getValues() 호출하여 결과값 추출
        // Regressor 클래스 정의에 따라 values 배열의 첫 번째 요소가 결과 점수입니다.
        return prediction.getOutput().getValues()[0];
    }
}