package com.saaes.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saaes.system.client.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    /**
     * 根据ID查询未删除的文件
     */
//    @Select("SELECT * FROM sys_file WHERE id = #{id} AND deleted = 0")
//    SysFile selectValidFileById(Integer id);

    /**
     * 根据fileCode查询未删除的文件
     */
    @Select("SELECT * FROM sys_file WHERE file_code = #{fileCode} AND deleted = 0")
    SysFile selectValidFileByFileCode(String fileCode);
}
