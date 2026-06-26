package com.app.backend.enums;

public enum SupportedLocale {
    LV("lv"), EN("en"), RU("ru");

    public final String code;

    SupportedLocale(String code) {
        this.code = code;
    }
}
