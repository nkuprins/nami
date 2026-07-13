package com.app.backend.service;

import com.app.backend.config.AppProperties;
import com.app.backend.dto.upload.PresignResponse;
import com.app.backend.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    @InjectMocks
    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        AppProperties.S3Properties s3 = new AppProperties.S3Properties("test-bucket", "us-east-1", 5, "https://cdn.test.local");
        AppProperties props = new AppProperties(null, s3, null, null, null, null, null, null);
        ReflectionTestUtils.setField(uploadService, "props", props);
    }

    @Test
    void presign_returnsPresignedUrlAndFileUrl() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/presigned").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        List<PresignResponse> results = uploadService.presign(List.of("photo.jpg"));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().uploadUrl()).isEqualTo("https://s3.amazonaws.com/presigned");
        assertThat(results.getFirst().fileUrl()).startsWith("https://cdn.test.local/uploads/");
        assertThat(results.getFirst().fileUrl()).endsWith("/photo.jpg");
    }

    @Test
    void presign_sanitizesFilenames() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.amazonaws.com/presigned").toURL());
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        List<PresignResponse> results = uploadService.presign(List.of("path/to/photo.jpg"));

        assertThat(results.getFirst().fileUrl()).contains("path_to_photo.jpg");
    }

    @Test
    void presign_rejectsNonImageFiles() {
        assertThatThrownBy(() -> uploadService.presign(List.of("floorplan.pdf")))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> uploadService.presign(List.of("noextension")))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void deleteObjects_callsS3WithCorrectKeys() {
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder().build());

        uploadService.deleteObjects(List.of(
                "https://cdn.test.local/uploads/abc/photo.jpg",
                "https://cdn.test.local/uploads/def/photo2.jpg"
        ));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());

        List<String> keys = captor.getValue().delete().objects().stream()
                .map(ObjectIdentifier::key)
                .toList();
        assertThat(keys).containsExactly("uploads/abc/photo.jpg", "uploads/def/photo2.jpg");
    }

    @Test
    void deleteObjects_doesNothing_whenListEmpty() {
        uploadService.deleteObjects(List.of());

        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }
}
