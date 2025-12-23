package aicard.perli.ml.tribuo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>카드 랭킹 예측의 최종 결과를 담는 응답 DTO 클래스입니다.</p>
 * <p>모델이 계산한 점수와 해당 카드의 식별자를 포함합니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    /** 카드의 고유 식별자 */
    private String cardId;

    /** * 모델에 의해 산출된 추천 점수입니다.
     * 점수가 높을수록 해당 사용자에게 더 적합한 카드로 분류됩니다.
     */
    private double score;

    @Override
    public String toString() {
        return "CardRecommendationResponse{" +
                "cardId='" + cardId + '\'' +
                ", score=" + score +
                '}';
    }
}