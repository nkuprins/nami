package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Rejects photo/plan URLs that don't point at our own CDN. These strings are stored verbatim,
 * rendered as {@code <img src>} to every viewer, and later handed to the image worker as an S3 key,
 * so an arbitrary URL is both an SSRF-on-viewer and an arbitrary-bucket-key risk. ({@code videoUrl}
 * is a legitimately external link and is not part of {@code allMediaUrls}.)
 */
@Service
@RequiredArgsConstructor
public class MediaUrlValidator {

    private final AppProperties props;

    public void validate(List<String> mediaUrls) {
        String prefix = props.s3().cdnUrl() + "/";
        for (String url : mediaUrls) {
            if (!url.startsWith(prefix)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Media URL is not an uploaded file: " + url);
            }
        }
    }
}
