package com.system.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果
 */
@Setter
@Getter
@NoArgsConstructor
@Schema(title = "分页结果")
public class PageResult<T> implements Serializable {
    @Schema(title = "列表数据")
    private List<T> list = new ArrayList<>();

    @Schema(title = "总数")
    private Integer total = 0;

    @Schema(title = "是否分页")
    private Boolean isPage;

    @Schema(title = "分页号")
    private Integer currentPage;

    @Schema(title = "分页大小")
    private Integer pageSize;

    public PageResult(List<T> list, Integer total) {
        this.list = list;
        this.total = total;
    }

    public PageResult(List<T> list, Integer total, Integer currentPage, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.isPage = true;
    }

    public PageResult(IPage<T> page) {
        if (page != null) {
            this.list = page.getRecords();
            this.total = (int) page.getTotal();
            this.currentPage = (int) page.getCurrent();
            this.pageSize = (int) page.getSize();
            this.isPage = true;
        }
    }
}
