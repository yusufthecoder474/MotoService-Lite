package com.motoservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motoservice.repository.UserRepository;
import java.time.Clock;
import java.util.Map;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  PasswordEncoder encoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetails(UserRepository users) {
    return name ->
        users
            .findByUsername(name)
            .map(
                u ->
                    User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/login", "/style.css", "/error")
                    .permitAll()
                    .requestMatchers("/api/advisor/**", "/advisor/**")
                    .hasRole("SERVICE_ADVISOR")
                    .anyRequest()
                    .authenticated())
        .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
        .httpBasic(
            b ->
                b.authenticationEntryPoint(
                    (req, res, ex) -> {
                      res.setStatus(401);
                      res.setContentType("application/json");
                      res.setHeader("WWW-Authenticate", "Basic realm=\"MotoService Lite\"");
                      mapper.writeValue(
                          res.getWriter(),
                          Map.of(
                              "status",
                              401,
                              "message",
                              "Invalid credentials",
                              "path",
                              req.getRequestURI()));
                    }))
        .logout(l -> l.logoutSuccessUrl("/login?logout"))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json");
                          mapper.writeValue(
                              res.getWriter(),
                              Map.of(
                                  "status",
                                  401,
                                  "message",
                                  "Authentication required",
                                  "path",
                                  req.getRequestURI()));
                        },
                        new AntPathRequestMatcher("/api/**"))
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json");
                          mapper.writeValue(
                              res.getWriter(),
                              Map.of(
                                  "status",
                                  403,
                                  "message",
                                  "Access denied or invalid CSRF token",
                                  "path",
                                  req.getRequestURI()));
                        }));
    // CSRF intentionally remains enabled for BOTH browser and HTTP Basic API calls.
    return http.build();
  }
}
