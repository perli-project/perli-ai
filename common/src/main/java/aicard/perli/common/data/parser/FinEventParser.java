package aicard.perli.common.data.parser;

import aicard.perli.common.model.FinEvent;
import org.apache.commons.csv.CSVRecord;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * CSV 레코드 데이터를 시스템 표준 모델인 {@link FinEvent} 객체로 변환하는 파서 클래스입니다.
 * <p>데이터 원천으로부터 읽어온 문자열 데이터를 검증하고, 날짜 및 수치 타입으로의 정밀한 변환을 담당합니다.</p>
 */
public class FinEventParser {

    /** Elo 데이터셋 표준 날짜 형식 규격 */
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * {@link CSVRecord}의 로우 데이터를 분석하여 {@link FinEvent} 도메인 모델을 생성합니다.
     *
     * @param record    Apache Commons CSV에서 제공하는 단일 행 레코드
     * @param targetMap 카드 식별자(card_id)별 타겟 점수가 매핑된 캐시 맵
     * @return 파싱 및 검증이 완료된 {@link FinEvent} 객체 (실패 시 null)
     */
    public FinEvent parse(CSVRecord record, Map<String, Double> targetMap) {
        try {
            String cardId = record.get("card_id");

            FinEvent event = new FinEvent();
            event.setCardId(cardId);

            // 시계열 분석을 위한 날짜 변환
            event.setTxDate(LocalDateTime.parse(record.get("purchase_date"), formatter));

            // 수치 및 상태값 변환
            event.setAmount(Double.parseDouble(record.get("purchase_amount")));
            event.setInstallments(Integer.parseInt(record.get("installments")));
            event.setAuthorized("Y".equalsIgnoreCase(record.get("authorized_flag")));

            // 범주형 데이터 변환
            event.setMctCatId(Integer.parseInt(record.get("merchant_category_id")));
            event.setCityId(record.get("city_id"));
            event.setStateId(record.get("state_id"));

            // 학습 정답지 매핑
            event.setTarget(targetMap.getOrDefault(cardId, 0.0));

            return event;
        } catch (Exception e) {
            // 데이터 결함 발생 시 해당 레코드를 무시하기 위해 null 반환
            return null;
        }
    }
}