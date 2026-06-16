package com.app.backend.service;

import com.app.backend.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.presign-ttl-minutes}")
    private int presignTtlMinutes;

    @Value("${app.s3.cdn-url}")
    private String cdnUrl;

    public List<PresignResponse> presign(List<String> filenames) {
        return filenames.stream().map(this::presignOne).toList();
    }

    private PresignResponse presignOne(String filename) {
        String safeName = filename.replaceAll("[/\\\\]", "_");
        String key = "uploads/" + UUID.randomUUID() + "/" + safeName;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignTtlMinutes))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String fileUrl = cdnUrl + "/" + key;

        return new PresignResponse(presigned.url().toString(), fileUrl);
    }
}
