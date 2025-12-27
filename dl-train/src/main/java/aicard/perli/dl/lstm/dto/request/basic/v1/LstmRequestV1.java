package aicard.perli.dl.lstm.dto.request.basic.v1;

import java.time.LocalDate;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DL4J 모델의 입력값으로 사용되는 요청 객체
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LstmRequestV1 {
    private String userId;    // 사용자 ID
    private LocalDate date;   // 결제 일자
    private double amount;    // 결제 금액
    private double limitBal;    // 카드 한도
    private double billAmt;     // 당월 청구 금액
    private double payAmt;      // 당월 결제 금액
    private int payStatus;      // 결제 상태 (UCI 데이터의 PAY_0 등)
}