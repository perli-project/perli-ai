package aicard.perli.ml.tribuo.ranking.v1;

import aicard.perli.ml.tribuo.service.v1.TribuoTrainServiceV1;
import lombok.extern.slf4j.Slf4j;

/**
 * 카드 추천 순위(Ranking) 모델의 학습 파이프라인을 실행하는 메인 클래스입니다.
 * <p>
 * Tribuo의 SLM(Sparse Linear Model) 모델을 생성하여 카드별 가치 점수를 산정할 수 있는 환경을 구축하며,
 * 데이터 로드부터 모델 직렬화(.gdpc)까지의 전체 라이프사이클을 관리합니다.
 * </p>
 */
@Slf4j
public class RankingAppV1 {

    /**
     * 랭킹 모델 학습 프로세스의 진입점(Entry Point)입니다.
     * <p>
     * {@link TribuoTrainServiceV1}을 통한 학습 엔진 초기화 <br>
     * 원천 데이터(Historical Transactions) 기반의 피처 추출 및 모델 빌드 <br>
     * 학습 결과물인 .gdpc 파일의 물리적 저장 절차를 수행합니다.
     * </p>
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        log.info("==========================================================");
        log.info("카드 추천 랭킹 학습 파이프라인 가동");
        log.info("==========================================================");

        // 학습 서비스 초기화 (Tribuo V1 전용 엔진)
        TribuoTrainServiceV1 trainService = new TribuoTrainServiceV1();

        try {
            // 유저-카드 간의 연관성 지도를 구축합니다.
            trainService.executeTrain();

            log.info("모델 학습 및 .gdpc 파일 저장 완료");
            log.info("저장 위치: resources/output/models/tribuo/v1/card_ranking_model.gdpc");

        } catch (Exception e) {
            // 랭킹 모델 학습 중 치명적 오류 발생 시 로그를 남기고 프로세스를 안전하게 강제 종료합니다.
            log.error("랭킹 모델 학습 중 치명적 오류 발생: ", e);
            System.exit(1);
        }

        log.info("==========================================================");
        log.info("프로젝트의 랭킹 최적화 모델링이 성공적으로 완료되었습니다.");
        log.info("==========================================================");
    }
}