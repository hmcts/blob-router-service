package uk.gov.hmcts.reform.blobrouter.util;

import java.util.function.BooleanSupplier;

public class Condition {

    private final BooleanSupplier check;
    private final String message;

    public Condition(BooleanSupplier check, String message) {
        this.check = check;
        this.message = message;
    }

    public boolean isMet() {
        return check.getAsBoolean();
    }

    public String getMessage() {
        return message;
    }
}
