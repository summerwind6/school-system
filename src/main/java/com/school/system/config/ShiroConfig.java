package com.school.system.config;

import com.school.system.shiro.UserRealm;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {
    
    @Bean
    public HashedCredentialsMatcher hashedCredentialsMatcher() {
        HashedCredentialsMatcher matcher = new HashedCredentialsMatcher();
        matcher.setHashAlgorithmName("SHA-256");
        matcher.setHashIterations(1);
        return matcher;
    }
    
    @Bean
    public UserRealm userRealm() {
        UserRealm userRealm = new UserRealm();
        userRealm.setCredentialsMatcher(hashedCredentialsMatcher());
        return userRealm;
    }
    
    @Bean
    public DefaultWebSessionManager sessionManager() {
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setGlobalSessionTimeout(30 * 60 * 1000); // 30分钟
        sessionManager.setDeleteInvalidSessions(true);
        sessionManager.setSessionValidationSchedulerEnabled(true);
        
        // 关键：关闭URL中的JSESSIONID重写，避免影响RESTful风格
        sessionManager.setSessionIdUrlRewritingEnabled(false);
        
        return sessionManager;
    }
    
    @Bean
    public DefaultWebSecurityManager securityManager() {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(userRealm());
        securityManager.setSessionManager(sessionManager());
        return securityManager;
    }
    
    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean() {
        ShiroFilterFactoryBean shiroFilter = new ShiroFilterFactoryBean();
        shiroFilter.setSecurityManager(securityManager());
        
        // 添加CORS过滤器
        Map<String, Filter> filters = new LinkedHashMap<>();
        filters.put("cors", new CorsFilter());
        filters.put("invalidRequest", new InvalidRequestFilter());
        shiroFilter.setFilters(filters);
        
        Map<String, String> filterMap = new LinkedHashMap<>();
        
        // 过滤链匹配的是 context-path（/api）之后的路径，且顺序即优先级。
        // 具体角色校验由 Controller 注解完成；这里保证任何业务接口先经过认证。
        filterMap.put("/auth/login", "cors,anon");
        filterMap.put("/auth/register", "cors,anon");
        filterMap.put("/auth/unauthorized", "cors,anon");
        filterMap.put("/auth/forbidden", "cors,anon");
        // 使用 authc 而非 Shiro 内置 logout 过滤器：后者会返回 302 重定向，
        // 而前后端分离的 Axios 调用需要由 AuthController 返回 JSON 结果。
        filterMap.put("/auth/logout", "cors,authc");
        filterMap.put("/**", "cors,authc");
        
        shiroFilter.setFilterChainDefinitionMap(filterMap);
        
        shiroFilter.setLoginUrl("/api/auth/unauthorized");
        shiroFilter.setUnauthorizedUrl("/api/auth/forbidden");
        
        return shiroFilter;
    }
    
    /**
     * CORS跨域过滤器 - 解决前后端分离Session问题
     */
    public static class CorsFilter implements Filter {
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 设置CORS头
            httpResponse.setHeader("Access-Control-Allow-Origin", httpRequest.getHeader("Origin"));
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpResponse.setHeader("Access-Control-Max-Age", "3600");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            
            // 关键：允许携带Cookie/凭证
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            
            // 处理预检请求
            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                httpResponse.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            
            chain.doFilter(request, response);
        }
    }
    
    /**
     * 无效请求过滤器 - 处理未认证请求返回JSON而非跳转
     */
    public static class InvalidRequestFilter implements Filter {
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            ShiroHttpServletRequest shiroRequest = (ShiroHttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 如果是AJAX请求且用户未登录，返回401 JSON而非跳转
            if ("XMLHttpRequest".equalsIgnoreCase(shiroRequest.getHeader("X-Requested-With")) ||
                (shiroRequest.getHeader("Accept") != null && 
                 shiroRequest.getHeader("Accept").contains("application/json"))) {
                
                // 继续执行，让Shiro处理认证失败
                chain.doFilter(request, response);
            } else {
                chain.doFilter(request, response);
            }
        }
    }
}
