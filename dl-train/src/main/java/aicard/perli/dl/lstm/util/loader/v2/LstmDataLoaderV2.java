package aicard.perli.dl.lstm.util.loader.v2;

import aicard.perli.dl.lstm.dto.request.advanced.v2.LstmAdvancedRequestV2;
import java.io.*;
import java.util.*;

/**
 * 고도화된 학습 데이터셋 로더.
 * 기존 시계열 데이터와 인구통계학적 피처(SEX, AGE 등)를 통합하여 로드함.
 */
public class LstmDataLoaderV2 {

    /**
     * CSV 파일을 읽어 V2용 고도화 DTO 리스트로 변환함.
     * * @param path 통합 CSV 파일 경로
     * @return LstmAdvancedRequestV2 리스트
     * @throws IOException 파일 읽기 실패 시 발생
     */
    public List<LstmAdvancedRequestV2> loadTrainingData(String path) throws IOException {
        List<LstmAdvancedRequestV2> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine(); // 헤더 건너뛰기
            String line;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                LstmAdvancedRequestV2 dto = new LstmAdvancedRequestV2();

                // 기본 ID 및 한도 정보
                dto.setId(v[0]);
                dto.setLimitBal(Double.parseDouble(v[1]));

                // 추가 인구통계학적 정보 (Index 2~5)
                dto.setSex(Integer.parseInt(v[2]));       // 성별
                dto.setEducation(Integer.parseInt(v[3])); // 교육 수준
                dto.setMarriage(Integer.parseInt(v[4]));  // 결혼 여부
                dto.setAge(Integer.parseInt(v[5]));       // 나이

                // 6개월치 결제 상태(PAY_0~6) 배열화 (Index 6~11)
                int[] status = new int[6];
                for(int i=0; i<6; i++) status[i] = Integer.parseInt(v[6+i]);
                dto.setPayStatus(status);

                // 6개월치 청구 금액(BILL_AMT) 배열화 (Index 12~17)
                double[] bills = new double[6];
                for(int i=0; i<6; i++) bills[i] = Double.parseDouble(v[12+i]);
                dto.setBillAmts(bills);

                // 6개월치 결제 금액(PAY_AMT) 배열화 (Index 18~23)
                double[] pays = new double[6];
                for(int i=0; i<6; i++) pays[i] = Double.parseDouble(v[18+i]);
                dto.setPayAmts(pays);

                // 라벨 및 기존 확장 피처(살) 매핑 (Index 24~30)
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