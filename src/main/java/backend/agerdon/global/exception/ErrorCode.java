package backend.agerdon.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON-400", "요청 본문을 읽을 수 없습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-400", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON-415", "지원하지 않는 미디어 타입입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-401", "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-403", "접근 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH-001", "사용자를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH-002", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-003", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // Taxi (Kakao Mobility)
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "TAXI-001", "카카오 길찾기 API 호출에 실패했습니다."),
    KAKAO_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "TAXI-002", "카카오 길찾기 API 요청 값이 올바르지 않습니다."),

    // Metro (서울 열린데이터광장)
    METRO_API_ERROR(HttpStatus.BAD_GATEWAY, "METRO-001", "지하철 실시간 도착정보 API 호출에 실패했습니다."),
    METRO_TIMETABLE_EMPTY(HttpStatus.NOT_FOUND, "METRO-002", "실시간 도착정보가 없습니다."),

    // Bus (공공데이터포털)
    BUS_API_ERROR(HttpStatus.BAD_GATEWAY, "BUS-001", "버스 도착정보 API 호출에 실패했습니다."),
    BUS_STOP_EMPTY(HttpStatus.NOT_FOUND, "BUS-002", "정류소 데이터가 없습니다."),

    // Trip
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "TRIP-001", "여정을 찾을 수 없습니다."),
    TRIP_IN_PROGRESS(HttpStatus.CONFLICT, "TRIP-002", "결과를 입력하지 않은 여정이 이미 존재합니다."),
    TRIP_RESULT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "TRIP-003", "이미 결과가 입력된 여정입니다."),
    ROUTE_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "TRIP-004", "이동 가능한 경로를 찾을 수 없습니다."),

    // Member (마이페이지)
    FAVORITE_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-001", "즐겨찾기 장소를 찾을 수 없습니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER-002", "존재하지 않는 장소입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
