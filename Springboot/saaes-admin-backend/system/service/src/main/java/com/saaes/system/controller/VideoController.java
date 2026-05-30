package com.saaes.system.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.saaes.common.core.Constant;
import com.saaes.common.core.web.MyException;
import com.saaes.common.core.web.RestResponse;
import com.saaes.system.client.entity.SysFile;
import com.saaes.system.service.SysFileService;
import com.saaes.system.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "AI视频分析")
@RestController
@RequestMapping("/api/video")
@Slf4j
public class VideoController {

    @Resource
    private SysFileService sysFileService;

    @Resource
    private VideoService videoService;

    /**
     * 一条历史记录对一个分析结果
     * @param file
     * @param historyId
     * @return
     */
    @SaIgnore
    @Operation(description = "毽球视频上传处理接口")
    @PostMapping(path = "/addAnalysisTask", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestResponse<?> addAnalysisTask(@RequestParam("file") MultipartFile file, @RequestParam("historyId") Integer historyId) {
        try {
            SysFile sysFile = sysFileService.uploadFile(file);
            System.out.println(sysFile.toString());

            if (sysFile.getObject() == null) {
                throw new MyException("文件上传失败");
            }


            return RestResponse.success(videoService.save(sysFile, historyId));
        } catch (Exception e) {
            log.error("视频任务创建失败", e);
            return RestResponse.error(e.getMessage());
        }
    }

    @SaIgnore
    @Operation(description = "返回主页视频链接")
    @GetMapping("/getHomeVideoUrlList")
    public RestResponse<List> getHomeVideoUrlList() {
        List<String> list = new ArrayList<>();
        list.add(Constant.BASE_URL + "/files/home/friendship.mp4");
        list.add(Constant.BASE_URL + "/files/home/Hongkongtrainingcenter.mp4");
        list.add(Constant.BASE_URL + "/files/home/JackieChanPlaysJianzi.mp4");
        list.add(Constant.BASE_URL + "/files/home/LightShuttlecock-FrancePlumfoot.mp4");
        list.add(Constant.BASE_URL + "/files/home/Matchpoint.mp4");
        list.add(Constant.BASE_URL + "/files/home/shuttlecocksmashslowmotion.mp4");
        return RestResponse.success("查询成功", list);
    }

}
