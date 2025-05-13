package com.ai.aicodeguard.presentation.rest;

import com.ai.aicodeguard.domain.codegen.document.GeneratedCodeDocument;
import com.ai.aicodeguard.infrastructure.common.util.ShiroUtils;
import com.ai.aicodeguard.infrastructure.mongo.GeneratedCodeDocumentRepository;
import com.ai.aicodeguard.presentation.response.WebResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * @ClassName: GeneratedCodeController
 * @Description: 已生成代码的管理控制器
 * @Author: LZX
 * @Date: 2025/5/10 15:30
 */
@RestController
@RequestMapping("/generated-code")
@RequiredArgsConstructor
@Slf4j
public class GeneratedCodeController {

    private final GeneratedCodeDocumentRepository generatedCodeDocumentRepository;

    /**
     * 获取当前用户的所有可见代码
     */
    @GetMapping("/list")
    public WebResponse getGeneratedCodes() {
        try {
            // 从安全上下文获取用户ID
            Integer userId = ShiroUtils.getUserId();
            if (userId == null) {
                return WebResponse.fail("未登录或会话已过期，请重新登录");
            }

            // 查询当前用户的所有可见代码，按创建时间降序排列
            List<GeneratedCodeDocument> codeDocuments = generatedCodeDocumentRepository.findByUserIdAndIsVisibleTrueOrderByCreatedAtDesc(userId);
            
            return WebResponse.success(codeDocuments);
        } catch (Exception e) {
            log.error("获取生成代码列表失败", e);
            return WebResponse.fail("获取生成代码列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取代码详情
     */
    @GetMapping("/{codeId}")
    public WebResponse getGeneratedCode(@PathVariable String codeId) {
        try {
            Integer userId = ShiroUtils.getUserId();
            if (userId == null) {
                return WebResponse.fail("未登录或会话已过期，请重新登录");
            }

            Optional<GeneratedCodeDocument> codeDocumentOpt = generatedCodeDocumentRepository.findById(codeId);
            
            if (codeDocumentOpt.isEmpty()) {
                return WebResponse.fail("代码不存在");
            }
            
            GeneratedCodeDocument codeDocument = codeDocumentOpt.get();
            
            // 验证代码所属用户和可见性
            if (!codeDocument.getUserId().equals(userId) || !codeDocument.getIsVisible()) {
                return WebResponse.fail("没有权限访问此代码或代码已被删除");
            }
            
            return WebResponse.success(codeDocument);
        } catch (Exception e) {
            log.error("获取代码详情失败", e);
            return WebResponse.fail("获取代码详情失败: " + e.getMessage());
        }
    }

    /**
     * 逻辑删除代码（设置isVisible=false）
     */
    @DeleteMapping("/{codeId}")
    public WebResponse deleteGeneratedCode(@PathVariable String codeId) {
        try {
            Integer userId = ShiroUtils.getUserId();
            if (userId == null) {
                return WebResponse.fail("未登录或会话已过期，请重新登录");
            }

            Optional<GeneratedCodeDocument> codeDocumentOpt = generatedCodeDocumentRepository.findById(codeId);
            
            if (codeDocumentOpt.isEmpty()) {
                return WebResponse.fail("代码不存在");
            }
            
            GeneratedCodeDocument codeDocument = codeDocumentOpt.get();
            
            // 验证代码所属用户
            if (!codeDocument.getUserId().equals(userId)) {
                return WebResponse.fail("没有权限删除此代码");
            }
            
            // 逻辑删除：设置isVisible为false
            codeDocument.setIsVisible(false);
            generatedCodeDocumentRepository.save(codeDocument);
            
            return WebResponse.success("代码已成功删除");
        } catch (Exception e) {
            log.error("删除代码失败", e);
            return WebResponse.fail("删除代码失败: " + e.getMessage());
        }
    }
}
