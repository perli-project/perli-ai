package aicard.perli.ml.tribuo.service.v1;

import aicard.perli.ml.tribuo.dto.request.v1.TribuoRequestV1;
import aicard.perli.ml.tribuo.dto.response.TribuoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 여러 후보 카드들에 대해 학습된 모델을 적용하고,
 * 점수 기반으로 정렬된 추천 리스트를 제공하는 최종 엔진 서비스입니다.
 * <p>
 * 추론 서비스로부터 전달받은 수리적 예측치를 가공하여 유저에게 실질적인
 * 카드 순위(Ranking)를 제공하는 비즈니스 로직을 담당합니다.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class TribuoRecommendationServiceV1 {

    /** 실시간 스코어링을 수행하는 추론 엔진 */
    private final TribuoInferenceServiceV1 inferenceService;

    /**
     * 후보 카드 리스트를 입력받아 추천 점수가 높은 순으로 정렬된 리스트를 반환합니다.
     * <p>
     * 각 카드별 피처를 모델에 입력하여 적합도 점수를 산출하고,
     * JAVA Stream API를 통해 고속으로 정렬하여 랭킹 데이터셋을 구축합니다.
     * </p>
     * @param candidates 추천 후보 카드들의 통계 데이터(DTO) 리스트
     * @return 추천 점수 내림차순(Desc)으로 정렬된 결과 리스트
     */
    public List<TribuoResponse> getRankedRecommendations(List<TribuoRequestV1> candidates) {
        return candidates.stream()
                .map(card -> {
                    // 모델을 통한 점수 예측 수행 (V1 표준 4종 피처 활용)
                    double score = inferenceService.predictScore(
                            card.getTotalAmount(),
                            card.getTxCount(),
                            card.getAuthorizedRatio(),
                            card.getAvgAmount()
                    );
                    // 결과 식별자와 스코어를 결합하여 응답 객체 생성
                    return new TribuoResponse(card.getCardId(), score);
                })
                // 적합도 점수(Score) 기준 내림차순 정렬 (높은 점수 우선)
                .sorted(Comparator.comparingDouble(TribuoResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 상위 N개의 최적 카드만 추출하여 반환합니다.
     * <p>
     * 전체 랭킹 리스트 중 유저에게 가장 유의미한 상위 소수의 카드 정보만 필터링합니다.
     * </p>
     * @param candidates 추천 후보 카드 리스트
     * @param n 반환할 상위 카드의 개수
     * @return 최적화된 상위 N개의 추천 카드 리스트
     */
    public List<TribuoResponse> getTopNRecommendations(List<TribuoRequestV1> candidates, int n) {
        return getRankedRecommendations(candidates).stream()
                .limit(n)
                .collect(Collectors.toList());
    }
}