package com.app.backend.security;

/** The verified subset of a Google ID token we act on. {@code sub} is Google's stable user id. */
public record GoogleUser(String sub, String email, String name) {}
