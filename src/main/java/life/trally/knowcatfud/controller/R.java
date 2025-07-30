package life.trally.knowcatfud.controller;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Getter
public class R {
    private Boolean success;
    private Integer code;
    private String message;
    private final Map<String, Object> data = new HashMap<>();

    public static R ok() {
        R r = new R();
        r.success = true;
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static R error() {
        R r = new R();
        r.success = false;
        r.code = 500;
        r.message = "error";
        return r;
    }

    public R success(Boolean success) {
        this.success = success;
        return this;
    }

    public R message(String message) {
        this.message = message;
        return this;
    }

    public R code(Integer code) {
        this.code = code;
        return this;
    }

    public R data(String name, Object data) {
        this.data.put(name, data);
        return this;
    }

}
