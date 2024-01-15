package com.jamesratzlaff.http;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.jsoup.Connection.KeyVal;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.FormElement;

import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.HttpCache;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.Methanol.Interceptor;
import com.github.mizosoft.methanol.MoreBodyHandlers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.MutableRequest;
import com.github.mizosoft.methanol.TypeRef;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jamesratzlaff.http.ApiClientTiming.ClientTimingType;

public class SoupyClient extends HttpClient {

	private HttpClient delegate;
	private final CookieManager cookieManager;
	private final String baseURI;
	private final RequestTimings timings;
	private final String[] defHeaders;

	public SoupyClient(String baseURI, String... defHeaders) {
		super();
		this.baseURI = baseURI;
		this.timings = new RequestTimings();
		this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
		this.defHeaders = defHeaders;
		CookieImporterAndExporter.importCookiesDotTxt(this, null);

	}
	
	public void saveCookies(Path p) {
		CookieImporterAndExporter.exportNetscapeCookie(this, p);
	}

	public CookieManager getCookieManager() {
		return this.cookieManager;
	}

	public String getBaseUri() {
		return this.baseURI;
	}

	protected MutableRequest createObjectRequest(String method, String path, String referer, BodyPublisher bp) {
		var request = MutableRequest.create(path);
		if ("GET".equals(method)) {
			bp = null;
			request = request.method(method, null);
		} else if (bp == null) {
			bp = BodyPublishers.noBody();
		}
		request = request.method(method, bp);
		request.header("Referer", referer);
		request.header("Accept", "application/json");
		return request;
	}

	public <T> T getObjectByClass(String method, String path, String referer, BodyPublisher bp, Class<T> clazz) {
		MutableRequest request = createObjectRequest(method, path, referer, bp);
		HttpResponse<T> response;
		T reso = null;
		try {
			response = send(request, MoreBodyHandlers.ofObject(clazz));
//			response = send(request, BodyHandlers.ofString());
			reso = response.body();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return reso;
	}

	public <T> CompletableFuture<T> getObjectByClassAsync(String method, String path, String referer, BodyPublisher bp,
			Class<T> clazz) {
		var request = createObjectRequest(method, path, referer, bp);
		CompletableFuture<HttpResponse<T>> response = sendAsync(request, MoreBodyHandlers.ofObject(clazz));
		CompletableFuture<T> reso = response.thenApply(r -> r.body());
		return reso;
	}

	public <T> CompletableFuture<T> getObjectAsync(String method, String path, String referer, BodyPublisher bp,
			Class<T> clazz) {
		var request = createObjectRequest(method, path, referer, bp);
		CompletableFuture<HttpResponse<T>> response = sendAsync(request, MoreBodyHandlers.ofObject(new TypeRef<T>() {
		}));
		CompletableFuture<T> reso = response.thenApply(r -> r.body());
		return reso;
	}

	public <T> T getObject(String method, String path, String referer, BodyPublisher bp) {
		var request = createObjectRequest(method, path, referer, bp);
		HttpResponse<T> response;
		T reso = null;
		try {
			response = send(request, MoreBodyHandlers.ofObject(new TypeRef<T>() {
			}));
			reso = response.body();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return reso;

	}

	protected Methanol getMethanolClient() {
		if (delegate == null) {
			Methanol.Builder builder = Methanol.newBuilder().cookieHandler(getCookieManager()).baseUri(baseURI)
					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0");
			if (defHeaders.length > 0) {
				builder = builder.defaultHeaders(defHeaders);
			}
			builder = builder.followRedirects(Redirect.NORMAL);
			delegate = builder.build();
		}

		return (Methanol) this.delegate;
	}

	public List<HttpCookie> getCookies() {
		return this.getCookies(null);
	}

	protected HttpCookie makeCookie(String name, String value) {
		HttpCookie cookie = new HttpCookie(name, value);
		cookie.setMaxAge(86400);
		return cookie;

	}

	public HttpCookie getCookie(String name) {
		List<HttpCookie> cookies = getCookies(name);
		if (cookies != null && !cookies.isEmpty()) {
			return cookies.get(0);
		}
		return null;
	}

	public List<HttpCookie> getCookies(String name) {
		if (name == null) {
			return getCookieManager().getCookieStore().getCookies();
		}
		List<HttpCookie> cookies = getCookieManager().getCookieStore().getCookies().stream()
				.filter(c -> c.getName().equalsIgnoreCase(name)).collect(Collectors.toList());
		return cookies;
	}

	public void addAllCookies(Collection<HttpCookie> cookies) {
		if (cookies == null) {
			cookies = Collections.emptyList();
		}
		cookies.forEach(cookie -> addCookie(cookie));
	}

	public String getCookiesAsJsonString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this.getCookies().stream().map(CookieImporterAndExporter.SerializableCookie::new)
				.collect(Collectors.toList()));
	}

	public void addCookie(HttpCookie cookie) {
		if (cookie != null) {
			getCookieManager().getCookieStore().add(null, cookie);
		}
	}

	public int hashCode() {
		return getMethanolClient().hashCode();
	}

	public boolean equals(Object obj) {
		return getMethanolClient().equals(obj);
	}

	public <T> Publisher<HttpResponse<T>> exchange(HttpRequest request, BodyHandler<T> bodyHandler) {
		return getMethanolClient().exchange(request, bodyHandler);
	}

	public <T> Publisher<HttpResponse<T>> exchange(HttpRequest request, BodyHandler<T> bodyHandler,
			Function<HttpRequest, BodyHandler<T>> pushPromiseAcceptor) {
		return getMethanolClient().exchange(request, bodyHandler, pushPromiseAcceptor);
	}

	public HttpClient underlyingClient() {
		return getMethanolClient().underlyingClient();
	}

	public Optional<String> userAgent() {
		return getMethanolClient().userAgent();
	}

	public Optional<URI> baseUri() {
		return getMethanolClient().baseUri();
	}

	public Optional<Duration> requestTimeout() {
		return getMethanolClient().requestTimeout();
	}

	public Optional<Duration> headersTimeout() {
		return getMethanolClient().headersTimeout();
	}

	public Optional<Duration> readTimeout() {
		return getMethanolClient().readTimeout();
	}

	public List<Interceptor> interceptors() {
		return getMethanolClient().interceptors();
	}

	public List<Interceptor> backendInterceptors() {
		return getMethanolClient().backendInterceptors();
	}

	public String toString() {
		return getMethanolClient().toString();
	}

	public HttpHeaders defaultHeaders() {
		return getMethanolClient().defaultHeaders();
	}

	public boolean autoAcceptEncoding() {
		return getMethanolClient().autoAcceptEncoding();
	}

	public Optional<HttpCache> cache() {
		return getMethanolClient().cache();
	}

	public Optional<CookieHandler> cookieHandler() {
		return getMethanolClient().cookieHandler();
	}

	public Optional<Duration> connectTimeout() {
		return getMethanolClient().connectTimeout();
	}

	public Redirect followRedirects() {
		return getMethanolClient().followRedirects();
	}

	public Optional<ProxySelector> proxy() {
		return getMethanolClient().proxy();
	}

	public SSLContext sslContext() {
		return getMethanolClient().sslContext();
	}

	public SSLParameters sslParameters() {
		return getMethanolClient().sslParameters();
	}

	public Optional<Authenticator> authenticator() {
		return getMethanolClient().authenticator();
	}

	public Version version() {
		return getMethanolClient().version();
	}

	public Optional<Executor> executor() {
		return getMethanolClient().executor();
	}

	public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> bodyHandler)
			throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		HttpResponse<T> response = getMethanolClient().send(request, bodyHandler);
		response = new TimedHttpResponseWrapper<T>(response, startTime, System.currentTimeMillis());
		timings.addTiming(response);
		return response;
	}

	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> bodyHandler) {
		long startTime = System.currentTimeMillis();
		CompletableFuture<HttpResponse<T>> result = getMethanolClient().sendAsync(request, bodyHandler);
		result.whenComplete((r, t) -> {
			timings.addTiming(new TimedHttpResponseWrapper<T>(r, startTime));
		});
		return result;
	}

	public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> bodyHandler,
			PushPromiseHandler<T> pushPromiseHandler) {
		long startTime = System.currentTimeMillis();
		CompletableFuture<HttpResponse<T>> result = getMethanolClient().sendAsync(request, bodyHandler,
				pushPromiseHandler);
		result.whenComplete((r, t) -> {
			timings.addTiming(new TimedHttpResponseWrapper<T>(r, startTime));
		});
		return result;
	}

	public java.net.http.WebSocket.Builder newWebSocketBuilder() {
		return getMethanolClient().newWebSocketBuilder();
	}

	public void shutdown() {
		getMethanolClient().shutdown();
	}

	public boolean awaitTermination(Duration duration) throws InterruptedException {
		return getMethanolClient().awaitTermination(duration);
	}

	public boolean isTerminated() {
		return getMethanolClient().isTerminated();
	}

	public void shutdownNow() {
		getMethanolClient().shutdownNow();
	}

	public void close() {
		getMethanolClient().close();
	}

	public RequestTimings getTimings() {
		return timings;
	}

	public Map<String, Map<ClientTimingType, LongSummaryStatistics>> getTimingStats(ClientTimingType... types) {
		return getTimings().getStatistics(types);
	}

	public Map<String, Map<ClientTimingType, LongSummaryStatistics>> getTimingStats(Predicate<String> keyPredicate,
			ClientTimingType... types) {
		return getTimings().getStatistics(keyPredicate, types);
	}

	protected HttpResponse<String> getHttpResponse(String path) {
		return getHttpResponse(path, null);
	}

	protected HttpResponse<String> getHttpResponse(String path, String referer) {
		HttpResponse<String> response = null;
		try {
			HttpRequest req = createHttpGetRequest(path, referer);
			response = send(req, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}

	protected CompletableFuture<HttpResponse<String>> getHttpResponseAsync(String path) {
		return getHttpResponseAsync(path, null);
	}

	protected CompletableFuture<HttpResponse<String>> getHttpResponseAsync(String path, String referer) {
		CompletableFuture<HttpResponse<String>> response = null;
		HttpRequest req = createHttpGetRequest(path, referer);
		response = sendAsync(req, BodyHandlers.ofString());
		return response;
	}

	protected HttpRequest createHttpGetRequest(String path, String referer) {
		MutableRequest req = MutableRequest.GET(path);
		if (referer != null) {
			req = req.header("Referer", referer);
		}
		return req;
	}

	public Document getDocumentWithBase64EncLastPathNode(String path, String referer) {
		path = createBase64EncLastPathNodeStr(path, referer);
		return getDocument(path, referer);
	}

	public CompletableFuture<Document> getDocumentWithBase64EncLastPathNodeAsync(String path, String referer) {
		path = createBase64EncLastPathNodeStr(path, referer);
		return getDocumentAsync(path, referer);
	}

	private static String createBase64EncLastPathNodeStr(String path, String referer) {
		if (referer != null) {
			if (!path.endsWith("/")) {
				path += "/";
			}
			String encodedLastNode = Base64.getUrlEncoder().encodeToString(referer.getBytes());
			path += encodedLastNode;
		}
		return path;
	}

	public Document getDocument(String path) {
		return getDocument(path, null);
	}

	public Document getDocument(String path, String referer) {
		HttpResponse<String> resp = getHttpResponse(path, referer);
		Document d = null;
		if (resp != null) {
			d = Jsoup.parse(resp.body(), resp.uri().toString());
		}
		return d;
	}

	public CompletableFuture<Document> getDocumentAsync(String path, String referer) {
		CompletableFuture<HttpResponse<String>> resp = getHttpResponseAsync(path, referer);
		CompletableFuture<Document> cDoc = resp.thenApply(r -> {
			if (r != null) {
				return Jsoup.parse(r.body(), r.uri().toString());
			}
			return null;
		});
		return cDoc;
	}

	public static class FormConverter {
		public static HttpRequest convertFormUsingQueryToRequest(Document d, String query, String... kvs) {
			FormElement fe = (FormElement) d.selectFirst(query);
			return convertFormToRequest(fe, kvs);
		}

		public static HttpRequest convertFormToRequest(Document d, String formId, String... kvs) {
			FormElement fe = (FormElement) d.getElementById(formId);
			return convertFormToRequest(fe, kvs);
		}

		public static HttpRequest convertFormToRequest(FormElement fe) {
			return convertFormToRequest(fe, (Map<String, String>) null);
		}

		public static HttpRequest convertFormToRequest(FormElement fe, String... kvs) {
			return convertFormToRequest(fe, toEntries(kvs));
		}

		public static HttpRequest convertFormToRequestMulti(FormElement fe, Map<String, List<String>> values) {
			List<KeyVal> mergedVals = mergeKeyValsForReal(fe.formData(), values);
			return convertFormToRequestWithMergedVals(fe, mergedVals);

		}

		public static HttpRequest convertFormToRequest(FormElement fe, Map<String, String> values) {
			List<KeyVal> mergedVals = mergeKeyVals(fe.formData(), values);
			return convertFormToRequestWithMergedVals(fe, mergedVals);

		}

		public static HttpRequest convertFormToRequest(FormElement fe, List<Entry<String, String>> values) {
			List<KeyVal> mergedVals = mergeKeyVals(fe.formData(), values);
			return convertFormToRequestWithMergedVals(fe, mergedVals);

		}

		private static HttpRequest convertFormToRequestWithMergedVals(FormElement fe, List<KeyVal> mergedVals) {
			String loc = fe.ownerDocument().location();
			if (loc == null) {
				loc = fe.baseUri();
			}
			String action = fe.absUrl("action");
			if (action == null) {
				action = loc;
			}
			String method = fe.attr("method");
			if (method == null) {
				method = "GET";
			}
			method = method.toUpperCase();
			BodyPublisher bp = getBodyPublisher(fe, mergedVals);
			if (Objects.equals("GET", method) && (bp instanceof FormBodyPublisher)) {
				action += "?" + ((FormBodyPublisher) bp).encodedString();
				bp = BodyPublishers.noBody();
			}
			HttpRequest.Builder builder = HttpRequest.newBuilder();
			builder = builder.uri(URI.create(action));
			builder = builder.method(method, bp);
			builder = builder.header("Referer", loc);
			return builder.build();
		}

		private static List<Entry<String, String>> toEntries(String[] strs) {
			int len = strs.length >>> 1;
			boolean isOdd = (strs.length & 1) == 1;
			if (isOdd) {
				len += 1;
			}
			List<Entry<String, String>> result = new ArrayList<Entry<String, String>>(len);
			for (int i = 0; i < strs.length; i += 2) {
				String key = strs[i];
				String value = null;
				if (i + 1 < strs.length) {
					value = strs[i + 1];
				}
				Entry<String, String> e = Map.entry(key, value);
				result.add(e);
			}
			return result;
		}

		private static BodyPublisher getBodyPublisher(FormElement fe, List<KeyVal> keyVals) {
			String encType = fe.attr("enctype");
			boolean isMultipart = false;
			if (encType != null) {
				String multipart = "multipart";
				if (encType.length() >= multipart.length()) {
					String beginning = encType.substring(0, multipart.length());
					if (beginning.toLowerCase().equals(multipart)) {
						isMultipart = true;
					}
				}
			}
			BodyPublisher bp = null;
			if (isMultipart) {
				bp = convertFormValuesToMultipartBodyPublisher(keyVals);
			} else {
				bp = convertFormValuesToFormBodyPublisher(keyVals);
			}
			return bp;
		}

		private static FormBodyPublisher convertFormValuesToFormBodyPublisher(List<KeyVal> keyVals) {
			FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();
			for (KeyVal kv : keyVals) {
				builder = builder.query(kv.key(), kv.value());
			}
			return builder.build();
		}

		private static MultipartBodyPublisher convertFormValuesToMultipartBodyPublisher(List<KeyVal> keyVals) {
			MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();
			for (KeyVal kv : keyVals) {
				builder = builder.textPart(kv.key(), kv.value());
			}
			return builder.build();
		}

		@SafeVarargs
		private static List<KeyVal> mergeKeyVals(List<KeyVal> keyVals, Entry<String, String>... entries) {
			return mergeKeyVals(keyVals, new ArrayList<Entry<String, String>>(List.of(entries)));
		}

		private static List<KeyVal> mergeKeyVals(List<KeyVal> keyVals, List<Entry<String, String>> entries) {
			if (entries == null) {
				return keyVals;
			}
			Map<String, ArrayList<String>> remapped = new LinkedHashMap<String, ArrayList<String>>();
			Collections.reverse(entries);
			for (Entry<String, String> e : entries) {
				String key = e.getKey();
				ArrayList<String> values = remapped.get(key);
				if (values == null) {
					values = new ArrayList<String>();
					remapped.put(key, values);
				}
				values.add(e.getValue());

			}
			remapped.values().forEach(v -> v.trimToSize());
			return mergeKeyValsForReal(keyVals, remapped);
		}

		private static List<KeyVal> mergeKeyVals(List<KeyVal> keyVals, Map<String, String> merge) {
			if (merge == null) {
				return keyVals;
			}
			Map<String, List<String>> remapped = new LinkedHashMap<String, List<String>>(merge.size());
			merge.entrySet().forEach(entry -> {
				List<String> list = new ArrayList<String>(1);
				list.add(entry.getValue());
				remapped.put(entry.getKey(), list);
			});
			return mergeKeyValsForReal(keyVals, remapped);
		}

		private static final Class<?> immutableMapClass = Map.of().getClass();
		private static final Class<?> unmodifiableMapClass = Collections.unmodifiableMap(new HashMap<String, String>(0))
				.getClass();

		private static List<KeyVal> mergeKeyValsForReal(List<KeyVal> keyVals,
				Map<String, ? extends List<String>> merge) {
			if (merge == null) {
				return keyVals;
			}
			Map<String, ? extends List<String>> mutable = (immutableMapClass == merge.getClass()
					|| unmodifiableMapClass == merge.getClass()) ? new LinkedHashMap<String, List<String>>(merge)
							: merge;
			for (KeyVal kv : keyVals) {
				String key = kv.key();
				if (mutable.containsKey(key)) {
					List<String> values = mutable.get(key);
					String val = values.remove(0);
					if (values.isEmpty()) {
						mutable.remove(key);
					}
					if (val != null) {
						kv.value(val);
					}
				}
			}
			mutable.entrySet().forEach(entry -> {
				List<String> values = entry.getValue();
				for (String value : values) {
					keyVals.add(HttpConnection.KeyVal.create(entry.getKey(), value));
				}
			});
			return keyVals;
		}
	}

}
