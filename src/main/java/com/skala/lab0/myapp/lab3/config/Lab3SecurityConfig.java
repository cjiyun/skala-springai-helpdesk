package com.skala.lab0.myapp.lab3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class Lab3SecurityConfig {
  @Bean
  SecurityFilterChain lab3SecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/lab3/admin/**"))
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .httpBasic(basic -> {})
        .build();
  }
}
