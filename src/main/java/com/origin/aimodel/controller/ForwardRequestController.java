package com.origin.aimodel.controller;

import com.origin.aimodel.base.ResultData;
import com.origin.aimodel.domain.vo.ForwardGenerateRequest;
import com.origin.aimodel.domain.vo.ForwardModelVO;
import com.origin.aimodel.domain.vo.ForwardRequestResult;
import com.origin.aimodel.service.ForwardRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 正向请求相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-model")
public class ForwardRequestController {

    private final ForwardRequestService forwardRequestService;

    @Autowired
    public ForwardRequestController(ForwardRequestService forwardRequestService) {
        this.forwardRequestService = forwardRequestService;
    }

    /**
     * 获取模型列表（用于前端下拉选择）
     */
    @GetMapping("/list")
    public ResultData<?> listModels() {
        return ResultData.ok(forwardRequestService.listModels());
    }

    /**
     * 根据模型配置和输入参数生成请求数据
     */
    @PostMapping("/generate-request")
    public ResultData<?> generateRequest(@RequestBody ForwardGenerateRequest request) {
        try {
            return ResultData.ok(forwardRequestService.generateRequest(request));
        } catch (IllegalArgumentException e) {
            log.warn("生成正向请求参数失败: {}", e.getMessage());
            return ResultData.fail(e.getMessage());
        } catch (Exception e) {
            log.error("生成正向请求参数异常", e);
            return ResultData.fail("生成请求失败: " + e.getMessage());
        }
    }
}
