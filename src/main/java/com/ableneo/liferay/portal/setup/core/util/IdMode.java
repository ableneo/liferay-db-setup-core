package com.ableneo.liferay.portal.setup.core.util;

public enum IdMode {
    ID("ID"),
    PLID("PLID"),
    UUID("UUID");

    private final String text;

    private IdMode(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
