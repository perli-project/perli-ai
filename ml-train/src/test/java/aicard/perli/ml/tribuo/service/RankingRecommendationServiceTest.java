package aicard.perli.ml.tribuo.service;

import aicard.perli.ml.tribuo.dto.request.CardRequest;
import aicard.perli.ml.tribuo.dto.response.CardResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

class RankingRecommendationServiceTest {

    @Test
    public void testRecommendation() {
        String modelPath = "C:/Coding/perli-ai/resources/output/models/card_ranking_model.gdpc";
        RankingRecommendationService engine = new RankingRecommendationService(modelPath);

        // 가상의 후보 카드 데이터
        List<CardRequest> candidates = List.of(
                new CardRequest("CARD_A", 5000000.0, 100.0, 0.98, 50000.0, 0.0),
                new CardRequest("CARD_B", 100000.0, 5.0, 0.50, 20000.0, 0.0),
                new CardRequest("CARD_C", 2500000.0, 60.0, 0.90, 41000.0, 0.0)
        );

        // 랭킹 산출
        List<CardResponse> results = engine.getRankedRecommendations(candidates);

        // 출력
        results.forEach(res -> System.out.println("카드: " + res.getCardId() + ", 점수: " + res.getScore()));
    }
}