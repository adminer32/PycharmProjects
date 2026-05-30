package com.saaes.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.aliyuncs.exceptions.ClientException;
import com.saaes.common.core.Constant;
import com.saaes.common.core.dao.BaseJdbcDaoImpl;
import com.saaes.common.core.service.BaseService;
import com.saaes.common.core.service.BaseServiceImpl;
import com.saaes.common.core.utils.CommonUtil;
import com.saaes.common.core.utils.QwenClient;
import com.saaes.common.core.utils.TranscriptionTaskUtils;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.PageQuery;
import com.saaes.common.core.web.PageResult;
import com.saaes.system.client.entity.AiNoteInfo;
import com.saaes.system.client.entity.CourseCategory;
import com.saaes.system.client.entity.CourseInfo;
import com.saaes.system.client.entity.SysFile;
import com.saaes.system.handler.TranscriptionTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AiService extends BaseServiceImpl {

    private final TranscriptionTaskHandler transcriptionTaskHandler;

    @Autowired
    private SysFileService sysFileService;

    public AiService(TranscriptionTaskHandler transcriptionTaskHandler, BaseJdbcDaoImpl baseJdbcDao) {
        this.transcriptionTaskHandler = transcriptionTaskHandler;
    }

    /**
     * 添加视频笔记转写任务
     * @param params
     * @return
     */
    public Map<String, Object> add(Map<String, Object> params) {
        if(params == null || params.isEmpty() || CommonUtil.isEmpty(params.get("url")) || CommonUtil.isEmpty(params.get("video_id")) || CommonUtil.isEmpty(params.get("course_id"))) {
            throw new MyException("参数不能为空");
        }

        try {

            // 输入音视频文件的URL（音视频文件必须是可以公开访问的URL）
            String fileUrl = params.get("url").toString();
            String videoId = params.get("video_id").toString();
            String courseId = params.get("course_id").toString();


            // 调用构建参数的方法，生成需要的请求参数
            JSONObject parameters = TranscriptionTaskUtils.buildRequestParameters();

            // 调用工具类提交离线转写任务，返回任务ID
            String id = TranscriptionTaskUtils.submitOfflineTranscriptionTask(fileUrl, parameters);

            if (id == null || id.isEmpty()) {throw new MyException("任务创建失败，请重试");}

            String idSql = "SELECT * FROM ai_note_info WHERE course_id = ? AND deleted IS FALSE";
            AiNoteInfo aiNoteInfoResult =  baseJdbcDao.findBySql(AiNoteInfo.class, idSql, courseId);

            if (aiNoteInfoResult != null) {
                aiNoteInfoResult.setTaskId(id);
                aiNoteInfoResult.setVideoUrl(fileUrl);
                aiNoteInfoResult.setCourseId(Integer.valueOf(courseId));
                aiNoteInfoResult.setTaskStatus("进行中");
                aiNoteInfoResult.setAutoChapters(null);
                aiNoteInfoResult.setSummarization(null);
                aiNoteInfoResult.setMeetingAssistance(null);
                baseJdbcDao.update(aiNoteInfoResult);
            } else {
                AiNoteInfo aiNoteInfo = new AiNoteInfo();
                aiNoteInfo.setCourseId(Integer.valueOf(courseId));
                aiNoteInfo.setTaskStatus("进行中");
                aiNoteInfo.setTaskId(id);
                aiNoteInfo.setVideoId(Integer.valueOf(videoId));
                aiNoteInfo.setVideoUrl(fileUrl);
                baseJdbcDao.insert(aiNoteInfo);
            }

            // 调用handler处理任务
            transcriptionTaskHandler.addTaskToQueue(id);
            return Map.of("course_task_id", id);
        } catch (ClientException e) {
            throw new MyException(e.getMessage());
        }
    }

    /**
     * 添加个人视频笔记转写任务
     * @param params
     * @return
     */
    public AiNoteInfo personalAdd(Map<String, Object> params) {

        if (params == null || params.isEmpty()) {throw new MyException("参数不能为空");}
        if (params.get("title") == null) {throw new MyException("标题不能为空");}
        if (params.get("video_url") == null) {throw new MyException("视频Url不能为空");}

        try {

            // 输入音视频文件的URL（音视频文件必须是可以公开访问的URL）
            String videoUrl = params.get("video_url").toString();
            String title = params.get("title").toString();
            SysFile sysFile = null;
            if (videoUrl != null && !(videoUrl.contains("http://") || videoUrl.contains("https://"))) {
                sysFile = sysFileService.getByUrl(videoUrl);
                if (sysFile == null) {throw new MyException("未找到关联的本地视频文件");}
                videoUrl = Constant.BASE_URL + videoUrl;
            }

            // 调用构建参数的方法，生成需要的请求参数
            JSONObject parameters = TranscriptionTaskUtils.buildRequestParameters();

            System.out.println(videoUrl);

            // 调用工具类提交离线转写任务，返回任务ID
            String id = TranscriptionTaskUtils.submitOfflineTranscriptionTask(videoUrl, parameters);

            if (id == null || id.isEmpty()) {throw new MyException("任务创建失败，请重试");}

                AiNoteInfo aiNoteInfo = new AiNoteInfo();
                aiNoteInfo.setTitle(params.get("title").toString());
                aiNoteInfo.setTaskStatus("进行中");
                aiNoteInfo.setTaskId(id);
                aiNoteInfo.setVideoId(sysFile == null ? null : sysFile.getId());
                aiNoteInfo.setVideoUrl(sysFile == null ? videoUrl : sysFile.getObject());
                baseJdbcDao.insert(aiNoteInfo);

            // 调用handler处理任务
            transcriptionTaskHandler.addTaskToQueue(id);

            Long createBy = StpUtil.getLoginIdAsLong();
            String idSql = "SELECT * FROM ai_note_info " +
                    "WHERE task_id = ? AND create_by = ? AND deleted IS FALSE AND (course_id IS NULL OR course_id = '')";
            AiNoteInfo aiNoteInfoResult = baseJdbcDao.findBySql(
                    AiNoteInfo.class,
                    idSql,
                    id,
                    createBy
            );

            return aiNoteInfoResult;
        } catch (ClientException e) {
            throw new MyException(e.getMessage());
        }
    }

    /**
     * 个人笔记内容更新
     * @param params
     * @return
     */
    public AiNoteInfo personalNoteUpdate(Map<String, Object> params) {
        if (params == null) {throw new MyException("参数不能为空");}
        if (params.get("id") == null) {throw new MyException("笔记id不能为空");}

        Long createBy = StpUtil.getLoginIdAsLong();
        String idSql = "SELECT * FROM ai_note_info " +
                "WHERE id = ? AND create_by = ? AND deleted IS FALSE AND (course_id IS NULL OR course_id = '')";
        AiNoteInfo aiNoteInfoResult = baseJdbcDao.findBySql(
                AiNoteInfo.class,
                idSql,
                params.get("id").toString(),
                createBy
        );

        if (aiNoteInfoResult == null) {throw new MyException("该笔记信息不存在");}

        if (CommonUtil.isNotEmpty(params.get("title"))) {
            aiNoteInfoResult.setTitle(params.get("title").toString());
        }


        if (CommonUtil.isNotEmpty(params.get("personal_note"))) {
            aiNoteInfoResult.setPersonalNote(params.get("personal_note").toString());
        }

        baseJdbcDao.update(aiNoteInfoResult);

        return aiNoteInfoResult;
    }

    /**
     * 任务删除
     * @param ids
     * @return
     */
    @Transactional
    public Map del(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of("success", 0, "error",0);
        }
        log.info("批量删除课程--");
        String sql = "update ai_note_info set deleted = 1 where id in (:ids)";
        Map<String, Object> paramMap = Collections.singletonMap("ids", ids);
        int result = primaryNPJdbcTemplate.update(sql, paramMap);
        return Map.of("success", result, "error", ids.size() - result);
    }

    /**
     * 查询任务结果
     * @param TaskId
     * @return
     * @throws ClientException
     */
    public String query(String TaskId) throws ClientException {
        String result = TranscriptionTaskUtils.getTaskResult(TaskId);

        if (result == null || result.isEmpty()) {throw new MyException("查询任务结果失败");}

        return result;
    }

    /**
     * 根据任务ID查询数据
     * @param TaskId
     * @return
     * @throws ClientException
     */
    public AiNoteInfo queryByTaskId(String TaskId) throws ClientException {
        if (TaskId == null || TaskId.isEmpty()) {throw new MyException("任务ID不能为空");}
        Long createBy = StpUtil.getLoginIdAsLong();
        String idSql = "SELECT * FROM ai_note_info " +
                "WHERE task_id = ? AND create_by = ? AND deleted IS FALSE AND (course_id IS NULL OR course_id = '')";
        return baseJdbcDao.findBySql(
                AiNoteInfo.class,
                idSql,
                TaskId,
                createBy
        );
    }

    /**
     * 查询个人笔记
     * @param pageQuery
     * @return
     */
    public PageResult<AiNoteInfo> queryPersonalNotes(PageQuery<Map<String, Object>> pageQuery) {
        Map<String, Object> params = pageQuery.getParam();
        StringBuilder sql = new StringBuilder("""
        select a.*, f.video_duration
        from ai_note_info a
        left join sys_file f on a.video_id = f.id
        where a.deleted is false 
          and (a.course_id is null or a.course_id = '')
          and a.title is not null and a.title != ''
    """);

        if (CommonUtil.isNotEmpty(params.get("title"))) {
            sql.append(" and a.title like concat('%', ?, '%')");
            pageQuery.addArg(params.get("title"));
        }

        if (CommonUtil.isNotEmpty(params.get("id"))) {
            sql.append(" and a.id = ?");
            pageQuery.addArg(params.get("id"));
        }

        if (CommonUtil.isNotEmpty(params.get("task_status"))) {
            sql.append(" and a.task_status = ?");
            pageQuery.addArg(params.get("task_status"));
        }

        if (CommonUtil.isNotEmpty(params.get("task_id"))) {
            sql.append(" and a.task_id = ?");
            pageQuery.addArg(params.get("task_id"));
        }

        Long loginId = StpUtil.getLoginIdAsLong();
        if (CommonUtil.isNotEmpty(loginId)) {
            sql.append(" and a.create_by = ?");
            pageQuery.addArg(loginId);
        }

        sql.append(" order by a.create_time desc");
        pageQuery.setBaseSql(sql.toString());

        return baseJdbcDao.query(AiNoteInfo.class, pageQuery);
    }


    /**
     * Qwen大模型调用
     * @param params
     * @return
     * @throws NoApiKeyException
     * @throws InputRequiredException
     */
    public String queryQwen(Map<String, Object> params) throws NoApiKeyException, InputRequiredException {
        if(params == null || CommonUtil.isEmpty(params.get("message"))) {
            throw new MyException("输入的内容不能为空");
        }

        String prompt = params.get("prompt") == null ? "You are a helpful assistant." : params.get("prompt").toString();
        String message = params.get("message").toString();
        QwenClient qwenClient = new QwenClient();

        try {
            // 调用Qwen模型
            GenerationResult result = qwenClient.callQwenMaxLatest(prompt, message);
            return CommonUtil.extractQwenContent(JsonUtils.toJson(result));
        } catch (ApiException | NoApiKeyException | InputRequiredException | IOException e) {
            // 使用日志框架记录异常信息
            throw  new MyException("调用生成服务时出错： " + e.getMessage());
        }

    }

    public String queryQwenTurbo(Map<String, Object> params) throws NoApiKeyException, InputRequiredException {
        if(params == null || CommonUtil.isEmpty(params.get("message"))) {
            throw new MyException("输入的内容不能为空");
        }

        String prompt = params.get("prompt") == null ? "You are a helpful assistant." : params.get("prompt").toString();
        String message = params.get("message").toString();
        QwenClient qwenClient = new QwenClient();

        try {
            // 调用Qwen模型
            GenerationResult result = qwenClient.callQwenMaxLatest(prompt, message, Constant.DASH_SCOPE_MODEL_QWEN_TURBO);
            return CommonUtil.extractQwenContent(JsonUtils.toJson(result));
        } catch (ApiException | NoApiKeyException | InputRequiredException | IOException e) {
            // 使用日志框架记录异常信息
            throw  new MyException("调用生成服务时出错： " + e.getMessage());
        }

    }

}
