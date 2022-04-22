package com.forum.community.config;

import com.forum.community.entity.User;
import com.forum.community.service.UserService;
import com.forum.community.util.CommunityConstant;
import com.forum.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 忽略静态资源的访问
        web.ignoring().antMatchers("/resources/**");
    }

    // AuthenticationManager: 认证的核心接口
    // AuthenticationManagerBuilder: 用于构建AuthenticationManager对象的工具
    // ProviderManager: AuthenticationManager接口的默认实现类

/*    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // 内置的认证规则
        // auth.userDetailsService(userService).passwordEncoder(new Pbkdf2PasswordEncoder("12345"));

        // 自定义认证规则
        // AuthenticationProvider: ProviderManager持有一组AuthenticationProvider
        // 每个AuthenticationProvider负责一种认证
        // 委托模式: ProviderManager将认证委托给AuthenticationProvider
        auth.authenticationProvider(new AuthenticationProvider() {
            // Authentication: 用于封装认证信息的接口, 不同的实现类代表不同类型的认证信息
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = (String) authentication.getCredentials();

                User user = userService.findUserByName(username);
                return null;
            }

            // 当前的AuthenticationProvider支持那种类型的认证
            @Override
            public boolean supports(Class<?> authentication) {
                // UsernamePassWordAuthenticationToken: Authentication接口的常用的实现类
                return UsernamePasswordAuthenticationToken.class.equals(authentication);
            }
        });
    }*/

/*    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 登录相关配置
        http.formLogin()
                .loginPage("/loginpage")
                .loginProcessingUrl("/login")
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                        response.sendRedirect(request.getContextPath() + "/index");
                    }
                })
                .failureHandler(new AuthenticationFailureHandler() {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                        request.setAttribute("error", exception.getMessage());
                        request.getRequestDispatcher("/loginpage").forward(request,response);
                    }
                });

        // 退出相关配置
        http.logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(new LogoutSuccessHandler() {
                    @Override
                    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                        response.sendRedirect(request.getContextPath() + "/index");
                    }
                });

        // 授权配置
        http.authorizeRequests()
                .antMatchers("/letter").hasAnyAuthority("USER","ADMIN")
                .antMatchers("/admin").hasAnyAuthority("ADMIN")
                .and().exceptionHandling().accessDeniedPage("/denied");

        // 增加Filter，处理验证码
        http.addFilterBefore(new Filter() {
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                if(request.getServletPath().equals("/login")){
                    String verifyCode = request.getParameter("verifyCode");

                }
                // 让请求继续向下执行
                filterChain.doFilter(request,response);
            }
        }, UsernamePasswordAuthenticationFilter.class);

        // 记住我
        http.rememberMe()
                .tokenRepository(new InMemoryTokenRepositoryImpl())
                .tokenValiditySeconds(3600 * 24)
                .userDetailsService(userService);

    }*/

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers("/discuss/top",
                        "/discuss/wonderful")
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers("/discuss/top",
                        "/discuss/wonderful",
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**")
                .hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll().and().csrf().disable();

        // 权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 没有登录
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"Not login yet"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"No priviledge."));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });

        // Security 底层默认会拦截 /logout 请求， 进行退出处理
        // 覆盖它默认的逻辑，才能执行我们自己的代码
        http.logout().logoutUrl("/securitylogout");
    }
}
