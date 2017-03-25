package service;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.qq.weixin.mp.aes.AesException;
import com.qq.weixin.mp.aes.WXBizMsgCrypt;
import com.thoughtworks.xstream.XStream;

import entity.TextMessage;
import entity.Token;
import entity.User;

@Component
public class ServiceImf implements Service{

	static int times = 0; //记录getBaseAccessToken()调用的次数
	private static String base_access_token;
	private WXBizMsgCrypt wx;
	private UtilService shalUtil;
	private Token access;
	private User user;
	
	@Autowired
	public void setUser(User user) {
		this.user = user;
	}
	@Autowired
	public void setTokenClass(Token tokenAccess) {
		this.access = tokenAccess;
	}
	@Autowired
	public void setShalUtil(UtilService shalUtil){
		this.shalUtil = shalUtil;
	}
	@Autowired
	public void setWx(WXBizMsgCrypt wx){
		this.wx = wx;
	}
	@Override
	public User login(String id) {
		return null;
	}

	public String getBase_access_token() {
		return base_access_token;
	}

	//获取微信access_token，并及时刷新。只能在外部调用一次。
	private void getBaseAccessToken(){
		String source = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
		source = source.replace("APPID", "wxcf5135a1d7227cb3");
		source = source.replace("APPSECRET", "4bbfaf482e22dbdf0ce4f3cd8caad638");
		String json = shalUtil.readURL(source);
		json = "["+json+"]";
		//解析json字符串
		JSONArray array = JSONArray.fromObject(json);
		for (int i = 0; i < array.size(); i++) {
			JSONObject obj = array.getJSONObject(i);
			base_access_token = obj.getString("access_token"); //将微信接口的凭证access_token存储起来
			System.out.println("微信的凭证access_token是："+base_access_token);
		}
		Timer timer = new Timer();
		TimerTask task = new TimerTask(){
			@Override
			public void run() {
				getBaseAccessToken();
			}			
		};
		timer.schedule(task, 7150000);//重新获取access_token
	}
	
	
	
	//微信公众平台接入的签名校验
	private static final String token="wechatofjaybill";
	@Override
	public boolean checkSignature(String signature,String timestamp,String nonce) {
		//1、排序
		String [] arr = new String[]{token,timestamp,nonce};
		Arrays.sort(arr);
		//2、生成新的字符串
		StringBuffer content = new StringBuffer();
		for(int i=0;i<arr.length;i++){
			content.append(arr[i]);
		}
		//3、shal加密
		String temp = shalUtil.getSha1(content.toString());
		return temp.equals(signature);
	}
	
	
	//接收前台传入的数据，将xml转换成map类型
	public Map<String,String> xmlToMap(HttpServletRequest request) throws IOException, DocumentException{
		
		Map<String,String> map = new HashMap<String,String>();
		
		//读取输入流的消息，并保存到StringBuilder里面
		request.setCharacterEncoding("utf-8");
		InputStream inputStream = request.getInputStream();//获取请求的输入流;
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("F:/wechat.xml"));
		byte [] b = new byte[1024];
		int i=0;
		StringBuffer s = new StringBuffer();
		while((i=inputStream.read(b))!=-1){//将输入流的内容输进字符串		
			s.append(new String(b,0,i));
			out.write(b, 0, i);
			out.flush();
		}
		inputStream.close();
		inputStream = null; 

		//解密消息
		String m = null;  //解密后的消息
		if("aes".equals(request.getParameter("encrypt_type"))){ //如果使用了安全模式
			try {
				m = wx.decryptMsg(request.getParameter("msg_signature"), request.getParameter("timestamp"), 
						request.getParameter("nonce"), s.toString());
			} catch (AesException e) {
				e.printStackTrace();
			}
		}
		
		//将解秘后的xml的结点保存到map
		Document doc = null;
		String temp = new String(s.toString().getBytes(),"utf-8");//StringBuffer直接toString不是utf编码。
		System.out.println(temp);	
		SAXReader reader = new SAXReader();  //dom4j的类，用于操作xml
		if(m!=null)  //如果是安全模式
			doc = reader.read(new ByteArrayInputStream(m.getBytes("UTF-8")));  //dom4j读取揭秘后的xml文档
		else     //如果为明文模式
			doc = reader.read(new ByteArrayInputStream(temp.getBytes("UTF-8")));  //dom4j读取揭秘后的xml文档
		Element root = doc.getRootElement();//获取根元素
		List<Element> list = root.elements();//获取根节点下的所有元素
		//遍历list集合，将list的值保存在map中
		for(Element e : list)
			map.put(e.getName(), e.getText());  //保存name和text
		return map;
	}
	
	//发送消息，将文本对象转换成xml
	public String textToXml(TextMessage text,HttpServletRequest request){
		XStream xstream = new XStream();
		xstream.alias("xml",text.getClass());
		String mess = xstream.toXML(text);
//		System.out.println("转换的结果是："+mess);
		if("aes".equals(request.getParameter("encrypt_type"))){
			try {
				wx.encryptMsg(mess, request.getParameter("timestamp"), request.getParameter("nonce")); //加密
			} catch (AesException e) {
				e.printStackTrace();
			}
		}
		return mess;//转换成xml
	}
	
	//获取用户信息
	public void getInfo(HttpServletRequest request){
		try {
			request.setCharacterEncoding("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		getAccessToken(request);
	}
	//通过code获取access_token，保存到Token对象里面
	private void getAccessToken(HttpServletRequest request){
		//通过code获取access_token
		String source = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
		//替换source里面的参数
		source = source.replace("APPID", "wxcf5135a1d7227cb3");
		source = source.replace("SECRET", "4bbfaf482e22dbdf0ce4f3cd8caad638");
		source = source.replace("CODE", request.getParameter("code"));		
		System.out.println("授权成功");
		
		String sb =	shalUtil.readURL(source);  //读取网页html文件
		String json = shalUtil.getJson(sb);  //从该html中获取json格式的字符串
		System.out.println(json);
		//解析json字符串
		JSONArray array = JSONArray.fromObject(json);
		for (int i = 0; i < array.size(); i++) {
			JSONObject obj = array.getJSONObject(i);
			//将json的数值存到Token的实例中
			access.setAccess_token(obj.getString("access_token"));
			access.setExpires_in(obj.getString("expires_in"));
			access.setOpenid(obj.getString("openid"));
			access.setRefresh_token(obj.getString("refresh_token"));
			access.setScope(obj.getString("scope"));
			getUserInfo(obj.getString("access_token"),obj.getString("openid"));  //获取用户信息
		}	
	}	
	/*
	 * 获取到access_token后，用access_token和openid获取用户信息
	 */
	private void getUserInfo(String accesstoken,String openid){
		String source = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
		source = source.replace("ACCESS_TOKEN", accesstoken);
		source = source.replace("OPENID", openid);
		String sb = shalUtil.readURL(source); //读取网页html文件
		String json = shalUtil.getJson(sb);  //从网页html中获取json格式的字符串
		//解析json字符串
		JSONArray array = JSONArray.fromObject(json);
		for (int i = 0; i < array.size(); i++) {
			JSONObject obj = array.getJSONObject(i);
			//保存到user实例中
			user.setOpenid(obj.getString("openid"));
			user.setNickname(obj.getString("nickname"));
			user.setSex(obj.getString("sex"));
			user.setCity(obj.getString("city"));
			user.setProvince(obj.getString("province"));
			user.setCountry(obj.getString("country"));
			user.setHeadimgurl(obj.getString("headimgurl"));
			System.out.println(user);
		}		
	}
	
	
	//创建自定义菜单
	static final Object lock = new Object();
	public void createMenu(String s){
		synchronized(lock){
			if(times==0){
				getBaseAccessToken(); //获取access_token
				times++;
			}		
		}
		create(s,base_access_token);
	}
	private void create(String params,String accessToken) {
		StringBuffer bufferRes = new StringBuffer();
		try {
			URL realUrl = new URL("https://api.weixin.qq.com/cgi-bin/menu/create?access_token="+ accessToken);
			HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
			
			// 连接超时
			conn.setConnectTimeout(25000);
			// 读取超时 --服务器响应比较慢，增大时间
			conn.setReadTimeout(25000);
			
			HttpURLConnection.setFollowRedirects(true);
			// 请求方式
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
			conn.setRequestProperty("Referer", "https://api.weixin.qq.com/");
			conn.connect();
			// 获取URLConnection对象对应的输出流
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(),"utf-8");
			// 发送请求参数
			//out.write(URLEncoder.encode(params,"UTF-8"));
			out.write(params);
			out.flush();
			out.close();
			
			InputStream in = conn.getInputStream();
			BufferedReader read = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			String valueString = null;
			while ((valueString=read.readLine())!=null){
				bufferRes.append(valueString);
			}
			System.out.println(bufferRes.toString());//根据字符串判断是否成功
			in.close();
			if (conn != null) {
				// 关闭连接
				conn.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
