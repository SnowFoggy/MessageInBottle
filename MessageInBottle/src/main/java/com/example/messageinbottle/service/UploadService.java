package com.example.messageinbottle.service;

import com.example.messageinbottle.config.QiniuProperties;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final QiniuProperties qiniuProperties;
    private final UploadManager uploadManager;

    public UploadService(QiniuProperties qiniuProperties) {
        this.qiniuProperties = qiniuProperties;
        Configuration configuration = new Configuration(Region.autoRegion());
        this.uploadManager = new UploadManager(configuration);
    }

    public String uploadTaskProof(MultipartFile file, Long taskId, Long userId) {
        validateFile(file, "请上传任务完成凭证图片");
        String key = buildTaskProofKey(taskId, userId, file.getOriginalFilename());
        return uploadToQiniu(file, key, "任务凭证上传失败");
    }

    public String uploadTaskImage(MultipartFile file, Long userId) {
        validateFile(file, "请上传任务图片");
        String key = buildTaskImageKey(userId, file.getOriginalFilename());
        return uploadToQiniu(file, key, "任务图片上传失败");
    }

    public String uploadUserAvatar(MultipartFile file, Long userId) {
        validateFile(file, "请上传头像图片");
        String key = buildAvatarKey(userId, file.getOriginalFilename());
        return uploadToQiniu(file, key, "头像上传失败");
    }

    private String uploadToQiniu(MultipartFile file, String key, String failMessage) {
        String uploadToken = Auth.create(qiniuProperties.getAccessKey(), qiniuProperties.getSecretKey())
                .uploadToken(qiniuProperties.getBucket());

        try (InputStream inputStream = file.getInputStream()) {
            Response response = uploadManager.put(inputStream, key, uploadToken, null, null);
            if (!response.isOK()) {
                log.warn("Qiniu upload failed: statusCode={}, body={}, reqId={}", response.statusCode, response.bodyString(), response.reqId);
                throw new IllegalArgumentException(failMessage);
            }
            return buildFileUrl(key);
        } catch (QiniuException exception) {
            String responseBody = null;
            String reqId = null;
            try {
                if (exception.response != null) {
                    responseBody = exception.response.bodyString();
                    reqId = exception.response.reqId;
                }
            } catch (QiniuException ignored) {
            }
            log.error("Qiniu upload exception: code={}, reqId={}, body={}", exception.code(), reqId, responseBody, exception);
            throw new IllegalArgumentException(failMessage);
        } catch (IOException exception) {
            log.error("Read upload file failed", exception);
            throw new IllegalArgumentException("读取上传文件失败");
        }
    }

    private void validateFile(MultipartFile file, String emptyMessage) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        if (!StringUtils.hasText(qiniuProperties.getAccessKey())
                || !StringUtils.hasText(qiniuProperties.getSecretKey())
                || !StringUtils.hasText(qiniuProperties.getBucket())) {
            throw new IllegalArgumentException("请先完成七牛云配置");
        }
    }

    private String buildTaskProofKey(Long taskId, Long userId, String originalFilename) {
        return "task-proof/" + userId + "/" + taskId + "-" + UUID.randomUUID() + extractExtension(originalFilename);
    }

    private String buildTaskImageKey(Long userId, String originalFilename) {
        return "task-image/" + userId + "/image-" + UUID.randomUUID() + extractExtension(originalFilename);
    }

    private String buildAvatarKey(Long userId, String originalFilename) {
        return "user-avatar/" + userId + "/avatar-" + UUID.randomUUID() + extractExtension(originalFilename);
    }

    private String extractExtension(String originalFilename) {
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return ".jpg";
    }

    private String buildFileUrl(String key) {
        String domain = qiniuProperties.getDomain();
        if (!StringUtils.hasText(domain)) {
            return "http://" + qiniuProperties.getBucket() + ".clouddn.com/" + key;
        }
        String normalizedDomain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        return normalizedDomain + "/" + key;
    }
}
