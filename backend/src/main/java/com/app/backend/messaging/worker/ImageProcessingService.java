package com.app.backend.messaging.worker;

import com.app.backend.config.AppProperties;
import com.app.backend.messaging.MediaVariants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Generates resized variants of an uploaded photo and stores them back in S3 under derived keys.
 * Idempotent: re-processing the same key just overwrites the variants, which is what makes safe
 * at-least-once delivery (and manual reprocessing) possible.
 *
 * <p>Part of the worker role: it depends only on the message, S3, and config — never on the DB —
 * so it stays cleanly extractable (enforced by an ArchUnit boundary rule on {@code messaging.worker}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageProcessingService {

    /** Reject decompression bombs before allocating a full raster. */
    private static final long MAX_PIXELS = 40_000_000L;

    /** Cap the download so a large object can't OOM the worker before the pixel guard runs. */
    private static final long MAX_BYTES = 15L * 1024 * 1024;

    private final S3Client s3Client;
    private final AppProperties props;

    public void process(String originalCdnUrl) {
        String key = keyFromCdnUrl(originalCdnUrl);
        guardSize(key);
        byte[] original = download(key);
        guardDimensions(original);

        String format = formatFor(key);
        String contentType = "png".equals(format) ? "image/png" : "image/jpeg";
        for (MediaVariants.Variant variant : MediaVariants.ALL) {
            byte[] resized = resize(original, variant.maxWidth(), format);
            upload(MediaVariants.derive(key, variant.suffix()), resized, contentType);
        }
        log.info("Generated {} variants for {}", MediaVariants.ALL.size(), key);
    }
    
    private String keyFromCdnUrl(String originalCdnUrl) {
        String prefix = props.s3().cdnUrl() + "/";
        if (!originalCdnUrl.startsWith(prefix)) {
            throw new IllegalStateException("Not a CDN URL: " + originalCdnUrl);
        }
        return originalCdnUrl.substring(prefix.length());
    }

    private void guardSize(String key) {
        long size = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(props.s3().bucket())
                .key(key)
                .build()).contentLength();
        if (size > MAX_BYTES) {
            throw new IllegalStateException("Image exceeds size limit: " + size);
        }
    }

    private byte[] download(String key) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(props.s3().bucket())
                .key(key)
                .build()).asByteArray();
    }

    private void upload(String key, byte[] bytes, String contentType) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(props.s3().bucket())
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes));
    }

    private static void guardDimensions(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IllegalStateException("Unsupported or unreadable image");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new IllegalStateException("Image exceeds pixel limit: " + pixels);
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read image dimensions", e);
        }
    }

    private static byte[] resize(byte[] original, int maxWidth, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(original))
                    .width(maxWidth)
                    .outputFormat(format)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not resize image", e);
        }
    }

    private static String formatFor(String key) {
        return key.toLowerCase().endsWith(".png") ? "png" : "jpg";
    }
}
