package com.ai.aicodeguard.infrastructure.common.security;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;

/**
 * @ClassName: JwtDefaultSubjectFactory
 * @Description: 自定义SubjectFactory，用于JWT无状态认证
 * @Author: LZX
 * @Date: 2025/4/24 11:35
 */
public class JwtDefaultSubjectFactory extends DefaultWebSubjectFactory {
    @Override
    public Subject createSubject(SubjectContext context) {
        // 不创建session
        context.setSessionCreationEnabled(false);
        return super.createSubject(context);
    }
}
