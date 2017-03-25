package login.controller;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONStringer;

import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import entity.TextMessage;
import service.Service;
import service.ServiceImf;

@Controller
@RequestMapping(value="/login")
public class Login {
	
	private Service service;
	@Autowired
	public void setService(ServiceImf service){
		this.service = service;
	}
	/**
	 * 测试登录
	 * @return
	 */
	public String login(){
		
		return "notelist";  
	}
	

	public void getAccessToken()
	{}
	
	/**
	 * 接入微信
	 * @param request
	 * @param response
	 */
	@RequestMapping(value="loginByWechat.do",method=RequestMethod.GET)
	public void loginByWechat(HttpServletRequest request,HttpServletResponse 
								response){
		String signature = request.getParameter("signature");
		String timestamp = request.getParameter("timestamp");
		String nonce = request.getParameter("nonce");
		String echostr = request.getParameter("echostr");
		boolean flag = service.checkSignature(signature, timestamp, nonce);
		PrintWriter p = null;
		try {
			p = response.getWriter();
			if(flag){
	        	p.print(echostr);  //回应
	        }
		} catch (IOException e) {
			e.printStackTrace();
		}				
	}
	
	/**
	 * 获取和发送文字
	 */
	@RequestMapping(value="loginByWechat.do",method=RequestMethod.POST)
	public void getAndSend(HttpServletRequest request,HttpServletResponse response){
		Map<String, String> map = null;
		//将xml转换成Map
		try {
			map = service.xmlToMap(request);
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (DocumentException e1) {
			e1.printStackTrace();
		}
		
		//获取xml中的信息
		String toUserName = map.get("ToUserName");
		String fromUserName = map.get("FromUserName");
		String msgType = map.get("MsgType");
		String content = map.get("Content");
		
		//转换成xml格式，包装并发送回微信客户端
		String message = null; 
		if("text".equalsIgnoreCase(msgType)){ 
			TextMessage text = new TextMessage();
			text.setFromUserName(toUserName);
			text.setToUserName(fromUserName);
			text.setCreateTime(new Date().getTime());
			text.setMsgType("text");
			text.setContent("您发送的内容是："+content); 
			message = service.textToXml(text,request);
		}
		
		PrintWriter print = null;
		try {
			response.setCharacterEncoding("utf-8");
			print = response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		print.print(message);//发送
		
		//创建底部的菜单
		String s = "{\"button\":[{\"type\":\"view\",\"name\":\"我的笔记\",\"url\":\"http://example.tunnel.qydev.com/notebook/login/getInWeb.do\"},{\"type\":\"click\",\"name\":\"其他\",\"key\":\"M3001\"}]}";
		System.out.println(s);
		service.createMenu(s);
	}
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value="getInWeb.do")
	public String getInWeb(HttpServletRequest request,HttpServletResponse response){
		String source = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect";
		source = source.replace("APPID", "wxcf5135a1d7227cb3");//换成自己的
		source = source.replace("REDIRECT_URI", "http%3A%2F%2Fexample.tunnel.qydev.com%2Fnotebook%2Flogin%2Fsuccess.do");
		source = source.replace("SCOPE", "snsapi_userinfo");
		return "redirect:"+source;  
	}
	
	
	@RequestMapping("success.do")
	public String getToken(HttpServletRequest request){
		service.getInfo(request); //获取用户信息
		return "notelist";
	}
	
	
	@RequestMapping("/test.do")
	public String test(HttpServletRequest request){
		return "contentlist";
	}
}
