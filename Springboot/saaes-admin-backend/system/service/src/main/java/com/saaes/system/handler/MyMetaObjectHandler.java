package com.saaes.system.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        try {
            int loginId = StpUtil.getLoginIdAsInt();

            strictInsertFill(metaObject, "createBy", Integer.class, loginId);
            strictInsertFill(metaObject, "updateBy", Integer.class, loginId);
        } catch (Exception ignored) {}

        strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "sendTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        try {
            int loginId = StpUtil.getLoginIdAsInt();
            strictUpdateFill(metaObject, "updateBy", Integer.class, loginId);
        } catch (Exception ignored) {}

        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}


