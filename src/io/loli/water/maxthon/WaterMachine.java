package io.loli.water.maxthon;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class WaterMachine {
	private static final HttpClient client = new DefaultHttpClient();
	private String site;

	WaterMachine(String site) {
		if (!site.endsWith("/")) {
			site += "/";
		}
		this.site = site;
		// HttpProtocolParams.setUserAgent(client.getParams(),
		// "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.52 Safari/537.36");
	}

	/**
	 * 获取get请求后的页面源码
	 * 
	 * @param url
	 *            需要获取源码的网址
	 * @return 该url的源码(html)
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	private String getHtml(String url) {
		HttpGet httpget = new HttpGet(url);
		HttpResponse response = null;
		HttpEntity entity = null;
		String html = "";
		try {
			response = client.execute(httpget);

			entity = response.getEntity();

			html = EntityUtils.toString(entity);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (entity != null)
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return html;
	}

	/**
	 * 获取post请求后的页面源码
	 * 
	 * @param url
	 *            需要获取源码的网址
	 * @param params
	 *            该url的源码(html)
	 * @return 该url的源码(html)
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@SuppressWarnings("deprecation")
	private String post(String url, List<NameValuePair> params) {
		HttpPost httpPost = new HttpPost(url);
		HttpResponse response = null;
		HttpEntity entity = null;
		String html = null;
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			response = client.execute(httpPost);
			entity = response.getEntity();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (entity != null) {
				try {
					html = EntityUtils.toString(entity);
				} catch (ParseException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return html;
	}

	/**
	 * 根据正则搜索指定字符串
	 * 
	 * @param html
	 *            在此html中寻找
	 * @param regex
	 *            正则
	 * @return
	 */
	private String findString(String html, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(html);
		if (m.find()) {
			return m.group(1);
		} else {
			return "";
		}
	}

	/**
	 * 登陆
	 * 
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @return 是否登陆成功
	 */
	public boolean login(String username, String password) {
		// 登陆地址
		String suffix = "member.php?mod=logging&action=login&mobile=yes";
		String loginUrl = site + suffix;
		// 登陆地址的html
		String loginHtml = getHtml(loginUrl);
		// 登陆表单的action地址, 需要替换&
		String loginPostUrl = findString(loginHtml,
				"method=\"post\" action=\"(.*)\" onsubmit").replaceAll("&amp;",
				"&");
		// 登陆hash
		String loginHash = findString(loginHtml,
				"name=\"formhash\" value=\"(.*)\"");
		// 登陆参数
		List<NameValuePair> loginParams = new ArrayList<NameValuePair>();
		NameValuePair param1 = new BasicNameValuePair("formhash", loginHash);
		NameValuePair param2 = new BasicNameValuePair("username", username);
		NameValuePair param3 = new BasicNameValuePair("password", password);
		loginParams.add(param1);
		loginParams.add(param2);
		loginParams.add(param3);
		String result = post(site + loginPostUrl, loginParams);
		return result.contains("欢迎");
	}

	/**
	 * 获取post的action地址并生成提交参数
	 * 
	 * @param threadUrl
	 *            需要回复帖子的地址
	 * @param content
	 *            回复内容
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List generateReplyUrlAndParam(String threadUrl, String content) {
		// 帖子的html
		String threadHtml = getHtml(threadUrl);
		// formhash
		String formHash = findString(threadHtml,
				"input type=\"hidden\" name=\"formhash\" value=\"(.*)\"");
		// 回复表单的action地址
		String postUrl = findString(threadHtml,
				"id=\"fastpostform\" action=\"(.*)\"").replaceAll("&amp;", "&");
		// 回复参数
		List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		NameValuePair posthashParam = new BasicNameValuePair("formhash",
				formHash);
		NameValuePair message = new BasicNameValuePair("message", content);
		postParams.add(posthashParam);
		postParams.add(message);
		List list = new ArrayList();
		list.add(site + postUrl);
		list.add(postParams);
		return list;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void reply(List list) {
		post((String) list.get(0), (List<NameValuePair>) list.get(1));
	}

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
		WaterMachine wm = new WaterMachine("http://bbs.maxthon.cn");
		if (wm.login("username", "password")) {
			System.out.println("登陆成功");
			String threadUrl = "http://bbs.maxthon.cn/thread-848187-1-1.html";
			List params = wm.generateReplyUrlAndParam(threadUrl, "某些coser欠x");
			for (int i = 0; i < 40; i++) {
				wm.reply(params);

				int time = (int) (Math.random() * 30 + 10);
				System.out.println("第" + i + "次灌水, 等待" + time + "秒");
				try {
					Thread.sleep(time * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("登陆失败");
		}
	}
}
