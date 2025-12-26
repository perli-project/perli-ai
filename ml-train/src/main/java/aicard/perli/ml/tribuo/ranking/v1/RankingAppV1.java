package aicard.perli.ml.tribuo.ranking.v1;

import aicard.perli.ml.tribuo.service.v1.TribuoTrainServiceV1;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>카드 추천 순위(Ranking) 모델의 학습 파이프라인을 실행하는 메인 클래스입니다.</p>
 * <p>Tribuo SLM 모델을 생성하여 카드별 가치 점수를 산정할 수 있는 환경을 구축합니다.</p>
 */
@Slf4j
public class RankingAppV1 {

    public static void main(String[] args) {
        log.info("카드 추천 랭킹 학습 파이프라인 가동");

        // 학습 서비스 초기화
        TribuoTrainServiceV1 trainService = new TribuoTrainServiceV1();

        try {
            // 내부적으로 resources/raw/historical_transactions.csv 등을 사용합니다.
            trainService.executeTrain();

            log.info("모델 학습 및 .gdpc 파일 저장 완료");
            log.info("저장 위치: resources/output/models/tribuo/v1/card_ranking_model.gdpc");

        } catch (Exception e) {
            log.error("랭킹 모델 학습 중 치명적 오류 발생: ", e);
            System.exit(1); // 오류 발생 시 비정상 종료 코드 반환
        }

        log.info("프로젝트의 랭킹 최적화 모델링이 성공적으로 완료되었습니다.");
    }
}