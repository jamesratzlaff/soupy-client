package com.jamesratzlaff.http;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import com.github.mizosoft.methanol.HttpCache;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.Methanol.Interceptor;

public class SoupyClient extends HttpClient {

	private HttpClient delegate;
	private final String baseURI;
	private final RequestTimings timings;

	public SoupyClient(String baseURI) {
		super();
		this.baseURI = baseURI;
		this.timings = new RequestTimings();
	}

	protected Methanol getMethanolClient() {
		if (delegate == null) {
			delegate = Methanol.newBuilder().cookieHandler(CookieHandler.getDefault()).baseUri(baseURI).build();
		}
		return (Methanol) this.delegate;
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

}
