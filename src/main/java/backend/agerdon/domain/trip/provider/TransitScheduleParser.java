package backend.agerdon.domain.trip.provider;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 서울 버스/지하철 API에서 내려오는 여러 시각 형식을 서울 현지 일시로 변환한다.
 */
final class TransitScheduleParser {

    private static final DateTimeFormatter DATE_TIME_SECONDS =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATE_TIME_MINUTES =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final LocalTime SERVICE_DAY_BOUNDARY = LocalTime.of(5, 0);

    private TransitScheduleParser() {
    }

    static Optional<LocalDateTime> parse(String rawValue, LocalDateTime requestedAt) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }

        String digits = rawValue.replaceAll("\\D", "");
        try {
            return switch (digits.length()) {
                case 14 -> Optional.of(LocalDateTime.parse(digits, DATE_TIME_SECONDS));
                case 12 -> Optional.of(LocalDateTime.parse(digits, DATE_TIME_MINUTES));
                case 6 -> Optional.of(resolveServiceDate(
                        LocalTime.parse(digits, DateTimeFormatter.ofPattern("HHmmss")),
                        requestedAt
                ));
                case 4 -> Optional.of(resolveServiceDate(
                        LocalTime.parse(digits, DateTimeFormatter.ofPattern("HHmm")),
                        requestedAt
                ));
                default -> Optional.empty();
            };
        } catch (DateTimeException exception) {
            return Optional.empty();
        }
    }

    private static LocalDateTime resolveServiceDate(LocalTime serviceTime, LocalDateTime requestedAt) {
        LocalDate date = requestedAt.toLocalDate();

        // 23시 이후 조회한 00~04시대 막차는 다음 달력 날짜에 운행한다.
        if (!serviceTime.isBefore(SERVICE_DAY_BOUNDARY)
                || requestedAt.toLocalTime().isBefore(SERVICE_DAY_BOUNDARY)) {
            return LocalDateTime.of(date, serviceTime);
        }
        return LocalDateTime.of(date.plusDays(1), serviceTime);
    }
}
