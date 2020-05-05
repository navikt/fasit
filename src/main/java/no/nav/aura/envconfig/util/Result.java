package no.nav.aura.envconfig.util;

public final class Result {

    private boolean ok;
    private String message;

    private Result(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static Result ok() {
        return new Result(true, "");
    }

    public static Result error(String message) {
        return new Result(false, message);
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

}
