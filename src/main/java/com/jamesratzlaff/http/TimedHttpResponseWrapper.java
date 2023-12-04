package com.jamesratzlaff.http;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLSession;

public class TimedHttpResponseWrapper<T> implements HttpResponse<T>{

	private final HttpResponse<T> delegate;
	private final HttpHeaders modifiedHeaders;
	public TimedHttpResponseWrapper(HttpResponse<T> response, long startTime) {
		this(response, startTime, System.currentTimeMillis());
	}
	public TimedHttpResponseWrapper(HttpResponse<T> response, long startTime, long endTime) {
		this.delegate=response;
		Map<String,List<String>> headersMap = new HashMap<String,List<String>>(this.delegate.headers().map());
		headersMap.put(ApiClientTiming.SENT_HEADER, List.of(Long.toString(startTime)));
		headersMap.put(ApiClientTiming.RECV_HEADER, List.of(Long.toString(endTime)));
		this.modifiedHeaders=HttpHeaders.of(headersMap, (a,b)->true);
	}
	public int statusCode() {
		return delegate.statusCode();
	}
	public HttpRequest request() {
		return delegate.request();
	}
	public Optional<HttpResponse<T>> previousResponse() {
		return delegate.previousResponse();
	}
	public HttpHeaders headers() {
		return modifiedHeaders;
	}
	public T body() {
		return delegate.body();
	}
	public Optional<SSLSession> sslSession() {
		return delegate.sslSession();
	}
	public URI uri() {
		return delegate.uri();
	}
	public Version version() {
		return delegate.version();
	}
	public HttpResponse<T> getDelegate() {
		return delegate;
	}
	@Override
	public String toString() {
		return delegate.toString();
	}
	
	
}
