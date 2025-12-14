package com.scccy.aimodel.controller;

import com.scccy.aimodel.base.ResultData;
import com.scccy.aimodel.domain.vo.ForwardGenerateRequest;
import com.scccy.aimodel.service.ForwardRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 正向请求相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-model")
public class ForwardRequestController {
    @Autowired
    ForwardRequestService forwardRequestService;




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

    /**
     * 获取模型的基础请求参数（根据 item 配置自动生成）
     *
     * @param modelName 模型名称
     * @return 基础请求参数对象
     */
    @GetMapping("/base-params/{modelName}")
    public ResultData<?> getModelBaseParams(@PathVariable String modelName) {
        try {
            return ResultData.ok(forwardRequestService.getModelBaseParams(modelName));
        } catch (IllegalArgumentException e) {
            log.warn("获取模型基础参数失败: {}", e.getMessage());
            return ResultData.fail(e.getMessage());
        } catch (Exception e) {
            log.error("获取模型基础参数异常", e);
            return ResultData.fail("获取基础参数失败: " + e.getMessage());
        }
    }
}
