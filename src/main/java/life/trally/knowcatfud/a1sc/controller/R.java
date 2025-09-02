package life.trally.knowcatfud.a1sc.controller;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class R<T> {
    private Boolean success;
    private Integer code;
    private String message;
    private T data;

    public static <T> R<T> ok() {
        return new <T>R<T>().success(true).code(200);
    }

    // 为了支持利用类型推断来简化代码
    public static <T> R<T> ok(int code) {
        return new R<T>().success(true).code(code);
    }

    public static <T> R<T> ok(String message) {
        return new R<T>().success(true).code(200).message(message);
    }

    public static <T> R<T> ok(int code, String message) {
        return new R<T>().success(true).code(code).message(message);
    }

    public static <T> R<T> ok(T data) {
        return new R<T>().success(true).code(200).message("success").data(data);
    }

    public static <T> R<T> error() {
        return new R<T>().success(false).code(500);
    }

    public static <T> R<T> error(int code) {
        return new R<T>().success(false).code(code);
    }

    public static <T> R<T> error(String message) {
        return new R<T>().success(false).code(500).message(message);
    }

    public static <T> R<T> error(int code, String message) {
        return new R<T>().success(false).code(code).message(message);
    }

    public static <T> R<T> error(T data) {
        return new R<T>().success(false).code(500).message("error").data(data);
    }

    public R<T> success(Boolean success) {
        this.success = success;
        return this;
    }

    public R<T> message(String message) {
        this.message = message;
        return this;
    }

    public R<T> code(Integer code) {
        this.code = code;
        return this;
    }

    public R<T> data(T data) {
        this.data = data;
        return this;
    }

}
