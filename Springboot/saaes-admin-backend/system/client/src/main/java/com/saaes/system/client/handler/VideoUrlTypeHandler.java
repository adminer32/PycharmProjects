package com.saaes.system.client.handler;

import com.saaes.common.core.Constant;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VideoUrlTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String videoUrl = rs.getString(columnName);
        // 仅对 video_url 字段拼接 URL
        if ("video_url".equals(columnName) && videoUrl != null && !videoUrl.startsWith("http") && !videoUrl.startsWith("https")) {
            return Constant.BASE_URL + videoUrl;
        }
        return videoUrl;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String videoUrl = rs.getString(columnIndex);
        // 仅对 video_url 字段拼接 URL
        if (videoUrl != null && !videoUrl.startsWith("http") && !videoUrl.startsWith("https")) {
            return Constant.BASE_URL + videoUrl;
        }
        return videoUrl;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String videoUrl = cs.getString(columnIndex);
        // 仅对 video_url 字段拼接 URL
        if (videoUrl != null && !videoUrl.startsWith("http") && !videoUrl.startsWith("https")) {
            return Constant.BASE_URL + videoUrl;
        }
        return videoUrl;
    }
}
