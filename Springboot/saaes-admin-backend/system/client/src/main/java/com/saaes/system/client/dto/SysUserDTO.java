package com.saaes.system.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.saaes.common.core.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;

import static com.saaes.common.core.Constant.BASE_URL;


@Schema(title = "系统用户")
@Table
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SysUserDTO extends BaseEntity<Integer> {

    @Schema(title = "用户代码")
    private String code;

    @Schema(title = "用户名")
    private String name;

    @Schema(title = "密码")
    private String password;

    @Schema(title = "头像")
    private String avatar;

    @Schema(title = "手机号码")
    private String telephone;

    @Schema(title = "注册平台，Web or App")
    private String platform;

    @Schema(title = "uniApp openId")
    private String openid;

    @Schema(title = "邮箱")
    private String email;

    @Schema(title = "状态（1：正常，2：锁定）")
    private Integer status;

    @Schema(title = "登录失败的次数")
    private Integer failuresNum;

    @Schema(title = "账号锁定的原因")
    private String lockMsg;

    @Schema(title = "允许重复登录")
    private Boolean allowRepeat;

    @Schema(title = "自动续签，请求会自动延长token失效时间")
    private Boolean autoRenewal;

    @Schema(title = "是否演示账号")
    private Boolean isDemo;

    @Schema(title = "是否启用")
    private Boolean enabled;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getAvatar() {
        if (avatar == null || avatar.isEmpty()) {
            return BASE_URL + "/files/2025/03/default-avatar.png"; // 默认头像 URL
        }
        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            return avatar;
        }
        return BASE_URL + avatar;
    }
}
