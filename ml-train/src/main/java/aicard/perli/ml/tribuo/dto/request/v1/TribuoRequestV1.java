package aicard.perli.ml.tribuo.dto.request.v1;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 기존 뼈대 모델용 카드 추천 요청 DTO 클래스입니다.
 * <p>
 * Tribuo 머신러닝 프레임워크에서 유저의 소비 패턴과 카드 간의 연관성을 학습하기 위한
 * 기초 피처(Features)와 타겟(Target) 데이터를 보유합니다.
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TribuoRequestV1 {

    /** 추천 대상 카드 식별자 */
    private String cardId;

    /** 분석 기간 내 총 소비 금액 (사용자의 소비 규모를 나타내는 핵심 피처) */
    private double totalAmount;

    /** 분석 기간 내 총 거래 발생 횟수 (사용자의 활동성을 나타내는 피처) */
    private double txCount;

    /** 전체 승인 요청 대비 승인 완료 비율 (사용자의 금융 신뢰도 및 패턴 안정성 지표) */
    private double authorizedRatio;

    /** 건당 평균 결제 금액 (사용자의 소비 단가 및 성향 분석 지표) */
    private double avgAmount;

    /** 모델 학습의 정답지(Label) 역할을 하는 연관성 점수입니다.
     * <p>
     * 특정 유저가 해당 카드를 사용했을 때 기대되는 혜택 만족도나 충성도를 수치화한 값으로,
     * Tribuo 모델의 회귀(Regression) 타겟 점수로 활용됩니다.
     * </p>
     */
    private double relevanceScore;
}