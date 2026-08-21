package com.skala.lab0.myapp.lab3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class Lab3SecurityConfig {
  @Bean
  SecurityFilterChain lab3SecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/lab3/**", "/api/**"))
        .authorizeHttpRequests(requests -> requests
            .requestMatchers("/api/chat/**", "/api/admin/**").authenticated()
            .anyRequest().permitAll())
        .httpBasic(basic -> {})
        .build();
  }

  @Bean
  UserDetailsService lab3Users(
      @Value("${LAB3_USER_PASSWORD:user}") String userPassword,
      @Value("${LAB3_ADMIN_PASSWORD:admin}") String adminPassword) {
    return new InMemoryUserDetailsManager(
        User.withUsername("user1").password("{noop}" + userPassword).roles("USER").build(),
        User.withUsername("user2").password("{noop}" + userPassword).roles("USER").build(),
        User.withUsername("admin").password("{noop}" + adminPassword).roles("ADMIN").build());
  }
}
