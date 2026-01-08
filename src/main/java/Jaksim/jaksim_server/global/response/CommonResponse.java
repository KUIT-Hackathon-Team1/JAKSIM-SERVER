package Jaksim.jaksim_server.global.response;

import Jaksim.jaksim_server.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommonResponse<T> {
    private boolean success;
    private T data;
    private ErrorResponse error;

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(true, data, null);
    }

    public static CommonResponse<?> error(ErrorCode errorCode) {
        return new CommonResponse<>(false, null, ErrorResponse.from(errorCode));
    }
}
