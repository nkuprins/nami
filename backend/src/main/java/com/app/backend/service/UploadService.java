package com.app.backend.service;

import com.app.backend.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadService {

    private final S3Client s3Client;
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

    public void deleteObjects(List<String> cdnUrls) {
        if (cdnUrls.isEmpty()) return;

        List<ObjectIdentifier> keys = cdnUrls.stream()
                .map(url -> url.replace(cdnUrl + "/", ""))
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(keys).build())
                .build());
    }
}
