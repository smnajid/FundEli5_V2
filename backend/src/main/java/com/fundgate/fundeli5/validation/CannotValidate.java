package com.fundgate.fundeli5.validation;

import java.util.List;

/** The card is not ready to validate; {@code reasons} says what the validator still has to do. */
public class CannotValidate extends RuntimeException {

    private final List<String> reasons;

    public CannotValidate(List<String> reasons) {
        super("The card cannot be validated: " + String.join("; ", reasons));
        this.reasons = List.copyOf(reasons);
    }

    public List<String> reasons() {
        return reasons;
    }
}
