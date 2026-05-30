package com.system.configuration;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;

import com.system.Constant;
import com.system.dto.SysLoginUserInfoDTO;
import com.system.dto.SysUserDTO;
import com.system.entity.SysLog;
import com.system.utils.CommonUtil;
import com.system.utils.LoginUtil;
import com.system.web.MyContext;
import com.system.web.MyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

/**
 * 拦截器，实现SaToken鉴权
 *
 */
@Component
@Slf4j
public class MyInterceptor extends SaInterceptor {

    @Resource
    private SaTokenConfig saTokenConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler)
            throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            Method method = handlerMethod.getMethod();
            Class<?> controllerClass = handlerMethod.getBeanType();
            // 如果有 SaIgnore 注解，直接放行
            if (method.isAnnotationPresent(cn.dev33.satoken.annotation.SaIgnore.class) ||
                    controllerClass.isAnnotationPresent(cn.dev33.satoken.annotation.SaIgnore.class)) {
                return true;
            }
        }

        // String requestURI = request.getRequestURI();
        // 自动程序跨服务，无法获取登录用户，直接登录为自动程序用户
        String feignValue = request.getHeader(Constant.AUTO_FEIGN_KEY);
        if (CommonUtil.isNotEmpty(feignValue)) {
            log.info("自动程序跨服务：{}", feignValue);
            StpUtil.login(feignValue, "auto-job");
        }

        // 对于无法添加header但需要鉴权的请求可以将token放在参数中，手动从请求中获取token设置
        String tokenValue = request.getParameter(StpUtil.getTokenName());
        if (CommonUtil.isNotEmpty(tokenValue)) {
            StpUtil.setTokenValue(tokenValue);
        }

        if (handler instanceof HandlerMethod handlerMethod) {
            final var requestURI = request.getRequestURI();
            
            // 排除 Knife4j / Swagger 内部路径，防止死循环或非法校验
            if (requestURI.contains("/v3/api-docs") || 
                requestURI.contains("/swagger-resources") || 
                requestURI.contains("/webjars") || 
                requestURI.contains("/doc.html") ||
                requestURI.contains("/swagger-ui")) {
                return true;
            }

            SysLog sysLog = MyContext.getSysLog();
            Class<?> controllerClass = handlerMethod.getBeanType();
            Tag tag = controllerClass.getAnnotation(Tag.class);
            Method method = handlerMethod.getMethod();
            Operation operation = method.getAnnotation(Operation.class);
            if (tag == null || CommonUtil.isEmpty(tag.name())) {
                throw new MyException(
                        "保持良好的开发规范，请补充：%s 类Tag注解name属性，描述controller用途".formatted(controllerClass.getName()));
            }
            // 兼容 summary 和 description，只要有一个就不报错
            if (operation == null || (CommonUtil.isEmpty(operation.description()) && CommonUtil.isEmpty(operation.summary()))) {
                throw new MyException("保持良好的开发规范，请补充：%s 方法Operation注解description或summary属性描述方法用途".formatted(method.getName()));
            }

            sysLog.setTag(tag.name());
            // 优先取 summary，如果没有就取 description
            String opDesc = CommonUtil.isNotEmpty(operation.summary()) ? operation.summary() : operation.description();
            sysLog.setOperation(opDesc);

            // 打印一下控制器相关日志
            log.info("{} {} {}--{}", controllerClass.getName(), method.getName(), tag.name(), opDesc);

            this.auth = ignored -> {
                // SaToken鉴权
                StpUtil.checkLogin();
                
                // 校验用户信息是否存在于 Session
                SysLoginUserInfoDTO userInfoDTO = LoginUtil.getSysUserInfo();
                if (userInfoDTO == null || userInfoDTO.getUser() == null) {
                    throw new MyException("登录会话已失效，请重新登录");
                }
                
                // 仅限 TEACHER 角色操作
                StpUtil.checkRole("TEACHER");
                
                SysUserDTO user = userInfoDTO.getUser();

                // 演示站的演示账号部分操作不允许
                if (Boolean.TRUE.equals(user.getIsDemo())) {
                    boolean hit = SaRouter.notMatch(
                                    "/api/system/user/personalCenterSave",
                                    "/api/file/operation/upload",
                                    "/api/system/user/imports",
                                    "/api/system/user/saveUserJobs",
                                    "/api/system/user/saveUserGroup",
                                    "/api/system/user/delUserGroup")
                            .notMatch(obj -> {
                                boolean isDel = requestURI.endsWith("/del");
                                boolean isSave = requestURI.endsWith("/save");
                                boolean isSwitchProp = requestURI.endsWith("/switch_prop");
                                return isDel || isSave || isSwitchProp;
                            }).isHit();
                    // 代码生成器不拦截
                    boolean hitGenCode = SaRouter.match("/api/generator/**").isHit();
                    if (!hit && !hitGenCode)
                        throw new MyException("演示账号不允许此操作");
                }

                if (Boolean.TRUE.equals(user.getAutoRenewal())) {
                    // 续签token过期时间
                    StpUtil.renewTimeout(saTokenConfig.getTimeout());
                }
            };
            return super.preHandle(request, response, handler);
        }
        return true;
    }
}
