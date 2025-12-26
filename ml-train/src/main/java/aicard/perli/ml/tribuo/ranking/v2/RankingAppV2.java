package aicard.perli.ml.tribuo.ranking.v2;

import aicard.perli.ml.tribuo.dto.request.v1.CardRequestV1;
import aicard.perli.ml.tribuo.dto.request.v2.CardRequestV2;
import aicard.perli.ml.tribuo.dto.response.CardResponse;
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
 * <p>고도화된 카드 추천 랭킹 모델의 학습 및 V1 대비 성능 검증을 실행하는 메인 클래스입니다.</p>
 * <p>수혈된 피처를 기반으로 XGBoost 모델을 생성하고, 기존 모델과의 추론 점수 차이를 분석합니다.</p>
 */
@Slf4j
public class RankingAppV2 {

    public static void main(String[] args) {
        // 경로 설정
        String csvPathV2 = "resources/processed/h2o/v2/train_uplift_v2.csv";
        String modelPathV2 = "resources/output/models/tribuo/v2/tribuo_xgboost_v2.gdpc";
        String modelPathV1 = "resources/output/models/tribuo/v1/card_ranking_model.gdpc";

        log.info("랭킹 모델 학습 및 비교 검증 파이프라인 가동");

        // V2 고도화 모델 학습 (XGBoost)
        TribuoDataConverterV2 converterV2 = new TribuoDataConverterV2();
        TribuoTrainServiceV2 trainServiceV2 = new TribuoTrainServiceV2(converterV2);

        try {
            log.info("V2 수혈 데이터 기반 XGBoost 학습 시작");
            trainServiceV2.trainV2(csvPathV2, modelPathV2);

            // 서비스 초기화 및 비교 추론
            TribuoInferenceServiceV1 infV1 = new TribuoInferenceServiceV1(modelPathV1);
            TribuoInferenceServiceV2 infV2 = new TribuoInferenceServiceV2(modelPathV2);

            TribuoRecommendationServiceV1 recV1 = new TribuoRecommendationServiceV1(infV1);
            TribuoRecommendationServiceV2 recV2 = new TribuoRecommendationServiceV2(infV2);

            // 테스트 데이터 구성 (V1 vs V2)
            CardRequestV1 testUserV1 = new CardRequestV1("USER_001", 5000000.0, 120.0, 0.95, 45000.0, 0.0);
            CardRequestV2 testUserV2 = new CardRequestV2("USER_001", 5000000.0, 120, 0.95, 45000.0, 15.0, 850000.0, 0.75);

            // 결과 산출
            List<CardResponse> resV1 = recV1.getRankedRecommendations(Arrays.asList(testUserV1));
            List<CardResponse> resV2 = recV2.getRankedRecommendationsV2(Arrays.asList(testUserV2));

            log.info("----------------------------------------------------------");
            log.info("V1 Score : {}", String.format("%.6f", resV1.get(0).getScore()));
            log.info("V2 Score : {}", String.format("%.6f", resV2.get(0).getScore()));
            log.info("----------------------------------------------------------");

            log.info("고도화 모델 학습 및 성능 검증 완료");

        } catch (Exception e) {
            log.error("V2 랭킹 파이프라인 실행 중 오류 발생: ", e);
            System.exit(1);
        }

        log.info("프로젝트의 랭킹 고도화 공정이 성공적으로 종료되었습니다.");
    }
}