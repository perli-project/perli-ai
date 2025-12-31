package aicard.perli.ml.tribuo.service.v2;

import aicard.perli.ml.tribuo.dto.request.v2.TribuoRequestV2;
import aicard.perli.ml.tribuo.dto.response.TribuoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 고도화된 추론 엔진을 관리하고 최종 카드 랭킹을 매기는 최상위 서비스 클래스입니다.
 * <p>
 * XGBoost 기반의 V2 추론 엔진으로부터 도출된 고정밀 예측 점수를 활용하여,
 * 유저의 최신 소비 성향이 반영된 최적의 카드 추천 리스트를 생성합니다.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class TribuoRecommendationServiceV2 {

    /** 고도화된 XGBoost 추론 로직을 수행하는 V2 엔진 */
    private final TribuoInferenceServiceV2 inferenceService;

    /**
     * 다수의 카드 후보군을 V2 엔진의 예측 점수 기준으로 정렬하여 반환합니다.
     * <p>
     * 수혈된 피처(최신 거래 추세, 프리미엄 비중 등)가 반영된 점수를 기반으로
     * JAVA Stream API를 통해 내림차순 정렬을 수행함으로써 실시간 랭킹 시스템을 구현합니다.
     * </p>
     * @param candidates 최신 행동 데이터가 포함된 후보 카드 DTO 리스트
     * @return XGBoost 예측 점수 내림차순(Desc)으로 정렬된 추천 결과 리스트
     */
    public List<TribuoResponse> getRankedRecommendationsV2(List<TribuoRequestV2> candidates) {
        return candidates.stream()
                // 각 후보 카드에 대해 V2 엔진을 통한 고정밀 스코어링 수행
                .map(card -> new TribuoResponse(card.getCardId(), inferenceService.predictScoreV2(card)))
                // [수리적 정렬]: 추천 적합도가 높은 순서대로 랭킹 재배열
                .sorted(Comparator.comparingDouble(TribuoResponse::getScore).reversed())
                .collect(Collectors.toList());
    }

    /** * 상위 N개의 핵심 카드 추천 결과만을 선별하여 반환합니다.
     * <p>
     * 전체 랭킹 중 유저의 결정 효율을 높이기 위해 최상위권의 유효한 추천 정보만 추출합니다.
     * </p>
     * @param candidates 후보 카드 리스트
     * @param n 최종 노출할 상위 카드의 개수
     * @return 최적화된 상위 N개의 V2 추천 결과 리스트
     */
    public List<TribuoResponse> getTopNRecommendationsV2(List<TribuoRequestV2> candidates, int n) {
        return getRankedRecommendationsV2(candidates).stream()
                .limit(n)
                .collect(Collectors.toList());
    }
}