package org.bdawg.open_audio.http_utils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.net.MediaType;

public abstract class HttpUtils {
	public static HttpResponse executeGet(String URI)
			throws ClientProtocolException, IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(URI);
		return httpclient.execute(httpGet);

	}

	public static HttpResponse executePost(String URI, Object toPost)
			throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(URI);
		ObjectMapper mapper = new ObjectMapper();
		StringEntity entity = new StringEntity(
				mapper.writeValueAsString(toPost));
		entity.setContentType(MediaType.JSON_UTF_8.toString());
		httpPost.setEntity(entity);
		return httpclient.execute(httpPost);
	}
}
