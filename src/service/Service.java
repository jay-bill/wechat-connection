package service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.DocumentException;

import entity.TextMessage;
import entity.User;

public interface Service {
	
	/*
	 * 目前只支持第三方登录。
	 * 每次来到index.html页面，用javascript先检测客户端的cookie
	 * 判断是否有user对象存在；如果存在，js将直接登录。
	 * 如果没有cookie存在，表明是 第一次登录，先根据第三方来源、账号查询数据库是否已经存在：
	 * 如果不存在，将获取到的来源（QQ、微信等）、账号、当前昵称插入数据库中。
	 * 完成后将该用户的账号存到webnotebookinfo_cookie的Cookie里面。
	 * 最后将用户对象返回给控制器，用控制器把账户存到session里面。
	 */
	public User login(String id);
	
	/*
	 * 检测微信平台登录是否匹配。接收三个字符串，先将他们按照字典顺序排序，再以此生成新的字符串
	 * 然后使用shell加密算法
	 */
	public boolean checkSignature(String signature,String timestamp,String nonce);
    
	/*
	 * 接收从微信前台传入的xml格式的数据，并且其转换成集合类型
	 */
	public Map<String,String> xmlToMap(HttpServletRequest request) throws IOException, DocumentException;
	
	/*
	 * 将文本对象转换成xml格式的字符串，用于响应客户端的请求
	 */
	public String textToXml(TextMessage text,HttpServletRequest request);
	
	/*
	 * 获取用户信息
	 */
	public void getInfo(HttpServletRequest request);
	
	public void createMenu(String s);
}
