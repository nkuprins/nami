package com.app.backend.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSRF token priming endpoint. A fresh visitor may issue a state-changing
 * request (e.g. presigning an upload) before any prior response has planted the
 * {@code XSRF-TOKEN} cookie, which would 403. The frontend hits this once before
 * its first mutation to plant the cookie via {@code CsrfCookieFilter}, so the
 * mutation is accepted on its first try. Deliberately DB-free — priming must not
 * wake the database.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
