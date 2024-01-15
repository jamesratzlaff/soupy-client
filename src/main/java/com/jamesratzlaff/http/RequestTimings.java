package com.jamesratzlaff.http;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.jamesratzlaff.http.ApiClientTiming.ClientTimingType;


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
		HttpResponse<?> rootMost = getInitialMostResponse(response);
		String key = rootMost.request().uri().getPath();
		List<ApiClientTiming> timingList = getOrAddList(key);
		timingList.add(new ApiClientTiming(response));
	}
	
	private HttpResponse<?> getInitialMostResponse(HttpResponse<?> response){
		HttpResponse<?> resp = response;
		while(resp!=null) {
			HttpResponse<?> prev=response.previousResponse().orElse(null);
			if(prev==null||prev==resp) {
				break;
			}
			resp=prev;
		}
		return resp;
	}

	public Map<String, List<ApiClientTiming>> getTimings() {
		return this.timings;
	}
	private static LongSummaryStatistics getComposite(Map<String,Map<String,List<ApiClientTiming>>> map,ClientTimingType timingType) {
		return map.values()
				.stream()
				.flatMap(vals->vals.entrySet().stream())
				.flatMap(entry->entry.getValue().stream())
				.mapToLong(timing->timingType.applyAsLong(timing))
				.summaryStatistics();
	}
	
	
	
	private static LongSummaryStatistics getComposite(Set<Entry<String,List<ApiClientTiming>>> entries,ClientTimingType timingType) {
		return entries.stream()
				.flatMap(entry->entry.getValue().stream())
				.mapToLong(timing->timingType.applyAsLong(timing))
				.summaryStatistics();
	}
	
	private static Map<ClientTimingType,LongSummaryStatistics> toMap(List<ApiClientTiming> timings, ClientTimingType...types){
		Map<ClientTimingType,LongSummaryStatistics> map = new EnumMap<ApiClientTiming.ClientTimingType, LongSummaryStatistics>(ClientTimingType.class);
		for(ClientTimingType type:types) {
			map.put(type, type.apply(timings));
		}
		return map;
	}
	public Map<String,Map<ClientTimingType,LongSummaryStatistics>> getStatistics(ClientTimingType...types){
		return getStatistics((s)->true, types);
	}
	public Map<String,Map<ClientTimingType,LongSummaryStatistics>> getStatistics(Predicate<String> filter, ClientTimingType...types){
		if(types.length==0) {
			types=ClientTimingType.values();
		}
		if(filter==null) {
			filter=(s)->true;
		}
		Set<String> keys = this.getTimings().keySet().stream().filter(filter).collect(Collectors.toSet());
		Map<String,Map<ClientTimingType,LongSummaryStatistics>> result = new HashMap<String,Map<ClientTimingType,LongSummaryStatistics>>(keys.size());
		for(String key : keys) {
			List<ApiClientTiming> timings = getTimings().get(key);
			result.put(key, toMap(timings, types));
		}
		Set<Entry<String,List<ApiClientTiming>>> all = getTimings().entrySet().stream().filter(entry->keys.contains(entry.getKey())).collect(Collectors.toSet());
		Map<ClientTimingType,LongSummaryStatistics> summary = toMap(all.stream().flatMap(entry->entry.getValue().stream()).collect(Collectors.toList()),types);
		result.put("",summary);
		return result;
	}

	@Override
	public String toString() {
		return "RequestTimings [timings=" + timings + "]";
	}

}
