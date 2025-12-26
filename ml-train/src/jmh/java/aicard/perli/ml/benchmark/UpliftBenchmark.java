package aicard.perli.ml.benchmark;

import aicard.perli.ml.h2o.service.v1.H2oInferenceServiceV1;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

/**
 * 업리프트 추론 엔진의 성능을 측정하는 벤치마크 클래스입니다.
 * S-Learner 특성상 1회 추론 시 내부적으로 2번의 모델 예측이 발생함을 검증합니다.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime) // 평균 응답 시간 측정
@OutputTimeUnit(TimeUnit.MILLISECONDS) // 결과를 밀리초 단위로 출력
public class UpliftBenchmark {

    private H2oInferenceServiceV1 inferenceService;

    @Setup // 벤치마크 시작 전 딱 한 번 실행 (모델 로드)
    public void setup() {
        String modelPath = "C:/Coding/perli-ai/resources/output/models/h2o/gbm_uplift_model.zip";
        inferenceService = new H2oInferenceServiceV1(modelPath);
    }

    @Benchmark // 실제 성능 측정 대상 메서드
    public double benchmarkPredictUplift() {
        // 실제 API 호출 시와 동일한 데이터 입력
        return inferenceService.predictUplift(3000.0, 50, 1.5, 1000.0, 60.0, 0.97);
    }
}