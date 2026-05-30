package com.saaes.common.core.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通用分页查询参数")
public class BasePageQuery{

    @Schema(description = "当前页码", defaultValue = "1")
    private Long page = 1L;

    @Schema(description = "每页条数", defaultValue = "10")
    private Long pageSize = 10L;

    public <T> Page<T> toPage() {
        long current = Math.max(1L, page);
        long size = Math.max(1L, pageSize);
        return new Page<>(current,size);
    }

}
