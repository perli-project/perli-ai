package aicard.perli.dl.lstm.util.loader.v2;

import aicard.perli.dl.lstm.dto.request.v2.LstmAdvancedRequestV2;
import java.io.*;
import java.util.*;

/**
 * 고도화된 V2 학습 데이터셋(train_lstm_v2.csv) 전용 데이터 로더 클래스입니다.
 * <p>
 * 기존의 6개월 시계열 결제 데이터에 더해, 사용자의 성별, 연령, 교육 수준 등
 * 인구통계학적 특성을 통합적으로 파싱하여 V2 규격의 고도화된 DTO 구조로 변환합니다.
 * </p>
 */
public class LstmDataLoaderV2 {

    /**
     * 고도화된 통합 CSV 파일을 읽어 LSTM V2 학습용 DTO 리스트로 변환합니다.
     * <p>
     * CSV의 평면적 인덱스 구조를 분석하여 인구통계 정보(정적 피처)와 6개월간의 금융 흐름(시계열 피처)을
     * 분리 및 재조합하며, 모델의 타겟 라벨과 확장 분석 지표까지 정밀하게 매핑하여 학습 데이터를 완성합니다.
     * </p>
     * @param path 고도화 통합 데이터셋 CSV 파일 경로
     * @return 인구통계 및 시퀀스 데이터가 결합된 LstmAdvancedRequestV2 객체 리스트
     * @throws IOException 파일 읽기 실패 또는 수치 파싱 중 오류 발생 시
     */
    public List<LstmAdvancedRequestV2> loadTrainingData(String path) throws IOException {
        List<LstmAdvancedRequestV2> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 데이터 정합성을 위해 헤더 행 스킵
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                LstmAdvancedRequestV2 dto = new LstmAdvancedRequestV2();

                // 기본 식별 ID 및 카드 한도 정보 매핑
                dto.setId(v[0]);
                dto.setLimitBal(Double.parseDouble(v[1]));

                // 추가 인구통계학적 정보 추출: 사용자의 사회적/개인적 특성을 정의하는 피처
                dto.setSex(Integer.parseInt(v[2]));       // 성별 정보 (1: 남성, 2: 여성)
                dto.setEducation(Integer.parseInt(v[3])); // 최종 학력 수준
                dto.setMarriage(Integer.parseInt(v[4]));  // 결혼 상태
                dto.setAge(Integer.parseInt(v[5]));       // 연령층

                // 6개월간의 결제 상태(PAY_0 ~ PAY_6) 시퀀스 데이터 배열화
                int[] status = new int[6];
                for(int i=0; i<6; i++) status[i] = Integer.parseInt(v[6+i]);
                dto.setPayStatus(status);

                // 6개월간의 청구 금액(BILL_AMT) 흐름 데이터 배열화
                double[] bills = new double[6];
                for(int i=0; i<6; i++) bills[i] = Double.parseDouble(v[12+i]);
                dto.setBillAmts(bills);

                // 6개월간의 실제 결제 금액(PAY_AMT) 흐름 데이터 배열화
                double[] pays = new double[6];
                for(int i=0; i<6; i++) pays[i] = Double.parseDouble(v[18+i]);
                dto.setPayAmts(pays);

                // 예측 목표(Label) 및 확장 분석 지표(통계/성향 피처) 매핑
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