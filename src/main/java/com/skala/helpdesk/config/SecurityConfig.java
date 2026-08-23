package com.skala.helpdesk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.MediaType;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .authorizeHttpRequests(requests -> requests
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/chat/**", "/api/admin/**").authenticated()
            .anyRequest().permitAll())
        .formLogin(form -> form
            .loginProcessingUrl("/api/auth/login")
            .successHandler((request, response, authentication) -> response.setStatus(204))
            .failureHandler((request, response, exception) -> {
              response.setStatus(401);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.getWriter().write("{\"message\":\"인증 정보가 올바르지 않습니다.\"}");
            })
            .permitAll())
        .logout(logout -> logout
            .logoutUrl("/api/auth/logout")
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
        .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) -> {
          response.setStatus(401);
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          response.getWriter().write("{\"message\":\"인증 정보가 올바르지 않습니다.\"}");
        }))
        .build();
  }

  @Bean
  UserDetailsService users(
      @Value("${HELPDESK_USER_PASSWORD:user}") String userPassword,
      @Value("${HELPDESK_ADMIN_PASSWORD:admin}") String adminPassword) {
    return new InMemoryUserDetailsManager(
        User.withUsername("user1").password("{noop}" + userPassword).roles("USER").build(),
        User.withUsername("user2").password("{noop}" + userPassword).roles("USER").build(),
        User.withUsername("admin").password("{noop}" + adminPassword).roles("ADMIN").build());
  }
}
