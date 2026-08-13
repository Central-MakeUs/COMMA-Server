package com.cmc.comma.global.storage;

import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * OCI Object Storage(S3 호환)에 이미지를 업로드하고, 공개 버킷 객체의 접근 URL을 만든다.
 * DB에는 URL이 아니라 객체 키(key)만 저장하고, 조회 시점에 고정 공개 URL을 조립한다.
 * (버킷 Visibility가 Public이므로 서명 없이 영구 URL로 접근 가능 — 캐싱에 유리)
 *
 * 버킷 리전이 사용자와 물리적으로 멀어(브라질) 왕복 지연이 크므로, 업로드 시점에
 * 리사이징/재압축해서 전송량 자체를 줄인다(긴 변 {@link #MAX_DIMENSION}px, 품질 {@link #JPEG_QUALITY}).
 */
@Slf4j
@Service
public class StorageService {

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    // Thumbnailator는 ImageIO 기반이라 순정 상태론 WebP를 디코딩 못 한다 — 리사이징은 이 포맷들만, WebP는 원본 그대로 업로드.
    private static final Set<String> RESIZABLE_TYPES = Set.of("image/jpeg", "image/png");
    private static final int MAX_DIMENSION = 1080;
    private static final double JPEG_QUALITY = 0.85;

    private final S3Client s3Client;
    private final String bucket;
    private final String endpoint;

    public StorageService(S3Client s3Client,
                          @Value("${oci.storage.bucket}") String bucket,
                          @Value("${oci.storage.endpoint}") String endpoint) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.endpoint = endpoint;
    }

    /** 이미지를 (가능하면 리사이징/재압축해서) 업로드하고 객체 키를 반환한다. */
    public String upload(MultipartFile file, String keyPrefix) {
        validate(file);
        String key = keyPrefix + "/" + UUID.randomUUID() + extension(file);
        try {
            byte[] body = RESIZABLE_TYPES.contains(file.getContentType())
                    ? resize(file)
                    : file.getBytes();
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(body));
        } catch (IOException e) {
            log.error("[STORAGE] 업로드 실패 key={}", key, e);
            throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return key;
    }

    /**
     * 긴 변 기준 {@link #MAX_DIMENSION}px로 축소하고 품질 {@link #JPEG_QUALITY}로 재압축한다.
     * Thumbnailator는 원본보다 작은 크기를 요청해도 확대해버리므로(내장 방지 기능 없음),
     * 원본 가로/세로가 이미 {@link #MAX_DIMENSION} 이하면 리사이징 없이 원본 바이트를 그대로 쓴다.
     */
    private byte[] resize(MultipartFile file) throws IOException {
        byte[] original = file.getBytes();
        BufferedImage probe = ImageIO.read(new ByteArrayInputStream(original));
        if (probe == null || (probe.getWidth() <= MAX_DIMENSION && probe.getHeight() <= MAX_DIMENSION)) {
            return original;
        }
        // Thumbnails.of(InputStream)은 스트림 내용을 스니핑해 출력 포맷을 추론한다 — BufferedImage를
        // 직접 넘기면 그 정보가 사라져 "Output format not specified" 예외가 나므로 원본 바이트를 다시 넘긴다.
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(new ByteArrayInputStream(original))
                    .size(MAX_DIMENSION, MAX_DIMENSION)
                    .outputQuality(JPEG_QUALITY)
                    .toOutputStream(out);
            return out.toByteArray();
        }
    }

    /** 공개 버킷 객체의 고정 URL을 조립한다(서명/네트워크 호출 없음). key가 null이면 null 반환. */
    public String publicUrl(String key) {
        if (key == null) {
            return null;
        }
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/" + bucket + "/" + key;
    }

    /** 객체를 삭제한다. 실패해도 흐름을 막지 않도록 예외를 삼키고 로그만 남긴다. */
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("[STORAGE] 삭제 실패 key={}", key, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CommaException(ErrorCode.IMAGE_REQUIRED);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new CommaException(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new CommaException(ErrorCode.INVALID_IMAGE_FORMAT);
        }
    }

    private String extension(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }
}