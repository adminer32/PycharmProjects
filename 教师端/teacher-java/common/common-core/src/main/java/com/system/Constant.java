package com.system;

/**
 * 常量
 *
 */
public class Constant {
    public static final String AUTO_FEIGN_KEY = "xxxxxxffffgfsdgfsdafsgguyyerg";

    /**
     * 验证码 redis 前缀
     */
    public static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 角色权限集合 redis 前缀
     */
    public static final String ROLE_PERMISSIONS_PREFIX = "role:permissions:";


    // DashScope API 配置
    public static final String DASH_SCOPE_API_KEY = "sk-5b195948827c4f19a67da690483bff82";
//    public static final String DASH_SCOPE_MODEL = "qwen-max";  //千问Max稳定版（qwen-max-2024-09-19）
    public static final String DASH_SCOPE_MODEL = "qwen-max-latest"; //千问MaxLatest（始终等同最新快照版）

    public static final String DASH_SCOPE_MODEL_QWEN_TURBO = "qwen-turbo";

    public static final String DASH_SCOPE_MODEL_QWEN_Plus = "qwen-plus-latest";


    // 其他 API 配置
    // 外网
//    public static final String VIDEO_POST_URL = "https://lxzj_python.tenir.cn/process_video";
//    public static final String VIDEO_POST_URL = "http://117.50.191.51:5000/process_video";
    // 内网
//    public static final String VIDEO_POST_URL = "http://10.100.147.76:5000/process_video";
    public static final String VIDEO_POST_URL = "http://127.0.0.1:5000/process_video";


    // 外网
//    public static final String BASE_URL = "https://lxzj.tenir.cn/lxzj";

    // 内网
//    public static final String BASE_URL = "http://10.200.70.15:9002";

//    public static final String BASE_URL = "http://10.96.81.199:9002";

    // 本地
    public static final String BASE_URL = "http://127.0.0.1:9002";

}
