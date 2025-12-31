package aicard.perli.dl.optimization.domain.fix.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 신용카드의 고유 정보 및 혜택 조건을 관리하는 고정(Fixed) 데이터 클래스입니다.
 * <p>
 * Timefold 최적화 엔진에서 변하지 않는 사실(Problem Fact)로 활용되며,
 * 특정 카드의 이름, 실적 달성 목표, 기본 혜택 비율 등의 기준점을 정의합니다.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardV1 {

    /** 카드를 식별하기 위한 시스템 고유 ID */
    private String cardId;

    /** 사용자가 식별할 수 있는 카드의 공식 명칭 (예: 신한 Mr.Life) */
    private String cardName;

    /** 차월 혜택을 받기 위해 반드시 달성해야 하는 최소 전월 실적 목표 금액입니다.
     * <p>
     * 최적화 엔진이 지출 배정을 결정할 때, 하드 제약(Hard Constraint)을 통과하기 위해
     * 최우선적으로 충족시켜야 하는 수리적 기준점이 됩니다.
     * </p>
     */
    private double performanceTarget;

    /** 해당 카드가 제공하는 표준 혜택 비율입니다. (예: 0.05는 결제 금액의 5% 혜택을 의미)
     * <p>
     * V1 모델에서는 업종 구분 없이 모든 지출에 대해 단일 비율을 적용하며,
     * 이는 소프트 제약(Soft Constraint)에서 총 혜택 점수를 산출하는 가중치가 됩니다.
     * </p>
     */
    private double benefitRate;
}