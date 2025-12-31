package aicard.perli.dl.lstm.util.loader.v1;

import aicard.perli.dl.lstm.dto.request.v1.LstmAdvancedRequestV1;
import java.io.*;
import java.util.*;

/**
 * 통합 학습 데이터셋(train_lstm_v1.csv) 전용 데이터 로더 클래스입니다.
 * <p>
 * CSV 파일의 평면적인 구조를 분석하여, LSTM 모델이 시계열 흐름을 파악할 수 있도록
 * 개별 필드들을 6개월 단위의 배열 구조(DTO)로 재구성하여 메모리에 로드합니다.
 * </p>
 */
public class LstmDataLoaderV1 {

    /**
     * 지정된 경로의 CSV 파일을 읽어 LSTM 학습용 DTO 리스트로 변환합니다.
     * <p>
     * UCI 신용 데이터의 특성에 맞춰 청구 금액, 결제 금액, 결제 상태 정보를 각각 6개의 사이즈를 가진
     * 배열로 그룹화하며, 모델의 정답지(Label) 및 확장 분석 피처들을 함께 매핑하여 완전한 학습 데이터셋을 구축합니다.
     * </p>
     * @param path 통합 CSV 파일이 위치한 물리적 경로
     * @return 시계열 데이터가 배열화된 LstmAdvancedRequestV1 객체 리스트
     * @throws IOException 파일 읽기 실패 또는 데이터 파싱 중 오류 발생 시
     */
    public List<LstmAdvancedRequestV1> loadTrainingData(String path) throws IOException {
        List<LstmAdvancedRequestV1> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 데이터 정합성을 위해 헤더 행 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                LstmAdvancedRequestV1 dto = new LstmAdvancedRequestV1();

                // 기본 식별 정보 및 카드 한도 설정
                dto.setId(v[0]);
                dto.setLimitBal(Double.parseDouble(v[1]));

                // 6개월간의 청구 금액(BILL_AMT) 흐름을 배열화하여 시계열 데이터 생성
                double[] bills = new double[6];
                for(int i=0; i<6; i++) bills[i] = Double.parseDouble(v[12+i]);
                dto.setBillAmts(bills);

                // 6개월간의 실제 결제 금액(PAY_AMT) 흐름을 배열화
                double[] pays = new double[6];
                for(int i=0; i<6; i++) pays[i] = Double.parseDouble(v[18+i]);
                dto.setPayAmts(pays);

                // 6개월간의 결제 이행 상태(PAY_0 ~ PAY_6) 시퀀스 데이터 매핑
                int[] status = new int[6];
                for(int i=0; i<6; i++) status[i] = Integer.parseInt(v[6+i]);
                dto.setPayStatus(status);

                // 학습 타겟(Label) 및 통계적 확장 분석 지표 매핑
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