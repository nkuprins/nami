package com.app.backend.messaging.worker;

import com.app.backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageProcessingServiceTest {

    private final S3Client s3Client = org.mockito.Mockito.mock(S3Client.class);
    private final AppProperties props = AppProperties.builder()
            .s3(new AppProperties.S3Properties("test-bucket", "us-east-1", 5, "https://cdn.test.local"))
            .build();

    private final ImageProcessingService service = new ImageProcessingService(s3Client, props);

    private void stubHeadSize(long bytes) {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(bytes).build());
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }

    private static int widthOf(RequestBody body) throws IOException {
        try (InputStream in = body.contentStreamProvider().newStream()) {
            return ImageIO.read(in).getWidth();
        }
    }

    @Test
    void process_generatesThumbAndCardVariants_atExpectedWidths() throws IOException {
        stubHeadSize(1_000_000);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), jpeg(1600, 1200)));

        service.process("https://cdn.test.local/uploads/x/foo.jpg");

        ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, times(2)).putObject(reqCaptor.capture(), bodyCaptor.capture());

        assertThat(reqCaptor.getAllValues()).extracting(PutObjectRequest::key)
                .containsExactly("uploads/x/foo_thumb.jpg", "uploads/x/foo_card.jpg");
        assertThat(widthOf(bodyCaptor.getAllValues().get(0))).isEqualTo(400);
        assertThat(widthOf(bodyCaptor.getAllValues().get(1))).isEqualTo(800);
    }

    @Test
    void process_rejectsUrlNotOnOurCdn() {
        assertThatThrownBy(() -> service.process("https://evil.example.com/../secret-backup.jpg"))
                .isInstanceOf(IllegalStateException.class);

        org.mockito.Mockito.verifyNoInteractions(s3Client);
    }

    @Test
    void process_rejectsObjectOverSizeLimit_withoutDownloading() {
        stubHeadSize(20L * 1024 * 1024);

        assertThatThrownBy(() -> service.process("https://cdn.test.local/uploads/x/huge.jpg"))
                .isInstanceOf(IllegalStateException.class);

        verify(s3Client, org.mockito.Mockito.never()).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    void process_rejectsUnreadableBytes() {
        stubHeadSize(1_000_000);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), "not an image".getBytes()));

        assertThatThrownBy(() -> service.process("https://cdn.test.local/uploads/x/foo.jpg"))
                .isInstanceOf(IllegalStateException.class);
    }
}
