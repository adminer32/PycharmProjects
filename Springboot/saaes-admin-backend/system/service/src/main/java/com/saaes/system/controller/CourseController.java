package com.saaes.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageQuery;
import com.saaes.common.core.web.PageResult;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.entity.*;
import com.saaes.system.service.CourseService;
import com.saaes.system.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "课程中心")
@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Resource
    private CourseService courseService;

    @Resource
    private SysFileService sysFileService;

    @Operation(description = "课程分类查询")
    @PostMapping("/queryCategory")
    @SaIgnore
    public RestResponse<PageResult<CourseCategory>> getCourseCateGoryList(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        PageResult<CourseCategory> pageResult = courseService.getCourseCateGoryList(pageQuery);
        return RestResponse.success("查询成功", pageResult);
    }

    @Operation(description = "课程列表查询")
    @PostMapping("/query")
// @SaCheckPermission("curriculum:course")
    @SaIgnore
    public RestResponse<PageResult<SubCategory>> query(@RequestBody PageQuery<Map<String, Object>> pageQuery) {
        // 调用 courseService.query(pageQuery)，返回一个 PageResult<ParentCategory>
        PageResult<SubCategory> result = courseService.query(pageQuery);

        // 使用 RestResponse 返回分页结果
        return RestResponse.success("查询成功", result);
    }

    @SaIgnore
    @Operation(description = "随机返回5条课程信息")
    @GetMapping("/randomCourseInfo")
    public RestResponse<List<Map<String, Object>>> getRandomCourses() {
        return RestResponse.success("查询成功", courseService.queryRandomCoursesWithCategory());
    }


    //    @SaCheckPermission(value = {"curriculum:course:add", "curriculum:course:edit"}, mode = SaMode.OR)
    @Operation(description = "课程保存")
    @PostMapping("/save")
    public RestResponse<CourseInfo> save(@RequestParam(value = "file", required = false) MultipartFile file ,
                                         @RequestParam("courseInfo") String courseInfoJson,
                                         @RequestParam String url) throws JsonProcessingException {

        CourseInfo courseInfo = new ObjectMapper().readValue(courseInfoJson, CourseInfo.class);

        if (courseInfo == null) {throw new MyException("课程信息不能为空");}
        if (courseInfo.getContent() == null || courseInfo.getContent().isEmpty()) {throw new MyException("课程内容不能为空");}
        if (courseInfo.getDescription() == null || courseInfo.getDescription().isEmpty()) {throw new MyException("课程描述不能为空");}
        if (courseInfo.getCategoryId() == null) {throw new MyException("课程分类不能为空");}

        // 如果文件为空或未上传
        if (file == null && file.isEmpty()) {
            return RestResponse.success(courseService.save(courseInfo, null, null));
        }

        SysFile sysFile = sysFileService.uploadFile(file);
        return RestResponse.success(courseService.save(courseInfo, sysFile, url));
    }

    @SaCheckPermission("curriculum:course:del")
    @Operation(description = "课程批量删除")
    @DeleteMapping("/del")
    public RestResponse<?> del(@RequestParam List<Integer> ids) {
        return RestResponse.success(courseService.del(ids));
    }

    /**
     * ai笔记查询
     */
    @SaIgnore
    @Operation(description = "Ai课程笔记查询")
    @GetMapping("/getNoteById")
    public RestResponse<AiNoteInfo> queryCourseAiNoteById(@RequestParam Integer id) {
        return RestResponse.success("笔记查询成功",courseService.queryNote(id));
    }


}
