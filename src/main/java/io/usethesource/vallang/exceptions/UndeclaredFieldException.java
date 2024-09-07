package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class UndeclaredFieldException extends FactTypeUseException {
    private static final long serialVersionUID = 8997399464492627705L;
    private Type type;
    private String label;

    public UndeclaredFieldException(Type type, String label) {
        super(type + " does not have a field with label " + label + " declared for it");
        this.type = type;
        this.label = label;
    }

    public Type getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }
}
