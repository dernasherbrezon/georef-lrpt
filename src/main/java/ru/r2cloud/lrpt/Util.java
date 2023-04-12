package ru.r2cloud.lrpt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	
	private static final Logger LOG = LoggerFactory.getLogger(Util.class);
	private static final Pattern COMMA = Pattern.compile(",");
	
	public static List<String> splitComma(String str) {
		String[] values = COMMA.split(str);
		List<String> result = new ArrayList<>();
		for (String cur : values) {
			cur = cur.trim();
			if (cur.length() == 0) {
				continue;
			}
			result.add(cur);
		}
		return result;
	}
	
	public static CloseableHttpClient createClient() {
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(params, 10000);
		HttpConnectionParams.setConnectionTimeout(params, 10000);
		HttpProtocolParams.setUserAgent(params, "r2cloud.ru");
		HttpProtocolParams.setContentCharset(params, "utf-8");

		DefaultHttpClient client = new DefaultHttpClient(params);
		client.addRequestInterceptor(new RequestAcceptEncoding());
		client.addResponseInterceptor(new ResponseContentEncoding());
		return client;
	}
	
	public static void toLog(Logger log, InputStream is) throws IOException {
		try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
			String curLine = null;
			while ((curLine = in.readLine()) != null) {
				log.info(curLine);
			}
		}
	}
	
	public static Date convertToMidnightMoscow(Date gmtVcdu) {
		Calendar vcduDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		vcduDate.setTime(gmtVcdu);

		Calendar result = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
		result.set(Calendar.YEAR, vcduDate.get(Calendar.YEAR));
		result.set(Calendar.MONTH, vcduDate.get(Calendar.MONTH));
		result.set(Calendar.DAY_OF_MONTH, vcduDate.get(Calendar.DAY_OF_MONTH));
		result.set(Calendar.HOUR_OF_DAY, 0);
		result.set(Calendar.MINUTE, 0);
		result.set(Calendar.SECOND, 0);
		result.set(Calendar.MILLISECOND, 0);
		return result.getTime();
	}
	
	public static boolean deleteDirectory(Path f) {
		if (Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> entries = Files.newDirectoryStream(f)) {
				for (Path entry : entries) {
					boolean curResult = deleteDirectory(entry);
					if (!curResult) {
						return curResult;
					}
				}
			} catch (IOException e) {
				LOG.error("unable to delete: " + f.toAbsolutePath(), e);
				return false;
			}
		}
		try {
			Files.delete(f);
			return true;
		} catch (IOException e) {
			LOG.error("unable to delete: " + f.toAbsolutePath(), e);
			return false;
		}
	}
}
