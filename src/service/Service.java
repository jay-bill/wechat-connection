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
	 * Ŀǰֻ֧�ֵ�������¼��
	 * ÿ������index.htmlҳ�棬��javascript�ȼ��ͻ��˵�cookie
	 * �ж��Ƿ���user������ڣ�������ڣ�js��ֱ�ӵ�¼��
	 * ���û��cookie���ڣ������� ��һ�ε�¼���ȸ��ݵ�������Դ���˺Ų�ѯ���ݿ��Ƿ��Ѿ����ڣ�
	 * ��������ڣ�����ȡ������Դ��QQ��΢�ŵȣ����˺š���ǰ�ǳƲ������ݿ��С�
	 * ��ɺ󽫸��û����˺Ŵ浽webnotebookinfo_cookie��Cookie���档
	 * ����û����󷵻ظ����������ÿ��������˻��浽session���档
	 */
	public User login(String id);
	
	/*
	 * ���΢��ƽ̨��¼�Ƿ�ƥ�䡣���������ַ������Ƚ����ǰ����ֵ�˳���������Դ������µ��ַ���
	 * Ȼ��ʹ��shell�����㷨
	 */
	public boolean checkSignature(String signature,String timestamp,String nonce);
    
	/*
	 * ���մ�΢��ǰ̨�����xml��ʽ�����ݣ�������ת���ɼ�������
	 */
	public Map<String,String> xmlToMap(HttpServletRequest request) throws IOException, DocumentException;
	
	/*
	 * ���ı�����ת����xml��ʽ���ַ�����������Ӧ�ͻ��˵�����
	 */
	public String textToXml(TextMessage text,HttpServletRequest request);
	
	/*
	 * ��ȡ�û���Ϣ
	 */
	public void getInfo(HttpServletRequest request);
	
	public void createMenu(String s);
}
