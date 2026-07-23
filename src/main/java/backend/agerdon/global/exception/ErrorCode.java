package backend.agerdon.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-400", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다."),

    // Taxi (Kakao Mobility)
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "TAXI-001", "카카오 길찾기 API 호출에 실패했습니다."),

    // Metro (서울 열린데이터광장)
    METRO_API_ERROR(HttpStatus.BAD_GATEWAY, "METRO-001", "지하철 시간표 API 호출에 실패했습니다."),
    METRO_TIMETABLE_EMPTY(HttpStatus.NOT_FOUND, "METRO-002", "시간표 데이터가 없습니다."),

    // Bus (공공데이터포털)
    BUS_API_ERROR(HttpStatus.BAD_GATEWAY, "BUS-001", "버스 도착정보 API 호출에 실패했습니다."),
    BUS_STOP_EMPTY(HttpStatus.NOT_FOUND, "BUS-002", "정류소 데이터가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
