package com.saaes.system.service;

import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.service.CommonService;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.system.client.entity.SysFile;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static com.saaes.common.core.utils.CommonUtil.getVideoDuration;

@Service
@Slf4j
public class SysFileService extends BaseServiceImpl {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/resources/uploads/";

    @Resource
    private CommonService commonService;

    /**
     * 上传文件（本地存储）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SysFile uploadFile(MultipartFile multipartFile) {
        try {
            // 确保上传目录存在
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 计算 SHA-1
            String sha1 = CommonUtil.getFileSha1(multipartFile.getInputStream());
            String sql = "SELECT * FROM sys_file WHERE sha1 = ? AND deleted IS FALSE AND status != 4";
            SysFile sysFile = baseJdbcDao.findBySql(SysFile.class, sql, sha1);
            if (sysFile != null) return sysFile;

            sysFile = new SysFile();

            // 生成文件路径
            DateTimeFormatter yyyyMM = DateTimeFormatter.ofPattern("yyyy/MM");
            String datePath = yyyyMM.format(LocalDateTime.now());
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            String filename = multipartFile.getOriginalFilename();
            String suffix = CommonUtil.getFileSuffix(filename);
            String objectPath = datePath + "/" + uuid + (suffix.isEmpty() ? "" : "." + suffix);
            File filePath = new File(UPLOAD_DIR, objectPath);

            // 确保子目录存在
            File fileDir = filePath.getParentFile();
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }

            // 保存文件
            try (InputStream inputStream = multipartFile.getInputStream()) {
                Files.copy(inputStream, filePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            objectPath = "/files/" + objectPath;
            sysFile.setObject(objectPath);
            sysFile.setSize(multipartFile.getSize());
            sysFile.setSuffix(suffix);
            sysFile.setContentType(multipartFile.getContentType());
            sysFile.setName(filename);
            sysFile.setSha1(sha1);
            sysFile.setStatus(1);
            // 生成并存储文件安全码
            sysFile.setFileCode(UUID.randomUUID().toString().replaceAll("-", ""));


            // 判断文件是否为图片文件，图片文件获取宽高，横纵比
            if (sysFile.getContentType().startsWith("image/")) {
                try (InputStream inputStream = multipartFile.getInputStream()) {

                    BufferedImage image = ImageIO.read(inputStream);
                    sysFile.setImgWidth(image.getWidth());
                    sysFile.setImgHeight(image.getHeight());
                    sysFile.setImgRatio(image.getWidth() / new BigDecimal(image.getHeight()).doubleValue());
                } catch (Exception e) {
                    log.info("非图片文件");
                }
            }

            //获取视频时长
            //如果是视频文件，抽第十帧为视频缩略图，长边压缩至40像素
            if (sysFile.getContentType().startsWith("video/")) {
                try {
                    sysFile.setVideo_duration(getVideoDuration(filePath));
                } catch (Exception e) {
                    log.error("获取视频时长失败", e);
                }
//                try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
//                    BufferedImage videoFrameImage = getVideoFrameImage(multipartFile.getInputStream(), 10);
//                    //压缩
//                    videoFrameImage = scaleImage(videoFrameImage, 200);
//
//                    ImageIO.write(videoFrameImage, "jpg", stream);
//                    try (InputStream inputStream = new ByteArrayInputStream(stream.toByteArray())) {
//                        MockMultipartFile mockMultipartFile = new MockMultipartFile(
//                                "视频预览图片",
//                                "视频预览图片.jpg",
//                                "image/jpeg",
//                                inputStream
//                        );
//                        //上传预览图片
//                        SysFile previewFile = uploadFile(mockMultipartFile);
//                        sysFile.setPreviewImageFileId(previewFile.getId());
//                    }
//                } catch (Exception e) {
//                    log.error("抽帧失败", e);
//                }


            }

            baseJdbcDao.insert(sysFile);
            // 通过 SHA-1 查找并返回文件记录
            String idSql = "SELECT * FROM sys_file WHERE sha1 = ? AND deleted IS FALSE ORDER BY create_time DESC LIMIT 1";
            return baseJdbcDao.findBySql(SysFile.class, idSql, sysFile.getSha1());
//            return getByUrl(objectPath);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }


    /**
     * id获取文件详情
     */
    @Transactional(readOnly = true)
    public SysFile getById(Serializable id) {
        return baseJdbcDao.findById(SysFile.class, id);
    }

    /**
     * URL 获取文件详情
     */
    @Transactional(readOnly = true)
    public SysFile getByUrl(String url) {
        // 确保 URL 参数不为空，避免执行无效的查询
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("找不到与Url相关的视频对象");
        }

        // 查询 SQL
        String sql = "SELECT * FROM sys_file WHERE object = ? AND deleted IS FALSE";

        // 使用 JdbcTemplate 执行查询，返回 SysFile 实体
        try {
            return primaryJdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(SysFile.class), url);
        } catch (EmptyResultDataAccessException e) {
            // 如果没有找到记录，则返回 null 或根据需要抛出异常
            return null;
        }
    }

    /**
     * 从视频中获取指定帧作为缩略图
     */
    public BufferedImage getVideoFrameImage(InputStream inputStream, int frameNum) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
            grabber.start();
            for (int i = 0; i < frameNum; i++) {
                grabber.grabImage();
            }
            Frame frame = grabber.grabImage();
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.convert(frame);
            grabber.stop();
            return image;
        } catch (Exception e) {
            log.error("视频抽帧失败", e);
            throw new RuntimeException("视频抽帧失败");
        }
    }

    /**
     * 图片缩放
     */
    public BufferedImage scaleImage(BufferedImage sourceImage, int width) {
        int height = (int) (sourceImage.getHeight() * (width / (double) sourceImage.getWidth()));
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        resizedImage.getGraphics().drawImage(sourceImage, 0, 0, width, height, null);
        return resizedImage;
    }

    /**
     * 处理文件下载/预览
     * 支持传入ID或fileCode进行文件定位
     */
    public Map<String, Object> getFileInfoForDownload(String idOrCode) {
        try {
            // 根据ID或fileCode查询文件记录
            SysFile sysFile;
            // 先尝试按fileCode查询
            String sql = "SELECT * FROM sys_file WHERE file_code = ? AND deleted IS FALSE";
            sysFile = baseJdbcDao.findBySql(SysFile.class, sql, idOrCode);
            // 如果fileCode查询不到，且是纯数字，尝试按ID查询
            if (sysFile == null && idOrCode.matches("\\d+")) {
                sysFile = getById(idOrCode);
            }
            if (sysFile == null) {
                return null;
            }

            // 权限校验：确保当前用户只能访问自己机构的文件
            // 这里需要根据实际的权限系统实现，暂时注释掉
            // long currentOrgId = StpUtil.getSession().getLong("sysOrgId");
            // if (sysFile.getSysOrgId() != currentOrgId) {
            //     throw new NotPermissionException("无权访问该机构文件");
            // }

            // 从 object 字段解析相对路径
            String objectPath = sysFile.getObject();
            // 如果 object 是以 "/files/" 开头的，去掉前缀
            String relativePath = objectPath.startsWith("/files/") ? objectPath.substring("/files/".length()) : objectPath;
            Path filePath = Paths.get(UPLOAD_DIR, relativePath);

            if (!Files.exists(filePath)) {
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sysFile", sysFile);
            result.put("filePath", filePath);
            result.put("contentLength", Files.size(filePath));
            return result;
        } catch (Exception e) {
            log.error("获取文件信息失败，idOrCode={}", idOrCode, e);
            return null;
        }
    }
}
