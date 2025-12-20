package aicard.perli.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 학습 및 분석을 위한 전 모듈 공용 통합 금융 이벤트 객체.
 *  <p>이 클래스는 Elo, UCI, Olist 등 다양한 금융 데이터셋을 통합하여
 * 머신러닝(ML) 및 딥러닝(DL) <br>모델의 입력 데이터로 사용하기 위해 설계되었다.</p>
 *  <p>하이브리드 구조 내에서 'Domain Model' 계층의 역할을 수행하며,
 * 데이터 로더(Loader)에 의해 생성되어 학습 서비스(Service)로 전달됩니다.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinEvent {

    /**
     * 카드 식별자 (Elo 데이터셋의 card_id).
     * 거래 내역 조인 및 사용자 패턴 그룹화의 기준이 되는 핵심 키.
     */
    private String cardId;

    /**
     * 사용자 식별자.
     * 여러 카드를 소유한 사용자의 통합 소비 성향 분석 시 사용.
     */
    private String userId;

    /**
     * 거래 발생 일시.
     * 시계열 분석(LSTM 등) 및 시간 기반 피처(주중/주말, 시간대) 추출에 사용.
     */
    private LocalDateTime txDate;

    /**
     * 거래 금액.
     * 분석 모델의 핵심 수치 피처 (정규화 또는 스케일링 대상).
     */
    private double amount;

    /**
     * 할부 개월 수.
     * 소비 규모 및 결제 패턴을 파악하는 범주형/수치형 피처.
     */
    private int installments;

    /**
     * 거래 승인 여부 (authorized_flag).
     * Y인 경우 true, N인 경우 false로 변환하여 학습에 활용.
     */
    private boolean authorized;

    /**
     * 가맹점 카테고리 ID (merchant_category_id).
     * 소비 업종별 패턴 분석을 위한 범주형 데이터.
     */
    private int mctCatId;

    /**
     * 도시 식별 코드 (city_id).
     * 지역별 소비 특성 및 거주지 추정 피처로 활용.
     */
    private String cityId;

    /**
     * 주 식별 코드 (state_id).
     * <p>사용자 또는 거래가 발생한 '주(State)' 단위의 지역 정보입니다.
     * 특정 지역의 경기 상황, 지역별 선호 업종, 또는 거주지 기반의
     * 세부 소비 패턴을 분석하기 위한 범주형(Categorical) 피처로 활용됩니다.</p>
     */
    private String stateId;

    /**
     * AI 모델의 학습 타겟 (Elo loyalty score 등).
     * 예측 모델(Regression)의 정답지(Label) 역할.
     */
    private double target;

    /**
     * 신용 한도 금액 (UCI Default 등).
     * 사용자의 금융 여력을 나타내는 피처.
     */
    private double limitBal;

    /**
     * 카드 이용 실적 포함 여부.
     * 비즈니스 로직에 따른 데이터 필터링 및 전처리 가이드로 활용.
     */
    private boolean isPerf;

}