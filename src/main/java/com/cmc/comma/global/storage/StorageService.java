package com.cmc.comma.global.storage;

import com.cmc.comma.global.exception.CommaException;
import com.cmc.comma.global.exception.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * OCI Object Storage(S3 호환)에 이미지를 업로드하고, 비공개 버킷 객체를 조회용 presigned URL로 변환한다.
 * DB에는 URL이 아니라 객체 키(key)만 저장하고, 조회 시점에 presigned URL을 발급한다.
 */
@Slf4j
@Service
public class StorageService {

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Duration PRESIGN_TTL = Duration.ofHours(1);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public StorageService(S3Client s3Client,
                          S3Presigner s3Presigner,
                          @Value("${oci.storage.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    /** 이미지를 업로드하고 객체 키를 반환한다. */
    public String upload(MultipartFile file, String keyPrefix) {
        validate(file);
        String key = keyPrefix + "/" + UUID.randomUUID() + extension(file);
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            log.error("[STORAGE] 업로드 실패 key={}", key, e);
            throw new CommaException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return key;
    }

    /** 비공개 버킷의 객체를 일정 시간 접근 가능한 presigned URL로 변환한다. */
    public String presignedUrl(String key) {
        GetObjectRequest getObject = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGN_TTL)
                .getObjectRequest(getObject)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
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