package aicard.perli.ml.tribuo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카드 랭킹 예측의 최종 결과를 담는 응답 DTO 클래스입니다.
 * <p>
 * Tribuo 모델이 특정 사용자의 소비 패턴을 분석하여 도출한
 * 각 카드별 추천 점수와 식별 정보를 포함하며, 최종 랭킹 산출의 기준 데이터로 활용됩니다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TribuoResponse {

    /** 추천 대상 카드의 시스템 고유 식별자 */
    private String cardId;

    /** 모델에 의해 산출된 수리적 추천 점수(Regression Score)입니다.
     * <p>
     * 해당 사용자의 피처와 카드 혜택 간의 적합도를 나타내며,
     * 점수가 높을수록 사용자에게 더 높은 가치를 제공하는 카드로 판단됩니다.
     * </p>
     */
    private double score;

    /**
     * 예측 결과를 문자열 형식으로 반환합니다.
     * 주로 디버깅 및 로그 기록 시 추천 결과의 정합성을 확인하기 위해 사용됩니다.
     */
    @Override
    public String toString() {
        return "CardRecommendationResponse{" +
                "cardId='" + cardId + '\'' +
                ", score=" + score +
                '}';
    }
}