package com.ai.aicodeguard.infrastructure.graph;

import com.ai.aicodeguard.domain.codegen.document.VulnerabilityReport;

/**
 * @ClassName: KnowledgeGraphService
 * @Description: 知识图谱服务接口
 * @Author: LZX
 * @Date: 2025/4/28 15:43
 */
public interface KnowledgeGraphService {

    /**
     * 根据漏洞报告更新知识图谱
     * @param codeId 代码ID
     * @param report 漏洞报告
     * @return 是否更新成功
     */
    boolean updateGraphWithVulnerabilities(String codeId, VulnerabilityReport report);

    /**
     * 查询代码模式与漏洞关联
     * @param language 编程语言
     * @param vulnerabilityType 漏洞类型
     * @return 关联的代码模式列表
     */
    String findCodePatternsForVulnerability(String language, String vulnerabilityType);
}
