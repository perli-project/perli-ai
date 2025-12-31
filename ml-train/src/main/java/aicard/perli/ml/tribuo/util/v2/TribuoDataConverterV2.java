package aicard.perli.ml.tribuo.util.v2;

import lombok.extern.slf4j.Slf4j;
import org.tribuo.Example;
import org.tribuo.Feature;
import org.tribuo.MutableDataset;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.impl.EmptyDatasetProvenance;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.Regressor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * V2 고도화 피처를 Tribuo 데이터셋 규격으로 변환하는 유틸리티 클래스입니다.
 * <p>
 * 수혈된 최신 행동 데이터(최근 건수, 금액, 프리미엄 비중)를 포함하여 고성능 앙상블 학습용 데이터셋을 구성하며,
 * Tribuo 라이브러리 명세에 따라 {@link EmptyDatasetProvenance}를 사용하여 데이터셋을 초기화합니다.
 * </p>
 */
@Slf4j
public class TribuoDataConverterV2 {

    /** Tribuo Regression 모델 및 예제 생성을 위한 표준 팩토리 */
    private final RegressionFactory factory = new RegressionFactory();

    /**
     * 고도화 CSV 파일을 읽어 XGBoost 학습에 최적화된 MutableDataset을 생성합니다.
     * <p>
     * 기존 금융 지표와 최근 행동 지표를 결합하여 다차원 피처 벡터를 구축하며,
     * 각 레코드는 정답값(target)과 독립 변수들이 매핑된 {@link ArrayExample} 형태로 적재됩니다.
     * </p>
     * @param csvPath 수혈 데이터가 포함된 CSV 파일의 절대 경로
     * @return {@link Regressor} 기반의 Tribuo 고도화 학습용 데이터셋
     * @throws IOException 파일 접근 및 읽기 실패 시 발생
     */
    public MutableDataset<Regressor> loadV2Dataset(String csvPath) throws IOException {

        // 라이브러리 규격에 따라 빈 출처 정보(Provenance) 객체를 전달하여 데이터셋 초기화
        MutableDataset<Regressor> dataset = new MutableDataset<>(new EmptyDatasetProvenance(), factory);

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine(); // 헤더(Header) 행 스킵
            String line;
            while ((line = br.readLine()) != null) {
                // CSV 행 파싱
                String[] v = line.split(",");

                // 5번 인덱스: 학습의 목표가 되는 로열티 점수(Target)
                double target = Double.parseDouble(v[5]);

                // 2~4번 인덱스: 유저의 기본 금융 속성 지표
                double f1 = Double.parseDouble(v[2]);
                double f2 = Double.parseDouble(v[3]);
                double f3 = Double.parseDouble(v[4]);

                // 6~8번 인덱스: 수혈된 최신 행동 지표 (활동성, 지출액, 가치 소비 비중)
                double nTx = Double.parseDouble(v[6]);
                double nAmt = Double.parseDouble(v[7]);
                double pRatio = Double.parseDouble(v[8]);

                // 정답 데이터 생성
                Regressor label = new Regressor("Score", target);

                // 피처 벡터 구성: 명시적 명칭 부여를 통해 모델의 해석력(Explainability) 확보
                List<Feature> features = new ArrayList<>();
                features.add(new Feature("feature_1", f1));
                features.add(new Feature("feature_2", f2));
                features.add(new Feature("feature_3", f3));
                features.add(new Feature("new_tx_count", nTx));
                features.add(new Feature("new_total_amt", nAmt));
                features.add(new Feature("premium_ratio", pRatio));

                // 데이터셋에 훈련 예제 추가
                dataset.add(new ArrayExample<>(label, features));
            }
        }
        log.info("고도화 데이터셋 변환 완료 (총 {} 건)", dataset.size());
        return dataset;
    }
}