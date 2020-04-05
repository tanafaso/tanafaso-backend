package com.azkar.interceptors;

import com.azkar.configs.authentication.UserPrincipal;
import com.azkar.entities.CurrentUser;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class UserInterceptor extends HandlerInterceptorAdapter {

  @Autowired
  CurrentUser currentUser;

  @Override
  public boolean preHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler) {
    PreAuthenticatedAuthenticationToken token =
        (PreAuthenticatedAuthenticationToken) SecurityContextHolder.getContext()
            .getAuthentication();
    currentUser.setId(((UserPrincipal) token.getPrincipal()).getUserId());
    return true;
  }
}
