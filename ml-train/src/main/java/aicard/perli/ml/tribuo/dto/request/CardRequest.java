package aicard.perli.ml.tribuo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {
    private String cardId;
    private double totalAmount; // 총 소비액
    private double txCount; // 거래 횟수
    private double authorizedRatio; // 승인율
    private double avgAmount; // 평균 결제액
    private double relevanceScore; // 정답(target) 점수
}
