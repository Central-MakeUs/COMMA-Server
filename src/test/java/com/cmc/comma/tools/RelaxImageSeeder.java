package com.cmc.comma.tools;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * 휴식 활동(Relax) 45개에 대표 이미지를 한 번에 채워 넣는 수동 도구.
 * Spring 컨텍스트 없이 순수 S3Client + JDBC만 사용 — Kakao/Google/Apple/JWT/Redis 없이 가볍게 동작.
 *
 * 필요 env var 없으면 자동 skip (CI에는 이 값들이 없어 항상 안전하게 스킵됨):
 *   OCI_S3_ENDPOINT / OCI_S3_REGION / OCI_S3_BUCKET / OCI_S3_ACCESS_KEY / OCI_S3_SECRET_KEY
 *   DB_URL (예: jdbc:mysql://localhost:3306/comma) / DB_USER / DB_PASSWORD
 *   RELAX_IMAGE_DIR (기본값: ./relax-images)
 *
 * 운영 DB는 127.0.0.1:3306 로만 열려있어 로컬에서 바로 못 붙는다. 먼저 SSH 터널을 연다:
 *   ssh -i ~/Downloads/ssh-key-2026-07-11.key -L 3306:localhost:3306 ubuntu@167.126.10.48
 * 그 상태에서 DB_URL=jdbc:mysql://localhost:3306/comma 로 이 테스트를 실행한다:
 *   ./gradlew test --tests '*RelaxImageSeeder'
 *
 * 로컬 이미지 파일명은 relaxes.name과 정확히 일치해야 매칭된다 (예: "창 밖 바라보기.jpg").
 */
class RelaxImageSeeder {

    private static final List<String> EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    @Test
    void seedRelaxImages() throws Exception {
        String endpoint = System.getenv("OCI_S3_ENDPOINT");
        String region = System.getenv("OCI_S3_REGION");
        String bucket = System.getenv("OCI_S3_BUCKET");
        String accessKey = System.getenv("OCI_S3_ACCESS_KEY");
        String secretKey = System.getenv("OCI_S3_SECRET_KEY");
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        assumeTrue(endpoint != null && region != null && bucket != null
                && accessKey != null && secretKey != null
                && dbUrl != null && dbUser != null && dbPassword != null,
                "OCI_S3_* / DB_* env 없음 → skip (수동 시딩 도구, 평소 CI/로컬 테스트에서는 스킵됨)");

        String imageDir = System.getenv().getOrDefault("RELAX_IMAGE_DIR", "./relax-images");
        File dir = new File(imageDir);
        assumeTrue(dir.isDirectory(), "이미지 폴더 없음: " + dir.getAbsolutePath() + " → skip");

        S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        List<String> matched = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            try (PreparedStatement select = conn.prepareStatement("SELECT id, name FROM relaxes");
                    ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    String name = rs.getString("name");
                    File imageFile = findLocalImage(dir, name);
                    if (imageFile == null) {
                        unmatched.add(name);
                        continue;
                    }

                    String ext = extension(imageFile.getName());
                    String key = "relaxes/" + id + "." + ext;
                    s3Client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(key)
                                    .contentType(contentType(ext))
                                    .build(),
                            RequestBody.fromBytes(Files.readAllBytes(imageFile.toPath())));

                    try (PreparedStatement update =
                            conn.prepareStatement("UPDATE relaxes SET image_key = ? WHERE id = ?")) {
                        update.setString(1, key);
                        update.setLong(2, id);
                        update.executeUpdate();
                    }
                    matched.add(name + " -> " + key);
                }
            }
        }

        System.out.println("[SEED] 매칭 " + matched.size() + "건:");
        matched.forEach(m -> System.out.println("  ✓ " + m));
        System.out.println("[SEED] 미매칭 " + unmatched.size() + "건 (로컬에 파일 없음):");
        unmatched.forEach(m -> System.out.println("  ✗ " + m));
    }

    /** relaxName.{jpg|jpeg|png|webp} 파일을 직접 존재 여부로 탐색한다 (listFiles 비교 금지 — 한글 NFC/NFD 정규화 이슈 회피). */
    private File findLocalImage(File dir, String relaxName) {
        String normalized = Normalizer.normalize(relaxName, Form.NFC);
        for (String ext : EXTENSIONS) {
            File candidate = new File(dir, normalized + "." + ext);
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return filename.substring(dot + 1).toLowerCase();
    }

    private String contentType(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
