package aicard.perli.ml.tribuo.ranking.v2;

import aicard.perli.ml.tribuo.dto.request.v1.TribuoRequestV1;
import aicard.perli.ml.tribuo.dto.request.v2.TribuoRequestV2;
import aicard.perli.ml.tribuo.dto.response.TribuoResponse;
import aicard.perli.ml.tribuo.service.v1.TribuoInferenceServiceV1;
import aicard.perli.ml.tribuo.service.v1.TribuoRecommendationServiceV1;
import aicard.perli.ml.tribuo.service.v2.TribuoInferenceServiceV2;
import aicard.perli.ml.tribuo.service.v2.TribuoRecommendationServiceV2;
import aicard.perli.ml.tribuo.service.v2.TribuoTrainServiceV2;
import aicard.perli.ml.tribuo.util.v2.TribuoDataConverterV2;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * 고도화된 카드 추천 랭킹 모델의 학습 및 V1 대비 성능 검증을 실행하는 메인 클래스입니다.
 * <p>
 * 수혈된 최신 행동 피처를 기반으로 XGBoost 알고리즘 모델을 생성하고,
 * 기존 선형 모델(V1)과의 추론 결과 비교를 통해 모델 고도화의 효과를 정량적으로 분석합니다.
 * </p>
 */
@Slf4j
public class RankingAppV2 {

    /**
     * V2 고도화 학습 및 비교 추론의 전체 프로세스를 관장하는 진입점입니다.
     * <p>
     * {@link TribuoTrainServiceV2}를 이용한 XGBoost 모델 빌드 <br>
     * V1 및 V2 추론 엔진 동시 로드 <br>
     * 동일 고객 시나리오에 대한 교차 예측 수행 및 점수 대조 리포트 산출 <br>
     * 순으로 진행됩니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        // 물리적 데이터 및 모델 저장 경로 설정
        String csvPathV2 = "resources/processed/h2o/v2/train_uplift_v2.csv";
        String modelPathV2 = "resources/output/models/tribuo/v2/tribuo_xgboost_v2.gdpc";
        String modelPathV1 = "resources/output/models/tribuo/v1/card_ranking_model.gdpc";

        log.info("==========================================================");
        log.info("랭킹 모델 학습 및 비교 검증 파이프라인 가동");
        log.info("==========================================================");

        // V2 고도화 모델 학습 (XGBoost 엔진 가동)
        TribuoDataConverterV2 converterV2 = new TribuoDataConverterV2();
        TribuoTrainServiceV2 trainServiceV2 = new TribuoTrainServiceV2(converterV2);

        try {
            log.info("V2 수혈 데이터 기반 XGBoost 학습 시작");
            trainServiceV2.trainV2(csvPathV2, modelPathV2);

            // 서비스 초기화 및 비교 추론 환경 구축
            TribuoInferenceServiceV1 infV1 = new TribuoInferenceServiceV1(modelPathV1);
            TribuoInferenceServiceV2 infV2 = new TribuoInferenceServiceV2(modelPathV2);

            TribuoRecommendationServiceV1 recV1 = new TribuoRecommendationServiceV1(infV1);
            TribuoRecommendationServiceV2 recV2 = new TribuoRecommendationServiceV2(infV2);

            // 비교 테스트용 가상 고객 데이터 구성
            // V1: 기초 통계 위주 / V2: 최근 거래 트렌드 및 프리미엄 소비 비중 포함
            TribuoRequestV1 testUserV1 = new TribuoRequestV1("USER_001", 5000000.0, 120.0, 0.95, 45000.0, 0.0);
            TribuoRequestV2 testUserV2 = new TribuoRequestV2("USER_001", 5000000.0, 120, 0.95, 45000.0, 15.0, 850000.0, 0.75);

            // 추천 결과 산출 및 점수 대조
            List<TribuoResponse> resV1 = recV1.getRankedRecommendations(Arrays.asList(testUserV1));
            List<TribuoResponse> resV2 = recV2.getRankedRecommendationsV2(Arrays.asList(testUserV2));

            log.info("----------------------------------------------------------");
            log.info("비교 분석 리포트");
            log.info("----------------------------------------------------------");
            log.info("V1 Score (Linear Baseline) : {}", String.format("%.6f", resV1.get(0).getScore()));
            log.info("V2 Score (Advanced XGBoost) : {}", String.format("%.6f", resV2.get(0).getScore()));
            log.info("----------------------------------------------------------");

            log.info("고도화 모델 학습 및 성능 검증 완료");

        } catch (Exception e) {
            // 파이프라인 실행 중 오류 발생 시 상세 로그 기록 후 비정상 종료 처리
            log.error("V2 랭킹 파이프라인 실행 중 오류 발생: ", e);
            System.exit(1);
        }

        log.info("==========================================================");
        log.info("프로젝트의 랭킹 고도화 공정이 성공적으로 종료되었습니다.");
        log.info("==========================================================");
    }
}