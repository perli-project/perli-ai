package aicard.perli.ml.tribuo.service.v1;

import aicard.perli.ml.tribuo.dto.request.v1.TribuoRequestV1;
import aicard.perli.ml.tribuo.dto.response.TribuoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>여러 후보 카드들에 대해 학습된 모델을 적용하고,
 * 점수 기반으로 정렬된 추천 리스트를 제공하는 최종 엔진 서비스입니다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TribuoRecommendationServiceV1 {

    private final TribuoInferenceServiceV1 inferenceService;

    /**
     * 후보 카드 리스트를 입력받아 추천 점수가 높은 순으로 정렬된 리스트를 반환합니다.
     *
     * @param candidates 추천 후보 카드들의 통계 데이터(DTO) 리스트
     * @return 추천 점수 내림차순으로 정렬된 결과 리스트
     */
    public List<TribuoResponse> getRankedRecommendations(List<TribuoRequestV1> candidates) {
        return candidates.stream()
                .map(card -> {
                    // 모델을 통한 점수 예측 수행 (기본 4종 피처 활용)
                    double score = inferenceService.predictScore(
                            card.getTotalAmount(),
                            card.getTxCount(),
                            card.getAuthorizedRatio(),
                            card.getAvgAmount()
                    );
                    // 응답 DTO 클래스로 변환
                    return new TribuoResponse(card.getCardId(), score);
                })
                // 점수(Score) 기준 내림차순 정렬
                .sorted(Comparator.comparingDouble(TribuoResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 상위 N개의 카드만 추출하여 반환합니다.
     */
    public List<TribuoResponse> getTopNRecommendations(List<TribuoRequestV1> candidates, int n) {
        return getRankedRecommendations(candidates).stream()
                .limit(n)
                .collect(Collectors.toList());
    }
}