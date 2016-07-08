package com.locationshare.utils.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;

import com.locationshare.app.model.User;
/**
 * @author Jason_zh
 * @date 2016��6��1��
 * @version 1.0
 */
public class SecurityServlet extends HttpServlet implements Filter{
	 private static final long serialVersionUID = 1L;  
	 public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2) throws IOException, ServletException {  
         HttpServletRequest request=(HttpServletRequest)arg0;     
         HttpServletResponse response  =(HttpServletResponse) arg1;      
         HttpSession session = request.getSession(true);       
         User user_role = (User)session.getAttribute("user");//��¼�û�
         String url=request.getRequestURI();
         if(user_role == null) {
        	 boolean isAjaxRequest = isAjaxRequest(request);
              //�жϻ�ȡ��·����Ϊ���Ҳ��Ƿ��ʵ�¼ҳ���ִ�е�¼����ʱ��ת     
              if(url!=null && !url.equals("") && ( url.indexOf("register")<0 && url.indexOf("login")<0 )) {
            	  if (isAjaxRequest) {
      				response.setCharacterEncoding("UTF-8");
      				response.sendError(HttpStatus.UNAUTHORIZED.value(),"���Ѿ�̫��ʱ��û�в����������µ�¼");
              	  }else{
              		response.setCharacterEncoding("UTF-8");
      				response.sendError(HttpStatus.UNAUTHORIZED.value(),"���ȵ�¼");
              	  }
            	  //response.sendRedirect(request.getContextPath() + "/user/login.do");     
                  return ;     
              }
          }
          arg2.doFilter(arg0, arg1);     
          return;     
	  } 
	 
	 
	 /** 
	     * �ж��Ƿ�ΪAjax���� 
	     * 
	     * @param request HttpServletRequest 
	     * @return ��true, ��false 
	     */  
	    public static boolean isAjaxRequest(HttpServletRequest request) {  
	        return request.getRequestURI().startsWith("/api");  
//	        String requestType = request.getHeader("X-Requested-With");  
//	        return requestType != null && requestType.equals("XMLHttpRequest");  
	    }
	    
	  public void init(FilterConfig arg0) throws ServletException {  
	  }
	 
	  
}  
