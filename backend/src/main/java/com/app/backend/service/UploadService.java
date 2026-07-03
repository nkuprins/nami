package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AppProperties props;

    public List<PresignResponse> presign(List<String> filenames) {
        return filenames.stream().map(this::presignOne).toList();
    }

    private PresignResponse presignOne(String filename) {
        String safeName = filename.replaceAll("[/\\\\]", "_");
        String key = "uploads/" + UUID.randomUUID() + "/" + safeName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(props.s3().bucket())
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(props.s3().presignTtlMinutes()))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String fileUrl = props.s3().cdnUrl() + "/" + key;

        return new PresignResponse(presigned.url().toString(), fileUrl);
    }

    /**
     * Deletes the given media from S3. Returns the CDN URLs that were <em>not</em> confirmed deleted
     * (per-object failures reported by S3); an empty list means everything was removed. Callers use
     * the result to decide what still needs retrying — S3 delete is idempotent, so retrying a URL
     * whose object is already gone is harmless.
     */
    public List<String> deleteObjects(List<String> cdnUrls) {
        if (cdnUrls.isEmpty()) return List.of();

        String prefix = props.s3().cdnUrl() + "/";
        Map<String, String> urlByKey = new LinkedHashMap<>();
        for (String url : cdnUrls) {
            urlByKey.put(url.replace(prefix, ""), url);
        }

        List<ObjectIdentifier> keys = urlByKey.keySet().stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

        DeleteObjectsResponse response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(props.s3().bucket())
                .delete(Delete.builder().objects(keys).build())
                .build());

        if (response.errors().isEmpty()) return List.of();

        List<String> failedUrls = response.errors().stream()
                .map(error -> urlByKey.get(error.key()))
                .filter(Objects::nonNull)
                .toList();
        log.warn("S3 delete failed for {} object(s): {}",
                failedUrls.size(),
                response.errors().stream().map(S3Error::code).distinct().toList());
        return failedUrls;
    }
}
