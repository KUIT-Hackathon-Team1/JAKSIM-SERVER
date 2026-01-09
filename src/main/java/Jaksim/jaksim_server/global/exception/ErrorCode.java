package Jaksim.jaksim_server.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_DAYINDEX_RANGE(HttpStatus.BAD_REQUEST, "dayIndex의 범위가 올바르지 않습니다"),
    FUTURE_SELECTION(HttpStatus.BAD_REQUEST, "아직 진행할 수 없는 일자입니다"),
    PAST_ONLY_MEMO(HttpStatus.BAD_REQUEST, "지난 일차는 메모만 수정할 수 있습니다."),
    PAST_ACCESS(HttpStatus.BAD_REQUEST, "이미 종료한 일차는 달성 여부를 수정할 수 없습니다."),
    MUST_SELECT_BEFORE_DAY(HttpStatus.BAD_REQUEST, "하루 끝내기 전에 달성 여부를 선택해야 합니다."),


    ACCESS_TO_FINISHED(HttpStatus.CONFLICT, "종료된 run은 수정할 수 없습니다"),
    ALREADY_PROGRESS(HttpStatus.CONFLICT, "해당 목표에 이미 진행 중인 챌린지가 존재합니다"),

    NONE_GOAL(HttpStatus.NOT_FOUND, "목표가 존재하지 않거나 해당 사용자의 목표가 아닙니다"),
    NONE_RECORD(HttpStatus.NOT_FOUND, "도전 기록이 없습니다"),
    NONE_DATE(HttpStatus.NOT_FOUND, "해당 일차가 존재하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),

    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
