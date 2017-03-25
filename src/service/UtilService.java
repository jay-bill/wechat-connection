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
	//shal�����㷨
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

		//���ߣ����ڳ�ȡ��ҳ��html�е�json���ݣ�����json
		public String getJson(String context){
			System.out.println("�鿴context��ֵ��"+context);
			String regex = "\\{[\\s\\S]*\\}";//������ʽ��ƥ������json
			String json = null;
			Pattern p = Pattern.compile(regex);//ʹ�ø������ƥ��
			Matcher m = p.matcher(context);
			while(m.find())
				json = m.group(); //ƥ�䵽json
			json = "["+json+"]";
			System.out.println("����json��ֵ�Ƿ����룺"+json);
			return json;
		}
		//���ߣ���ȡurl�е�html����
		public String readURL(String source){
			//��ȡ��ҳ��html���룬���ҳ�ȡ��json����
				URL url = null;
				String context = null;
				StringBuilder sb = new StringBuilder();
				BufferedReader reader = null;
				BufferedWriter out = null;
				try {
					url = new URL(source);
					reader = new BufferedReader(new InputStreamReader(url.openStream(),"utf-8")); //��ȡhtml����
					out = new BufferedWriter(new FileWriter("F:/json.txt"));
					while((context = reader.readLine())!=null){
						sb.append(context);//��html��ȡ��sb������
					}
					out.write(sb.toString());
					out.flush(); //û������
					out.close();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return sb.toString();
		}
}
