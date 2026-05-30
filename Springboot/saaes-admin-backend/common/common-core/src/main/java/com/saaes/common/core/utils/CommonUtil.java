package com.saaes.common.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 通用工具类
 */
@Slf4j
public class CommonUtil {
    /**
     * 获取字符串，null返回空串
     */
    public static String getString(Object object) {
        return object == null ? "" : object.toString();
    }

    /**
     * 判断不为null且不为为空串
     */
    public static boolean isNotEmpty(Object object) {
        return !isEmpty(object);
    }

    /**
     * 判断为null或者为空串
     */
    public static boolean isEmpty(Object object) {
        return object == null || object.toString().isEmpty();
    }

    /**
     * 驼峰转小写下划线
     */
    public static String toLowerUnderscore(String str) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
    }

    /**
     * 驼峰转大写下划线
     */
    public static String toUpperUnderscore(String str) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, str);
    }

    /**
     * 下划线转小写驼峰
     */
    public static String toLowerCamel(String str) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, str);
    }

    /**
     * 下划线转大写驼峰
     */
    public static String toUpperCamel(String str) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, str);
    }

    /**
     * 获取所有的field包括父级继承的，子类会覆盖父类的field
     */
    public static Collection<Field> getAllFields(Class<?> clazz) {
        if (clazz != null) {
            LinkedList<Class<?>> list = new LinkedList<>();
            Class<?> currentClass = clazz;
            while (currentClass != null) {
                list.addFirst(currentClass);
                currentClass = currentClass.getSuperclass();
            }
            Map<String, Field> fieldMap = new LinkedHashMap<>();
            for (Class<?> aClass : list) {
                Field[] declaredFields = aClass.getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    fieldMap.put(declaredField.getName(), declaredField);
                }
            }
            return fieldMap.values();
        }
        return null;
    }

    /**
     * 获取field包括父级继承的，子类会覆盖父类的field
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (clazz != null) {
            Class<?> currentClass = clazz;
            while (currentClass != null) {
                try {
                    return currentClass.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    currentClass = currentClass.getSuperclass();
                }
            }
        }
        return null;
    }

    /**
     * 获取文件的后缀名
     */
    public static String getFileSuffix(String filename) {
        if (filename == null) return null;
        int index = filename.lastIndexOf(".");
        if (index == -1) return null;
        return filename.substring(index + 1);
    }

    /**
     * 获取文件摘要sha1
     */
    public static String getFileSha1(InputStream inputStream) {
        try (inputStream) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[1024 * 1024 * 10];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
            StringBuilder sha1 = new StringBuilder(new BigInteger(1, digest.digest()).toString(16));
            int length = 40 - sha1.length();
            if (length > 0) {
                for (int i = 0; i < length; i++) {
                    sha1.insert(0, "0");
                }
            }
            return sha1.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回异常的堆栈文本信息
     */
    public static String getThrowString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter printWriter = new PrintWriter(sw);
        throwable.printStackTrace(printWriter);
        return sw.toString();
    }

    /**
     * 获取视频时长
     * @param videoFile
     * @return
     */
    public static String getVideoDuration(File videoFile) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();  // 启动 FFmpeg 抓取器

            long durationInMicroseconds = grabber.getLengthInTime();  // 获取时长（单位：微秒）
            long durationInSeconds = durationInMicroseconds / 1_000_000; // 转换为秒

            long hours = durationInSeconds / 3600;
            long minutes = (durationInSeconds % 3600) / 60;
            long seconds = durationInSeconds % 60;

            grabber.stop();  // 停止 FFmpeg 抓取器

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
            return "00:00:00"; // 如果获取失败，返回默认值
        }
    }

    /**
     * 提取千问大模型返回的content
     * @param jsonString
     * @return
     * @throws IOException
     */
    public static String extractQwenContent(String jsonString) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        JsonNode contentNode = jsonNode.path("output")
                .path("choices")
                .get(0)
                .path("message")
                .path("content");
        return contentNode.asText();
    }


    /**
     * 生成随机用户昵称
     * @param roleName
     * @return
     */
    public static String generateNickname(String roleName) {
        String prefix = (roleName == null || roleName.trim().isEmpty()) ? "用户" : roleName.trim();
        String randomPart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 5);
        return prefix + "_" + randomPart;
    }



}
