package aicard.perli.ml.tribuo.service;

import aicard.perli.ml.tribuo.dto.request.v1.TribuoRequestV1;
import aicard.perli.ml.tribuo.dto.response.TribuoResponse;
import aicard.perli.ml.tribuo.service.v1.TribuoInferenceServiceV1;
import aicard.perli.ml.tribuo.service.v1.TribuoRecommendationServiceV1;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * <p>랭킹 추천 엔진 테스트 클래스입니다.</p>
 * <p>InferenceService를 생성하여 RecommendationService에 주입하는 의존성 주입 방식을 검증합니다.</p>
 */
class TribuoRecommendationServiceV1Test {

    @Test
    public void testRecommendation() {
        // 모델 경로 설정
        String modelPath = "C:/Coding/perli-ai/resources/output/models/tribuo/v1/tribuo_ranking_v1.gdpc";

        // 객체 주입 구조로 변경
        TribuoInferenceServiceV1 inferenceService = new TribuoInferenceServiceV1(modelPath);
        TribuoRecommendationServiceV1 engine = new TribuoRecommendationServiceV1(inferenceService);

        // 가상의 후보 카드 데이터 (V1 규격: 6개 필드 준수)
        List<TribuoRequestV1> candidates = List.of(
                new TribuoRequestV1("CARD_A", 5000000.0, 100.0, 0.98, 50000.0, 0.0),
                new TribuoRequestV1("CARD_B", 100000.0, 5.0, 0.50, 20000.0, 0.0),
                new TribuoRequestV1("CARD_C", 2500000.0, 60.0, 0.90, 41000.0, 0.0)
        );

        // 랭킹 산출
        List<TribuoResponse> results = engine.getRankedRecommendations(candidates);

        // 출력 및 검증
        System.out.println("======= 엔진 테스트 결과 =======");
        results.forEach(res -> System.out.println("카드: " + res.getCardId() + ", 점수: " + res.getScore()));
        System.out.println("=======================================");
    }
}