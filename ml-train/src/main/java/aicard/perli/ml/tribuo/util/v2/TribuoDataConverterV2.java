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
 * <p>[V2] 고도화 피처를 Tribuo 데이터셋 규격으로 변환하는 유틸리티 클래스입니다.</p>
 *
 * <p>수혈된 최신 행동 데이터(최근 건수, 금액, 프리미엄 비중)를 포함하여 학습용 데이터셋을 구성하며,
 * Tribuo 라이브러리 명세에 따라 {@link EmptyDatasetProvenance}를 사용하여 데이터셋을 초기화합니다.</p>
 */
@Slf4j
public class TribuoDataConverterV2 {

    /** Tribuo Regression 모델 생성을 위한 팩토리 */
    private final RegressionFactory factory = new RegressionFactory();

    /**
     * <p>고도화 CSV 파일을 읽어 XGBoost 학습에 최적화된 MutableDataset을 생성합니다.</p>
     *
     * @param csvPath 수혈 데이터가 포함된 CSV 파일의 절대 경로
     * @return {@link Regressor} 기반의 Tribuo 데이터셋
     * @throws IOException 파일 접근 및 읽기 실패 시
     */
    public MutableDataset<Regressor> loadV2Dataset(String csvPath) throws IOException {
        // 라이브러리 규격에 따라 EmptyDatasetProvenance 객체 전달
        MutableDataset<Regressor> dataset = new MutableDataset<>(new EmptyDatasetProvenance(), factory);

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            br.readLine(); // 헤더 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");

                // 데이터 수혈 및 매핑
                double target = Double.parseDouble(v[5]);    // 로열티 점수
                double f1 = Double.parseDouble(v[2]);        // 기본 피처 1
                double f2 = Double.parseDouble(v[3]);        // 기본 피처 2
                double f3 = Double.parseDouble(v[4]);        // 기본 피처 3
                double nTx = Double.parseDouble(v[6]);       // 최근 거래 건수
                double nAmt = Double.parseDouble(v[7]);      // 최근 거래 금액
                double pRatio = Double.parseDouble(v[8]);    // 프리미엄 결제 비중 (수혈)

                Regressor label = new Regressor("Score", target);
                List<Feature> features = new ArrayList<>();
                features.add(new Feature("feature_1", f1));
                features.add(new Feature("feature_2", f2));
                features.add(new Feature("feature_3", f3));
                features.add(new Feature("new_tx_count", nTx));
                features.add(new Feature("new_total_amt", nAmt));
                features.add(new Feature("premium_ratio", pRatio));

                dataset.add(new ArrayExample<>(label, features));
            }
        }
        log.info("데이터셋 변환 완료 (총 {} 건)", dataset.size());
        return dataset;
    }
}