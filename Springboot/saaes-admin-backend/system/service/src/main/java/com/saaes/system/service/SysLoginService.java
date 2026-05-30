package com.saaes.system.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.http.Header;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.saaes.common.core.Constant;
import com.saaes.common.core.dto.OnlineUserDTO;
import com.saaes.common.core.dto.SysLoginUserInfoDTO;
import com.saaes.common.core.dto.SysOrgRoleDTO;
import com.saaes.common.core.dto.SysUserDTO;
import com.saaes.common.core.entity.SysLog;
import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.service.CommonService;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.utils.LoginUtil;
import com.saaes.common.core.web.*;
import com.saaes.system.client.dto.ImageCaptchaDTO;
import com.saaes.system.client.dto.SysLoginRequestDTO;
import com.saaes.system.client.dto.SysRegisterRequestDTO;
import com.saaes.system.client.dto.SysUserJobDTO;
import com.saaes.system.client.entity.SysUser;
import com.saaes.system.client.entity.SysUserJob;
import com.saaes.system.client.vo.LoginUserInfoVO;
import jakarta.annotation.Resource;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.awt.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SysLoginService extends BaseServiceImpl {
    @Resource
    private DataSourceTransactionManager dstManager;
    @Resource
    private CommonService commonService;
    @Autowired
    private MultipartConfigElement multipartConfigElement;

    /**
     * 生成图形验证码
     */
    public ImageCaptchaDTO getImageCaptcha(String captchaKey) {
        // 定义图形验证码的长、宽、验证码字符数、干扰元素个数
        // ShearCaptcha 扭曲干扰验证码
        // CircleCaptcha 圆圈干扰验证码
        // LineCaptcha 线段干扰的验证码
        AbstractCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 30, 4, 10);
        captcha.setBackground(null);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        ImageCaptchaDTO imageCaptcha = new ImageCaptchaDTO();
        imageCaptcha.setCaptchaKey(captchaKey);
        imageCaptcha.setImageBase64(captcha.getImageBase64Data());

        // 临时返回正确验证码用于测试
        imageCaptcha.setCaptchaCode(captcha.getCode());

        valueOperations.set(Constant.CAPTCHA_KEY_PREFIX + captchaKey, captcha, 2, TimeUnit.MINUTES);
        return imageCaptcha;
    }

    /**
     * 管理端用户登录
     */
    @Transactional
    public LoginUserInfoVO login(HttpServletRequest request, SysLoginRequestDTO sysLoginRequestDTO) {

        String username = CommonUtil.getString(sysLoginRequestDTO.getUsername());
        String password = CommonUtil.getString(sysLoginRequestDTO.getPassword());
        String captchaKey = CommonUtil.getString(sysLoginRequestDTO.getCaptchaKey());
        String captchaCode = CommonUtil.getString(sysLoginRequestDTO.getCaptchaCode());
        String locale = CommonUtil.getString(sysLoginRequestDTO.getLocale());
        String localeLabel = CommonUtil.getString(sysLoginRequestDTO.getLocaleLabel());
        // Boolean isDemo = sysLoginRequestDTO.getIsDemo();
        Boolean isDemo = null;

        SaSession session = null;
        try {
            session = StpUtil.getSession();
        } catch (NotLoginException e) {
            log.info("用户未登录需要重新登录~");
        }
        // 尝试登录
        if (session == null && CommonUtil.isNotEmpty(username)) {

            if (CommonUtil.isEmpty(isDemo)) {
                if (CommonUtil.isEmpty(captchaKey))
                    throw new MyException("非法登录");
                if (CommonUtil.isEmpty(captchaCode))
                    throw new MyException("请输入图形验证码");

                // 验证图形验证码
                ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
                String key = Constant.CAPTCHA_KEY_PREFIX + captchaKey;
                AbstractCaptcha captcha = (AbstractCaptcha) valueOperations.get(key);
                // 删除验证码
                redisTemplate.delete(key);
                if (captcha == null)
                    throw new MyException("验证码已失效");
                boolean verify = captcha.verify(captchaCode);
                if (!verify)
                    throw new MyException("验证码错误");
            }

            String sql = "select * from sys_user where code = ? and enabled is true";
            SysUser sysUser = baseJdbcDao.findBySql(SysUser.class, sql, username);

            if (sysUser == null)
                throw new MyException("账号不存在");
            if (CommonUtil.getString(sysUser.getStatus()).equals("2"))
                throw new MyException(sysUser.getLockMsg());
            boolean matches = BCrypt.checkpw(password, sysUser.getPassword());
            if (!matches) {
                if (Boolean.TRUE.equals(sysUser.getIsDemo())) {
                    throw new MyException("密码错误！");
                }
                try {
                    // 最大尝试次数
                    int maxTryNum = 10;
                    Integer failuresNum = sysUser.getFailuresNum();
                    if (failuresNum == null)
                        failuresNum = 0;
                    failuresNum++;
                    // 记录登录失败次数
                    sysUser.setFailuresNum(failuresNum);
                    // 失败次数大于最大尝试次数账号锁定，保存
                    if (failuresNum >= maxTryNum) {
                        sysUser.setStatus(2);
                        sysUser.setLockMsg("用户登录失败次数超过%s次，账号已锁定，请联系管理员处理。".formatted(maxTryNum));
                        throw new MyException(sysUser.getLockMsg());
                    }
                    throw new MyException("密码错误！您还可以尝试%s次。".formatted(maxTryNum - failuresNum));
                } finally {
                    // 开启新事务，保存用户登录的失败信息
                    DefaultTransactionDefinition def = new DefaultTransactionDefinition();
                    def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    // 获得事务状态
                    TransactionStatus transaction = dstManager.getTransaction(def);
                    try {
                        baseJdbcDao.update(sysUser);
                        dstManager.commit(transaction);
                    } catch (Exception e) {
                        dstManager.rollback(transaction);
                        log.error(e.getMessage());
                    }
                }
            } else {
                // 失败次数置零
                sysUser.setFailuresNum(0);
                sysUser.setLockMsg(null);
                baseJdbcDao.update(sysUser);
            }

            // 获取用户岗位角色
            List<SysOrgRoleDTO> roles = getUserRoles(sysUser.getId());
            if (roles.isEmpty()) {
                throw new MyException("该用户未分配角色，无法登录!");
            }
            
            // 如果账号不允许重复登录，则将已登录的强制下线
            if (Boolean.FALSE.equals(sysUser.getAllowRepeat())) {
                StpUtil.kickout(sysUser.getId(), "WEB");
            }

            // 登录
            StpUtil.login(sysUser.getId(), "WEB");
            session = StpUtil.getSession();

            // 刷新用户信息和权限缓存
            SysUserDTO sysUserDTO = new SysUserDTO();
            BeanUtils.copyProperties(sysUser, sysUserDTO);
            SysLoginUserInfoDTO loginUserInfoDTO = new SysLoginUserInfoDTO();
            loginUserInfoDTO.setUser(sysUserDTO);
            loginUserInfoDTO.setRoles(roles);
            session.set(LoginUtil.SYS_USER_KEY, loginUserInfoDTO);

            UserAgent ua = UserAgentUtil.parse(request.getHeader(Header.USER_AGENT.toString()));
            SysLog sysLog = MyContext.getSysLog();
            OnlineUserDTO onlineUserDTO = new OnlineUserDTO();
            onlineUserDTO.setToken(StpUtil.getTokenValue());
            onlineUserDTO.setUserId(sysUser.getId());
            onlineUserDTO.setUserCode(sysUser.getCode());
            onlineUserDTO.setUserName(sysUser.getName());
            onlineUserDTO.setLoginBrowser(ua.getBrowser().getName());
            onlineUserDTO.setBrowserVersion(ua.getVersion());
            onlineUserDTO.setLoginIp(sysLog.getIp());
            onlineUserDTO.setLoginBrowser(ua.getBrowser().getName());
            onlineUserDTO.setLoginOs(ua.getOs().getName());
            onlineUserDTO.setIsMobile(ua.isMobile());
            onlineUserDTO.setLoginAddress(sysLog.getIpAddress());
            onlineUserDTO.setLocale(locale);
            onlineUserDTO.setLocaleLabel(localeLabel);

            SysOrgRoleDTO orgRole = roles.getFirst(); // 默认当前使用角色为第一个角色
            onlineUserDTO.setOrgId(orgRole.getSysOrgId());
            onlineUserDTO.setRoleId(orgRole.getSysRoleId());
            onlineUserDTO.setOrgName(orgRole.getOrgName());
            onlineUserDTO.setRoleName(orgRole.getRoleName());
            onlineUserDTO.setLoginTime(LocalDateTime.now());
            StpUtil.getTokenSession().set(LoginUtil.SYS_USER_KEY, onlineUserDTO);
            return getCurrentLoginUserVO(true);
        } else {
            return getCurrentLoginUserVO(false);
        }
    }

    public void register(SysRegisterRequestDTO sysRegisterRequestDTO) {

        if (sysRegisterRequestDTO == null || sysRegisterRequestDTO.getUsername().isEmpty()
                || sysRegisterRequestDTO.getPlatform().isEmpty()) {
            throw new MyException("注册信息不能为空");
        }

        if (!Objects.equals(sysRegisterRequestDTO.getPassword(), sysRegisterRequestDTO.getRePassword())) {
            throw new MyException("两次密码不一致");
        }

        if (sysRegisterRequestDTO.getUsername().length() < 4 || sysRegisterRequestDTO.getUsername().length() > 20) {
            throw new MyException("用户昵称长度不符合");
        }
        // if (sysUser.getTelephone() == null || !Pattern.matches("^1[3-9]\\d{9}$",
        // sysUser.getTelephone())) {throw new MyException("用户手机号码格式不符合");}
        // if (sysUser.getEmail() == null || !Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$",
        // sysUser.getEmail())) {throw new MyException("用户邮箱地址格式不符合");}

        String selectSql = "SELECT COUNT(*) FROM sys_user WHERE code = :code";
        Map<String, Object> params = new HashMap<>();
        params.put("code", sysRegisterRequestDTO.getUsername());

        if (sysRegisterRequestDTO.getOpenid() != null && !sysRegisterRequestDTO.getOpenid().isEmpty()) {
            selectSql += " OR openid = :openid";
            params.put("openid", sysRegisterRequestDTO.getOpenid());
        }

        Integer count = primaryNPJdbcTemplate.queryForObject(selectSql, params, Integer.class);
        count = count != null ? count : 0;
        if (count > 0) {
            throw new MyException("用户账号%s已存在！".formatted(sysRegisterRequestDTO.getUsername()));
        }

        String platform = sysRegisterRequestDTO.getPlatform();
        String openId = sysRegisterRequestDTO.getOpenid();

        SysUser sysUser = new SysUser();
        sysUser.setCode(sysRegisterRequestDTO.getUsername());
        sysUser.setName(CommonUtil.generateNickname(null));
        sysUser.setPlatform(platform);
        sysUser.setTelephone(sysRegisterRequestDTO.getTelephone());
        sysUser.setEnabled(true);
        sysUser.setStatus(1);

        if ("App".equals(platform) && openId != null && !openId.isEmpty()) {
            sysUser.setAvatar(sysRegisterRequestDTO.getAvatar());
            baseJdbcDao.insert(sysUser);
        } else if ("Web".equals(platform)) {
            sysUser.setPassword(BCrypt.hashpw(sysRegisterRequestDTO.getPassword(), BCrypt.gensalt()));
            baseJdbcDao.insert(sysUser);
        } else {
            throw new MyException("不支持的注册来源");
        }

        String selectUserSql = "SELECT * FROM sys_user WHERE code = :code";
        SysUser sysUserNew = primaryNPJdbcTemplate.queryForObject(
                selectUserSql,
                params,
                new BeanPropertyRowMapper<>(SysUser.class));
        if (sysUserNew == null) {
            throw new MyException("注册失败");
        }
        SysUserJob sysUserJob = new SysUserJob();
        sysUserJob.setUserId(sysUserNew.getId());
        sysUserJob.setType(1);
        sysUserJob.setSysOrgId(1);

        // 修复：动态查询 student 角色的 ID
        String roleSql = "SELECT id FROM sys_role WHERE name = :roleName";
        Map<String, Object> roleParams = new HashMap<>();
        roleParams.put("roleName", "student");
        try {
            Integer studentRoleId = primaryNPJdbcTemplate.queryForObject(roleSql, roleParams, Integer.class);
            sysUserJob.setSysRoleId(studentRoleId);
        } catch (Exception e) {
            // 如果查不到 student 角色，为了防止报错，可以给一个默认值或者抛出异常提醒管理员
            throw new MyException("系统配置错误：未找到 'student' 角色，请联系管理员初始化数据库。");
        }

        sysUserJob.setEnabled(true);
        baseJdbcDao.insert(sysUserJob);
    }

    /**
     * 管理端用户角色切换
     */
    public LoginUserInfoVO switchUserRole(Map<String, Object> params) {
        String orgId = CommonUtil.getString(params.get("sysOrgId"));
        String roleId = CommonUtil.getString(params.get("sysRoleId"));
        SysLoginUserInfoDTO loginUserInfoDTO = LoginUtil.getSysUserInfo();
        List<SysOrgRoleDTO> roles = loginUserInfoDTO.getRoles();
        for (SysOrgRoleDTO orgRole : roles) {
            // 从当前登录用户session中寻找匹配的角色，并设置当前角色，机构，及名称
            if (Objects.equals(orgRole.getSysOrgId().toString(), orgId)
                    && Objects.equals(orgRole.getSysRoleId().toString(), roleId)) {
                SaSession tokenSession = StpUtil.getTokenSession();
                OnlineUserDTO onlineUserDTO = tokenSession.getModel(LoginUtil.SYS_USER_KEY, OnlineUserDTO.class);
                onlineUserDTO.setOrgId(orgRole.getSysOrgId());
                onlineUserDTO.setOrgName(orgRole.getOrgName());
                onlineUserDTO.setRoleId(orgRole.getSysRoleId());
                onlineUserDTO.setRoleName(orgRole.getRoleName());
                tokenSession.set(LoginUtil.SYS_USER_KEY, onlineUserDTO);
                return getCurrentLoginUserVO(true);
            }
        }
        throw new MyException("角色切换异常，请重新登录后操作！");
    }

    /**
     * 语言切换
     */
    public void switchLocale(Map<String, Object> params) {
        String locale = CommonUtil.getString(params.get("locale"));
        String localeLabel = CommonUtil.getString(params.get("localeLabel"));
        SaSession tokenSession = StpUtil.getTokenSession();
        OnlineUserDTO onlineUserDTO = tokenSession.getModel(LoginUtil.SYS_USER_KEY, OnlineUserDTO.class);
        onlineUserDTO.setLocale(locale);
        onlineUserDTO.setLocaleLabel(localeLabel);
        tokenSession.set(LoginUtil.SYS_USER_KEY, onlineUserDTO);
    }

    /**
     * 管理端用户注销
     */
    public RestResponse<?> logout() {
        StpUtil.logout();
        return RestResponse.success();
    }

    /**
     * 在线用户查询
     */
    public PageResult<OnlineUserDTO> queryOnlineUser(PageQuery<Map<String, Object>> pageQuery) {
        final Map<String, Object> param = pageQuery.getParam();
        // 查询所有已登录的 Token
        List<String> tokens = StpUtil.searchTokenValue("", 0, -1, false);
        List<OnlineUserDTO> onlineUserList = tokens.stream()
                // 截取实际的token值，去掉前缀
                .map(i -> i.split(":")[3])
                // 过滤掉未登录的token
                .filter(i -> StpUtil.getLoginIdByToken(i) != null)
                .map(StpUtil::getTokenSessionByToken)
                .map(i -> i.getModel(LoginUtil.SYS_USER_KEY, OnlineUserDTO.class))
                .filter(Objects::nonNull)
                // 模糊查询
                .filter(i -> {
                    boolean r = true;
                    if (CommonUtil.isNotEmpty(param.get("userCode"))) {
                        r = i.getUserCode().contains(param.get("userCode").toString());
                    }
                    if (r && CommonUtil.isNotEmpty(param.get("userName"))) {
                        r = i.getUserName().contains(param.get("userName").toString());
                    }
                    if (r && CommonUtil.isNotEmpty(param.get("ip"))) {
                        r = i.getLoginIp().contains(param.get("ip").toString());
                    }
                    return r;
                })
                // 排序
                .sorted((a, b) -> {
                    if (pageQuery.getOrderProp() == null || pageQuery.getOrderDirection() == null) {
                        pageQuery.setOrderProp("loginTime");
                        pageQuery.setOrderDirection(PageQuery.OrderDirection.desc);
                    }
                    try {
                        Field field = CommonUtil.getField(OnlineUserDTO.class, pageQuery.getOrderProp());
                        if (field == null)
                            return 0;
                        field.setAccessible(true);
                        Object aVal = field.get(a);
                        Object bVal = field.get(b);
                        if (aVal != null && bVal != null) {
                            var px = aVal.toString().compareTo(bVal.toString());
                            if (pageQuery.getOrderDirection() == PageQuery.OrderDirection.desc) {
                                return -px;
                            }
                            return px;
                        }
                    } catch (ReflectiveOperationException e) {
                        log.error("比较错误", e);
                        throw new MyException(e.getMessage());
                    }
                    return 0;
                })
                .toList();
        PageResult<OnlineUserDTO> pageResult = new PageResult<>();
        pageResult.setIsPage(pageQuery.getIsPage());
        pageResult.setCurrentPage(pageQuery.getCurrentPage());
        pageResult.setPageSize(pageQuery.getPageSize());
        pageResult.setTotal(onlineUserList.size());
        if (pageQuery.getIsPage()) {
            onlineUserList = onlineUserList.stream()
                    .skip((long) (pageQuery.getCurrentPage() - 1) * pageQuery.getPageSize())
                    .limit(pageQuery.getPageSize())
                    .toList();
        }
        pageResult.setList(onlineUserList);
        return pageResult;
    }

    /**
     * 踢用户下线
     */
    public void kickOut(String token) {
        StpUtil.kickoutByTokenValue(token);
    }

    /**
     * 获取用户岗位角色
     */
    public List<SysOrgRoleDTO> getUserRoles(Object userId) {
        // 用户拥有的机构角色岗位，包括所在用户组的机构角色岗位
        String sql = """
                    select
                        tem.*,
                        o.code org_code,
                        o.name org_name,
                        r.name role_name
                    from (
                        SELECT
                            sys_org_id, sys_role_id
                        FROM
                            sys_user_job
                        WHERE
                           type = 1 AND enabled is true AND user_id = ?
                        UNION
                        SELECT
                            c.sys_org_id, c.sys_role_id
                        FROM
                            sys_user_group_member a
                            LEFT JOIN sys_user_group b ON b.id = a.sys_user_group_id
                            LEFT JOIN sys_user_job c ON b.id = c.user_id AND c.type = 2 AND c.enabled is true
                        WHERE
                            a.sys_user_id = ?
                    ) tem
                    left join sys_org o on o.id = tem.sys_org_id
                    left join sys_role r on r.id = tem.sys_role_id
                """;
        return baseJdbcDao.findList(SysOrgRoleDTO.class, sql, userId, userId);
    }

    /**
     * 获取当前token的用户角色信息
     */
    private LoginUserInfoVO getCurrentLoginUserVO(Boolean refresh) {
        try {
            SaSession session = StpUtil.getSession();
            SaSession tokenSession = StpUtil.getTokenSession();
            LoginUserInfoVO loginUserInfo = null;
            if (session != null && tokenSession != null) {
                SysLoginUserInfoDTO loginUserInfoDTO = session.getModel(LoginUtil.SYS_USER_KEY,
                        SysLoginUserInfoDTO.class);
                loginUserInfo = new LoginUserInfoVO();
                loginUserInfo.setTokenName(StpUtil.getTokenName());
                loginUserInfo.setTokenValue(StpUtil.getTokenValue());
                loginUserInfo.setUser(loginUserInfoDTO.getUser());
                OnlineUserDTO onlineUser = tokenSession.getModel(LoginUtil.SYS_USER_KEY, OnlineUserDTO.class);
                List<SysOrgRoleDTO> roles = loginUserInfoDTO.getRoles();
                for (SysOrgRoleDTO role : roles) {
                    role.setActive(Objects.equals(onlineUser.getRoleId(), role.getSysRoleId())
                            && Objects.equals(onlineUser.getOrgId(), role.getSysOrgId()));
                }
                loginUserInfo.setRoles(roles);
                loginUserInfo.setMenus(commonService.getRolePermissions(onlineUser.getRoleId(), refresh));
            }
            return loginUserInfo;
        } catch (NotLoginException e) {
            return null;
        }
    }
}
