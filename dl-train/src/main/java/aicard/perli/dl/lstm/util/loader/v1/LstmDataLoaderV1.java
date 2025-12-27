package aicard.perli.dl.lstm.util.loader.v1;

import aicard.perli.dl.lstm.dto.request.advanced.v1.LstmAdvancedRequestV1;
import java.io.*;
import java.util.*;

/**
 * 통합 학습 데이터셋(train_lstm_v1.csv) 전용 로더.
 * CSV의 평면적 구조를 LSTM 모델용 시계열 배열 구조(DTO)로 변환하여 로드함.
 */
public class LstmDataLoaderV1 {

    /**
     * CSV 파일을 읽어 학습용 DTO 리스트로 변환함.
     * UCI 시계열(결제/청구/상태)을 각각 6개 사이즈의 배열로 그룹화하여 매핑.
     * * @param path 통합 CSV 파일 경로
     * @return LstmAdvancedRequestV1 리스트
     * @throws IOException 파일 읽기 실패 시 발생
     */
    public List<LstmAdvancedRequestV1> loadTrainingData(String path) throws IOException {
        List<LstmAdvancedRequestV1> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 헤더 건너뛰기
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                LstmAdvancedRequestV1 dto = new LstmAdvancedRequestV1();

                dto.setId(v[0]);
                dto.setLimitBal(Double.parseDouble(v[1]));

                // 6개월치 청구 금액(BILL_AMT) 배열화 (Index 12~17)
                double[] bills = new double[6];
                for(int i=0; i<6; i++) bills[i] = Double.parseDouble(v[12+i]);
                dto.setBillAmts(bills);

                // 6개월치 결제 금액(PAY_AMT) 배열화 (Index 18~23)
                double[] pays = new double[6];
                for(int i=0; i<6; i++) pays[i] = Double.parseDouble(v[18+i]);
                dto.setPayAmts(pays);

                // 6개월치 결제 상태(PAY_0~6) 배열화 (Index 6~11)
                int[] status = new int[6];
                for(int i=0; i<6; i++) status[i] = Integer.parseInt(v[6+i]);
                dto.setPayStatus(status);

                // 라벨 및 확장 피처(살) 매핑 (Index 24~30)
                dto.setLabel(Integer.parseInt(v[24]));
                dto.setTotalAmount(Double.parseDouble(v[25]));
                dto.setTxCount(Integer.parseInt(v[26]));
                dto.setAvgInstallments(Double.parseDouble(v[27]));
                dto.setAuthorizedRatio(Double.parseDouble(v[28]));
                dto.setBalance(Double.parseDouble(v[29]));
                dto.setPurchases(Double.parseDouble(v[30]));

                list.add(dto);
            }
        }
        return list;
    }
}