package aicard.perli.ml.tribuo.util.v1;

import aicard.perli.ml.tribuo.dto.request.v1.TribuoRequestV1;
import org.tribuo.*;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * <p>Tribuo 머신러닝 라이브러리를 사용하여 카드 랭킹 학습을 위한 데이터 변환을 담당하는 유틸리티 클래스입니다.</p>
 *
 * <p>이 클래스는 서비스 레이어에서 가공된 {@link TribuoRequestV1} DTO 리스트를
 * Tribuo의 표준 데이터셋 규격인 {@link Dataset} (세부적으로는 {@link MutableDataset})으로 변환합니다.
 * 랭킹 알고리즘 구현을 위해 회귀(Regression) 모델인 {@link Regressor} 타입을 기반으로 동작합니다.</p>
 */
public class TribuoDataConverterV1 {

    /** 회귀 분석용 결과 객체(Regressor)를 생성하기 위한 팩토리 클래스입니다.
     */
    private final RegressionFactory regressionFactory = new RegressionFactory();

    /**
     * <p>카드 랭킹 요청 데이터 리스트를 Tribuo 학습용 데이터셋으로 변환합니다.</p>
     * <p>변환 과정은 다음과 같습니다:
     * <ol>
     * <li>{@link org.tribuo.provenance.SimpleDataSourceProvenance}를 사용하여 데이터 출처 정보를 포함한 빈 데이터셋을 초기화합니다.</li>
     * <li>각 DTO에서 피처(금액, 건수, 승인율 등)를 추출하여 {@link Feature} 리스트를 구성합니다.</li>
     * <li>정답 데이터인 relevanceScore를 {@link Regressor} 객체로 변환합니다.</li>
     * <li>피처와 정답을 결합한 {@link ArrayExample}을 생성하여 최종 데이터셋에 추가합니다.</li>
     * </ol>
     * </p>
     *
     * @param requests 가공된 카드 통계 데이터 리스트 (DTO)
     * @return Tribuo 모델 학습에 직접 투입 가능한 Regressor 타입의 Dataset
     */
    public Dataset<Regressor> convertToDataset(List<TribuoRequestV1> requests) {

        MutableDataset<Regressor> dataset = new MutableDataset<>(
                Collections.emptyList(),
                new org.tribuo.provenance.SimpleDataSourceProvenance("Generated from CardRankingRequest DTO", regressionFactory),
                regressionFactory
        );

        for (TribuoRequestV1 req : requests) {
            // 모델이 학습할 독립 변수(Feature)들을 정의합니다.
            List<Feature> features = new ArrayList<>();
            features.add(new Feature("totalAmount", req.getTotalAmount()));
            features.add(new Feature("txCount", req.getTxCount()));
            features.add(new Feature("authRatio", req.getAuthorizedRatio()));
            features.add(new Feature("avgAmount", req.getAvgAmount()));

            // 정답(target)인 랭킹 점수를 Tribuo가 이해할 수 있는 Regressor 객체로 변환합니다.
            Regressor label = regressionFactory.generateOutput(req.getRelevanceScore());

            // 피처와 라벨을 묶어 데이터셋에 훈련 예제로 추가합니다.
            dataset.add(new ArrayExample<>(label, features));
        }

        return dataset;
    }
}