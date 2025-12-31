package aicard.perli.ml.tribuo.util.v1;

import aicard.perli.ml.tribuo.dto.request.v1.TribuoRequestV1;
import org.tribuo.*;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Tribuo 머신러닝 라이브러리를 사용하여 카드 랭킹 학습을 위한 데이터 변환을 담당하는 유틸리티 클래스입니다.
 * <p>
 * 서비스 레이어에서 가공된 {@link TribuoRequestV1} DTO 리스트를 Tribuo의 표준 데이터셋 규격인
 * {@link Dataset} (세부적으로는 {@link MutableDataset})으로 변환합니다.
 * 본 클래스는 랭킹 알고리즘 구현을 위해 회귀(Regression) 모델인 {@link Regressor} 타입을 기반으로 동작합니다.
 * </p>
 */
public class TribuoDataConverterV1 {

    /** 회귀 분석용 결과 객체(Regressor)를 생성하기 위한 표준 팩토리 클래스입니다. */
    private final RegressionFactory regressionFactory = new RegressionFactory();

    /**
     * 카드 랭킹 요청 데이터 리스트를 Tribuo 학습용 데이터셋으로 변환합니다.
     * <p>변환 메커니즘:
     * <ol>
     * <li>{@link org.tribuo.provenance.SimpleDataSourceProvenance}를 사용하여 데이터 출처 정보를 포함한 빈 데이터셋 초기화</li>
     * <li>DTO에서 추출한 수치형 피처들을 Tribuo 표준 {@link Feature} 객체 리스트로 매핑</li>
     * <li>학습 목표값(Target)인 relevanceScore를 {@link Regressor} 출력 객체로 변환</li>
     * <li>피처와 출력을 결합한 {@link ArrayExample}을 생성하여 {@link MutableDataset}에 적재</li>
     * </ol>
     * </p>
     *
     * @param requests 가공된 카드 통계 데이터 리스트 (DTO)
     * @return Tribuo 모델 학습에 직접 투입 가능한 Regressor 타입의 Dataset 객체
     */
    public Dataset<Regressor> convertToDataset(List<TribuoRequestV1> requests) {

        // 데이터셋 초기화: 출처(Provenance) 기록을 통해 모델의 재현성 확보
        MutableDataset<Regressor> dataset = new MutableDataset<>(
                Collections.emptyList(),
                new org.tribuo.provenance.SimpleDataSourceProvenance("Generated from CardRankingRequest DTO", regressionFactory),
                regressionFactory
        );

        for (TribuoRequestV1 req : requests) {
            // 피처 엔지니어링: 모델이 학습할 독립 변수(Feature)들을 정의
            List<Feature> features = new ArrayList<>();
            features.add(new Feature("totalAmount", req.getTotalAmount()));
            features.add(new Feature("txCount", req.getTxCount()));
            features.add(new Feature("authRatio", req.getAuthorizedRatio()));
            features.add(new Feature("avgAmount", req.getAvgAmount()));

            // 라벨 생성: 정답(Target)인 랭킹 점수를 Tribuo 전용 Regressor 객체로 캡슐화
            Regressor label = regressionFactory.generateOutput(req.getRelevanceScore());

            // 데이터 적재: 피처 벡터와 라벨을 하나의 ArrayExample로 묶어 데이터셋에 추가
            dataset.add(new ArrayExample<>(label, features));
        }

        return dataset;
    }
}