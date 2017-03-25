package service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class UtilService {
	//shal加密算法
		public String getSha1(String str){
		    if (null == str || 0 == str.length())
		    	return null;
		    char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
		            'a', 'b', 'c', 'd', 'e', 'f'};
		    try {
		        MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
		        mdTemp.update(str.getBytes("UTF-8"));
		        byte[] md = mdTemp.digest();
		        int j = md.length;
		        char[] buf = new char[j * 2];
		        int k = 0;
		        for (int i = 0; i < j; i++) {
		            byte byte0 = md[i];
		            buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
		            buf[k++] = hexDigits[byte0 & 0xf];
		        }
		        return new String(buf);
		    } catch (Exception e) {
		        e.printStackTrace();
		        return null;
		    }
		}

		//工具，用于抽取网页的html中的json数据，返回json
		public String getJson(String context){
			System.out.println("查看context的值："+context);
			String regex = "\\{[\\s\\S]*\\}";//正则表达式，匹配所有json
			String json = null;
			Pattern p = Pattern.compile(regex);//使用该类进行匹配
			Matcher m = p.matcher(context);
			while(m.find())
				json = m.group(); //匹配到json
			json = "["+json+"]";
			System.out.println("测试json的值是否乱码："+json);
			return json;
		}
		//工具，读取url中的html代码
		public String readURL(String source){
			//读取网页的html代码，并且抽取出json数据
				URL url = null;
				String context = null;
				StringBuilder sb = new StringBuilder();
				BufferedReader reader = null;
				BufferedWriter out = null;
				try {
					url = new URL(source);
					reader = new BufferedReader(new InputStreamReader(url.openStream(),"utf-8")); //获取html代码
					out = new BufferedWriter(new FileWriter("F:/json.txt"));
					while((context = reader.readLine())!=null){
						sb.append(context);//将html读取到sb串里面
					}
					out.write(sb.toString());
					out.flush(); //没有乱码
					out.close();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return sb.toString();
		}
}
