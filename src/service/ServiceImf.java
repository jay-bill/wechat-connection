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

	static int times = 0; //��¼getBaseAccessToken()���õĴ���
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

	//��ȡ΢��access_token������ʱˢ�¡�ֻ�����ⲿ����һ�Ρ�
	private void getBaseAccessToken(){
		String source = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
		source = source.replace("APPID", "wxcf5135a1d7227cb3");
		source = source.replace("APPSECRET", "4bbfaf482e22dbdf0ce4f3cd8caad638");
		String json = shalUtil.readURL(source);
		json = "["+json+"]";
		//����json�ַ���
		JSONArray array = JSONArray.fromObject(json);
		for (int i = 0; i < array.size(); i++) {
			JSONObject obj = array.getJSONObject(i);
			base_access_token = obj.getString("access_token"); //��΢�Žӿڵ�ƾ֤access_token�洢����
			System.out.println("΢�ŵ�ƾ֤access_token�ǣ�"+base_access_token);
		}
		Timer timer = new Timer();
		TimerTask task = new TimerTask(){
			@Override
			public void run() {
				getBaseAccessToken();
			}			
		};
		timer.schedule(task, 7150000);//���»�ȡaccess_token
	}
	
	
	
	//΢�Ź���ƽ̨�����ǩ��У��
	private static final String token="wechatofjaybill";
	@Override
	public boolean checkSignature(String signature,String timestamp,String nonce) {
		//1������
		String [] arr = new String[]{token,timestamp,nonce};
		Arrays.sort(arr);
		//2�������µ��ַ���
		StringBuffer content = new StringBuffer();
		for(int i=0;i<arr.length;i++){
			content.append(arr[i]);
		}
		//3��shal����
		String temp = shalUtil.getSha1(content.toString());
		return temp.equals(signature);
	}
	
	
	//����ǰ̨��������ݣ���xmlת����map����
	public Map<String,String> xmlToMap(HttpServletRequest request) throws IOException, DocumentException{
		
		Map<String,String> map = new HashMap<String,String>();
		
		//��ȡ����������Ϣ�������浽StringBuilder����
		request.setCharacterEncoding("utf-8");
		InputStream inputStream = request.getInputStream();//��ȡ�����������;
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("F:/wechat.xml"));
		byte [] b = new byte[1024];
		int i=0;
		StringBuffer s = new StringBuffer();
		while((i=inputStream.read(b))!=-1){//������������������ַ���		
			s.append(new String(b,0,i));
			out.write(b, 0, i);
			out.flush();
		}
		inputStream.close();
		inputStream = null; 

		//������Ϣ
		String m = null;  //���ܺ����Ϣ
		if("aes".equals(request.getParameter("encrypt_type"))){ //���ʹ���˰�ȫģʽ
			try {
				m = wx.decryptMsg(request.getParameter("msg_signature"), request.getParameter("timestamp"), 
						request.getParameter("nonce"), s.toString());
			} catch (AesException e) {
				e.printStackTrace();
			}
		}
		
		//�����غ��xml�Ľ�㱣�浽map
		Document doc = null;
		String temp = new String(s.toString().getBytes(),"utf-8");//StringBufferֱ��toString����utf���롣
		System.out.println(temp);	
		SAXReader reader = new SAXReader();  //dom4j���࣬���ڲ���xml
		if(m!=null)  //����ǰ�ȫģʽ
			doc = reader.read(new ByteArrayInputStream(m.getBytes("UTF-8")));  //dom4j��ȡ���غ��xml�ĵ�
		else     //���Ϊ����ģʽ
			doc = reader.read(new ByteArrayInputStream(temp.getBytes("UTF-8")));  //dom4j��ȡ���غ��xml�ĵ�
		Element root = doc.getRootElement();//��ȡ��Ԫ��
		List<Element> list = root.elements();//��ȡ���ڵ��µ�����Ԫ��
		//����list���ϣ���list��ֵ������map��
		for(Element e : list)
			map.put(e.getName(), e.getText());  //����name��text
		return map;
	}
	
	//������Ϣ�����ı�����ת����xml
	public String textToXml(TextMessage text,HttpServletRequest request){
		XStream xstream = new XStream();
		xstream.alias("xml",text.getClass());
		String mess = xstream.toXML(text);
//		System.out.println("ת���Ľ���ǣ�"+mess);
		if("aes".equals(request.getParameter("encrypt_type"))){
			try {
				wx.encryptMsg(mess, request.getParameter("timestamp"), request.getParameter("nonce")); //����
			} catch (AesException e) {
				e.printStackTrace();
			}
		}
		return mess;//ת����xml
	}
	
	//��ȡ�û���Ϣ
	public void getInfo(HttpServletRequest request){
		try {
			request.setCharacterEncoding("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		getAccessToken(request);
	}
	//ͨ��code��ȡaccess_token�����浽Token��������
	private void getAccessToken(HttpServletRequest request){
		//ͨ��code��ȡaccess_token
		String source = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
		//�滻source����Ĳ���
		source = source.replace("APPID", "wxcf5135a1d7227cb3");
		source = source.replace("SECRET", "4bbfaf482e22dbdf0ce4f3cd8caad638");
		source = source.replace("CODE", request.getParameter("code"));		
		System.out.println("��Ȩ�ɹ�");
		
		String sb =	shalUtil.readURL(source);  //��ȡ��ҳhtml�ļ�
		String json = shalUtil.getJson(sb);  //�Ӹ�html�л�ȡjson��ʽ���ַ���
		System.out.println(json);
		//����json�ַ���
		JSONArray array = JSONArray.fromObject(json);
		for (int i = 0; i < array.size(); i++) {
			JSONObject obj = array.getJSONObject(i);
			//��json����ֵ�浽Token��ʵ����
			access.setAccess_token(obj.getString("access_token"));
			access.setExpires_in(obj.getString("expires_in"));
			access.setOpenid(obj.getString("openid"));
			access.setRefresh_token(obj.getString("refresh_token"));
			access.setScope(obj.getString("scope"));
			getUserInfo(obj.getString("access_token"),obj.getString("openid"));  //��ȡ�û���Ϣ
		}	
	}	
	/*
	 * ��ȡ��access_token����access_token��openid��ȡ�û���Ϣ
	 */
	private void getUserInfo(String accesstoken,String openid){
		String source = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
		source = source.replace("ACCESS_TOKEN", accesstoken);
		source = source.replace("OPENID", openid);
		String sb = shalUtil.readURL(source); //��ȡ��ҳhtml�ļ�
		String json = shalUtil.getJson(sb);  //����ҳhtml�л�ȡjson��ʽ���ַ���
		//����json�ַ���
		JSONArray array = JSONArray.fromObject(json);
		for (int i = 0; i < array.size(); i++) {
			JSONObject obj = array.getJSONObject(i);
			//���浽userʵ����
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
	
	
	//�����Զ���˵�
	static final Object lock = new Object();
	public void createMenu(String s){
		synchronized(lock){
			if(times==0){
				getBaseAccessToken(); //��ȡaccess_token
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
			
			// ���ӳ�ʱ
			conn.setConnectTimeout(25000);
			// ��ȡ��ʱ --��������Ӧ�Ƚ���������ʱ��
			conn.setReadTimeout(25000);
			
			HttpURLConnection.setFollowRedirects(true);
			// ����ʽ
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
			conn.setRequestProperty("Referer", "https://api.weixin.qq.com/");
			conn.connect();
			// ��ȡURLConnection�����Ӧ�������
			OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(),"utf-8");
			// �����������
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
			System.out.println(bufferRes.toString());//�����ַ����ж��Ƿ�ɹ�
			in.close();
			if (conn != null) {
				// �ر�����
				conn.disconnect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
