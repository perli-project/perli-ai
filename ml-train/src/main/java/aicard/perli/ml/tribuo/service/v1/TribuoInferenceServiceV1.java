package aicard.perli.ml.tribuo.service.v1;

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
 * 학습된 Tribuo 랭킹 모델을 사용하여 실시간 카드 점수를 산출하는 서비스 클래스입니다.
 * <p>
 * Tribuo의 Regressor 소스 규격에 맞춰 예측 결과에서 수치 데이터를 추출하며,
 * 입력된 소비 지표를 바탕으로 카드의 추천 적합도(Score)를 계산합니다.
 * </p>
 */
public class TribuoInferenceServiceV1 {

    /** Tribuo 랭킹 모델 (Regression 타입) */
    private final Model<Regressor> model;

    /** Regressor 출력을 생성하기 위한 표준 팩토리 (추론용 Unknown Output 생성 시 사용) */
    private final RegressionFactory factory = new RegressionFactory();

    /**
     * 지정된 경로에서 직렬화된 Tribuo 모델 파일(.gdpc)을 로드하여 시스템 메모리에 복원합니다.
     * @param modelPath .gdpc 확장자의 학습 완료된 모델 파일 경로
     * @throws RuntimeException 모델 파일을 찾을 수 없거나 객체 역직렬화 실패 시 발생
     */
    @SuppressWarnings("unchecked")
    public TribuoInferenceServiceV1(String modelPath) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelPath))) {
            // 직렬화된 모델 객체를 읽어 Regressor 타입으로 캐스팅
            this.model = (Model<Regressor>) ois.readObject();
        } catch (Exception e) {
            throw new RuntimeException("모델 파일을 읽을 수 없습니다: " + modelPath, e);
        }
    }

    /**
     * 카드별 통계 지표를 입력하여 모델이 계산한 수리적 랭킹 점수를 반환합니다.
     * <p>
     * 입력된 4개 피처를 Tribuo 표준 {@link Feature} 객체로 변환하고,
     * Regressor의 Unknown 규격을 적용한 {@link ArrayExample}을 통해 모델 추론(Inference)을 수행합니다.
     * </p>
     * @param totalAmount 총 결제액
     * @param txCount 거래 횟수
     * @param authRatio 승인율
     * @param avgAmount 평균 결제액
     * @return 랭킹 점수 (Regressor 가중치 연산 결과값)
     */
    public double predictScore(double totalAmount, double txCount, double authRatio, double avgAmount) {
        // 학습 시 정의한 피처 명칭과 순서에 엄격히 맞춰 리스트 생성
        List<Feature> features = new ArrayList<>();
        features.add(new Feature("totalAmount", totalAmount));
        features.add(new Feature("txCount", txCount));
        features.add(new Feature("authRatio", authRatio));
        features.add(new Feature("avgAmount", avgAmount));

        // 추론 시점에는 정답을 모르므로 UnknownOutput 규격을 사용하여 예제 객체 생성
        Regressor unknownOutput = factory.getUnknownOutput();
        ArrayExample<Regressor> example = new ArrayExample<>(unknownOutput, features);

        // 모델을 통한 순방향 연산 수행
        Prediction<Regressor> prediction = model.predict(example);

        // Regressor 결과 텐서에서 첫 번째 인덱스의 스칼라 예측값 추출
        return prediction.getOutput().getValues()[0];
    }
}