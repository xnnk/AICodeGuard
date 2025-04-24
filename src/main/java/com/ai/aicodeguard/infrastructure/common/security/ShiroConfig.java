package com.ai.aicodeguard.infrastructure.common.security;

import com.ai.aicodeguard.application.service.interfaces.SysUserService;
import com.ai.aicodeguard.infrastructure.common.util.JwtUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @ClassName: ShiroConfig
 * @Description:
 * @Author: LZX
 * @Date: 2025/4/8 11:52
 */
@Configuration
public class ShiroConfig {

    /**
     * 添加用户认证到Shiro中
     * @param jwtRealm
     * @return
     */
    @Bean
    public DefaultWebSecurityManager securityManager(JwtRealm jwtRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(jwtRealm);

        // 禁用会话
        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        DefaultSessionStorageEvaluator sessionStorageEvaluator = new DefaultSessionStorageEvaluator();
        sessionStorageEvaluator.setSessionStorageEnabled(false);
        subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator);
        securityManager.setSubjectDAO(subjectDAO);

        // 使用自定义的SubjectFactory
        DefaultWebSubjectFactory subjectFactory = new JwtDefaultSubjectFactory();
        securityManager.setSubjectFactory(subjectFactory);

        // 禁用RememberMe
        securityManager.setRememberMeManager(null);

        SecurityUtils.setSecurityManager(securityManager);
        return securityManager;
    }

    /**
     * 添加拦截器
     * @param securityManager
     * @return
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilter(DefaultWebSecurityManager securityManager) {
        ShiroFilterFactoryBean factoryBean = new ShiroFilterFactoryBean();
        factoryBean.setSecurityManager(securityManager);

        Map<String, Filter> filterMap = new HashMap<>();
        filterMap.put("jwt", new JwtFilter());
        factoryBean.setFilters(filterMap);

        Map<String, String> filterRuleMap = new LinkedHashMap<>();
        filterRuleMap.put("/api/auth/login", "anon");
        filterRuleMap.put("/api/auth/register", "anon");
        filterRuleMap.put("/**", "jwt");
        factoryBean.setFilterChainDefinitionMap(filterRuleMap);

        return factoryBean;
    }

    @Bean
    public JwtRealm jwtRealm(JwtUtils jwtUtils, SysUserService sysUserService) {
        JwtRealm jwtRealm = new JwtRealm(jwtUtils, sysUserService);

        // 配置JWT凭证匹配器
        CredentialsMatcher credentialsMatcher = (token, info) -> {
            String tokenCredentials = (String) token.getCredentials();
            String storedCredentials = (String) info.getCredentials();
            return tokenCredentials.equals(storedCredentials);
        };

        jwtRealm.setCredentialsMatcher(credentialsMatcher);
        return jwtRealm;
    }

    @Bean
    public static LifecycleBeanPostProcessor lifecycleBeanPostProcessor() {
        return new LifecycleBeanPostProcessor();
    }

    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }

    /**
     * 开启shiro注解支持
     * @param securityManager
     * @return
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(DefaultWebSecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}
