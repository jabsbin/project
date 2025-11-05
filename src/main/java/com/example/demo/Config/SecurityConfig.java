package com.example.demo.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /**
     * PasswordEncoder 빈 등록
     * UserController, UserService 등에서 주입받을 수 있음
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 기본 설정
     * - 로그인/로그아웃 화면 비활성화
     * - API, 정적 리소스 모두 접근 허용
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // REST API라면 CSRF 비활성화
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index", "/index.html",
                                "/css/**", "/js/**", "/images/**",
                                "/api/**", "/users/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(login -> login.disable())   // ✅ 기본 로그인 화면 비활성화
                .logout(l -> l.disable());             // 로그아웃 비활성화
        return http.build();
    }
}
