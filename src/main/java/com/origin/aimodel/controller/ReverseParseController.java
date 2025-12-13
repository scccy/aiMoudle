package com.origin.aimodel.controller;

import com.origin.aimodel.base.ResultData;
import com.origin.aimodel.domain.vo.ReverseParseRequest;
import com.origin.aimodel.domain.vo.ReverseParseResponse;
import com.origin.aimodel.service.ReverseParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 反向解析 Controller
 * 提供接口用于从实际请求数据生成 paramItem 和 headerItem 配置
 *
 * @author origin
 * @since 2025-12-13
 */
@Slf4j
@RestController
@RequestMapping("/api/reverse-parse")
public class ReverseParseController {

    private final ReverseParseService reverseParseService;

    @Autowired
    public ReverseParseController(ReverseParseService reverseParseService) {
        this.reverseParseService = reverseParseService;
    }

    /**
     * 生成完整配置：根据 requestBody 和 requestHeaders 直接生成完整的配置项
     * 
     * @param request 反向解析请求（只需要 requestBody 和 requestHeaders，可选 paramMapping/headerMapping）
     * @return 生成的完整配置项（包含所有字段）
     */
    @PostMapping("/generate")
    public ResultData<?> generateConfig(@RequestBody ReverseParseRequest request) {
        try {
            return ResultData.ok(reverseParseService.generateConfig(request));
        } catch (Exception e) {
            log.error("生成配置失败", e);
            return ResultData.fail("生成配置失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置：将生成的配置保存到数据库
     * 
     * @param request 保存请求（包含模型信息和编辑后的配置项）
     * @return 保存结果
     */
    @PostMapping("/save")
    public ResultData<?> saveConfig(@RequestBody ReverseParseRequest request) {
        try {
            return ResultData.ok(reverseParseService.saveConfig(request));
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return ResultData.fail("保存配置失败: " + e.getMessage());
        }
    }
}
