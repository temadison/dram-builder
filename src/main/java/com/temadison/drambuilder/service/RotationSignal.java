package com.temadison.drambuilder.service;

public enum RotationSignal {
    HOLD_DRAM("HOLD DRAM"),
    ROTATE_TO_SK_HYNIX("ROTATE TO SK HYNIX"),
    WAIT("WAIT"),
    AVOID_ADDING("AVOID ADDING");

    private final String displayName;

    RotationSignal(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
