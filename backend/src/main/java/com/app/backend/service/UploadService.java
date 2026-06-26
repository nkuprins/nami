package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.PresignResponse;
import lombok.RequiredArgsConstructor;
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

    public void deleteObjects(List<String> cdnUrls) {
        if (cdnUrls.isEmpty()) return;

        List<ObjectIdentifier> keys = cdnUrls.stream()
                .map(url -> url.replace(props.s3().cdnUrl() + "/", ""))
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

        s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(props.s3().bucket())
                .delete(Delete.builder().objects(keys).build())
                .build());
    }
}
