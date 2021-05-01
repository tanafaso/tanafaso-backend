package com.azkar.configs;

import com.azkar.configs.authentication.JwtAuthenticationFilter;
import com.azkar.configs.authentication.OAuthSuccessHandler;
import com.azkar.controllers.AuthenticationController;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  public static final String[] PRE_AUTHENTICAITON_ALLOWED_ENDPOINT_PATTERNS = {
      AuthenticationController.REGISTER_WITH_EMAIL_PATH,
      AuthenticationController.VERIFY_EMAIL_PATH,
      AuthenticationController.LOGIN_WITH_EMAIL_PATH,
      AuthenticationController.LOGIN_WITH_FACEBOOK_PATH
  };
  @Autowired
  JwtAuthenticationFilter jwtAuthenticationFilter;
  @Autowired
  OAuthSuccessHandler oauthSuccessHandler;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .antMatchers(PRE_AUTHENTICAITON_ALLOWED_ENDPOINT_PATTERNS)
        .permitAll()
        .antMatchers("/**")
        .authenticated()
        .and()
        .oauth2Login()
        .failureUrl("/loginFailure")
        .successHandler(oauthSuccessHandler)
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    // allowing cors origin.
    http.cors().and().csrf().disable();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("*"));
    configuration.setAllowedMethods(Arrays.asList("*"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
