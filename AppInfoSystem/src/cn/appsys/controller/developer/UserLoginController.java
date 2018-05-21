package cn.appsys.controller.developer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.Session;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import cn.appsys.pojo.BackendUser;
import cn.appsys.service.backend.BackendUserService;
import cn.appsys.tools.Constants;

@Controller
@RequestMapping(value="/manager")
public class UserLoginController {
	@Resource
	private BackendUserService backendUserService;
	
	@RequestMapping(value="/login")
	public String login() {
		return "backendlogin";
	}
	
	/**
	 * 登录方法
	 * @param userCode
	 * @param userPassword
	 * @param request
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/dologin",method=RequestMethod.POST)
	public String doLigin(@RequestParam String userCode,@RequestParam String userPassword,HttpServletRequest request,HttpSession session) {
		BackendUser backendUser = null;
		try {
			//放入session
			backendUser = backendUserService.login(userCode, userPassword);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		if (null != backendUser) {
			session.setAttribute(Constants.USER_SESSION,backendUser);
			return "redirect:/manager/flatform/main";
		} else {
			//页面跳转（login.jsp）带出提示信息--转发
			request.setAttribute("error", "用户名或密码错误");
			return "backendlogin";
		}
	}
	
	/**
	 * 跳转登陆页面
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/flatform/main")
	public String main(HttpSession session) {
		if (session.getAttribute(Constants.USER_SESSION) == null) {
			return "redirect:/manager/login";
		}
		return "backend/main";
	}
	
	/**
	 * 退出登录
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/logout")
	public String outLogin(HttpSession session) {
		//清除session
		session.removeAttribute(Constants.USER_SESSION);
		return "backendlogin";
	}
}
