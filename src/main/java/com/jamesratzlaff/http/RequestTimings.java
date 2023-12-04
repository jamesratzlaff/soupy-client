package com.jamesratzlaff.http;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class RequestTimings {
	private final Map<String, List<ApiClientTiming>> timings;

	public RequestTimings() {
		this.timings = new ConcurrentHashMap<String, List<ApiClientTiming>>();
	}

	private synchronized List<ApiClientTiming> getOrAddList(String key) {
		List<ApiClientTiming> existing = this.timings.get(key);
		if (existing == null) {
			existing = new ArrayList<ApiClientTiming>();
			this.timings.put(key, existing);
		}
		return existing;
	}

	public void addTiming(HttpResponse<?> response) {
		String key = response.request().uri().getPath();
		List<ApiClientTiming> timingList = getOrAddList(key);
		timingList.add(new ApiClientTiming(response));
	}

	public Map<String, List<ApiClientTiming>> getTimings() {
		return this.timings;
	}

	@Override
	public String toString() {
		return "RequestTimings [timings=" + timings + "]";
	}

}
