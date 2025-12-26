package aicard.perli.ml.tribuo.service.v2;

import aicard.perli.ml.tribuo.dto.request.v2.CardRequestV2;
import aicard.perli.ml.tribuo.dto.response.CardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>고도화된 추론 엔진을 관리하고 최종 카드 랭킹을 매기는 최상위 서비스 클래스입니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TribuoRecommendationServiceV2 {

    /** 추론 엔진 */
    private final TribuoInferenceServiceV2 inferenceService;

    /**
     * <p>다수의 카드 후보군을 V2 엔진의 예측 점수 기준으로 정렬하여 반환합니다.</p>
     *
     * @param candidates 후보 카드 DTO 리스트
     * @return 점수 내림차순으로 정렬된 추천 결과 리스트
     */
    public List<CardResponse> getRankedRecommendationsV2(List<CardRequestV2> candidates) {
        return candidates.stream()
                .map(card -> new CardResponse(card.getCardId(), inferenceService.predictScoreV2(card)))
                .sorted(Comparator.comparingDouble(CardResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    /** 상위 N개 카드 추천 결과 반환 */
    public List<CardResponse> getTopNRecommendationsV2(List<CardRequestV2> candidates, int n) {
        return getRankedRecommendationsV2(candidates).stream()
                .limit(n)
                .collect(Collectors.toList());
    }
}