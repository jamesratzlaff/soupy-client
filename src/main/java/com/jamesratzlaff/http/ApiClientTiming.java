package com.jamesratzlaff.http;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LongSummaryStatistics;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;


public record ApiClientTiming(long sent, long received, long serverDate) {
	public static final String SENT_HEADER = "OkHttp-Sent-Millis";
	public static final String RECV_HEADER = "OkHttp-Received-Millis";

	public ApiClientTiming(HttpResponse<?> response) {
		this(response != null ? response.headers() : null);
	}

	public ApiClientTiming(HttpHeaders headers) {
		this(headers != null ? headers.firstValue(SENT_HEADER).orElse(null) : null, headers != null ? headers.firstValue(RECV_HEADER).orElse(null) : null,
				headers != null ? headers.firstValue("Date").orElse(null) : null);
	}

	public ApiClientTiming(String sent, String revd, String serverDate) {
		this(retNeg1IfNull(sent), retNeg1IfNull(revd), serverDate != null ? Date.parse(serverDate) : -1);
	}

	private static long retNeg1IfNull(String str) {
		try {
			return Long.parseLong(str);
		} catch (NullPointerException | NumberFormatException nfe) {
			return -1;
		}
	}

	public long getRoundTrip() {
		return this.received() - this.sent();
	}

	public long getClientToServerTime() {
		return this.serverDate() - this.sent();
	}

	public long getServerToClientTime() {
		return this.received() - this.serverDate();
	}

	@Override
	public String toString() {
		return "ApiClientTiming [sent=" + sent + ", received=" + received + ", serverDate=" + serverDate
				+ ", roundTrip=" + getRoundTrip() + ", clientToServerTime=" + getClientToServerTime()
				+ ", serverToClientTime=" + getServerToClientTime() + "]";
	}
	
	public static enum ClientTimingType implements ToLongFunction<ApiClientTiming>, Comparator<ApiClientTiming> {
		SENT(ApiClientTiming::sent),RECEIVED(ApiClientTiming::received),SERVER_DATE(ApiClientTiming::serverDate),ROUND_TRIP(ApiClientTiming::getRoundTrip),CLIENT_TO_SERVER(ApiClientTiming::getClientToServerTime),SERVER_TO_CLIENT(ApiClientTiming::getServerToClientTime);
		private final ToLongFunction<ApiClientTiming> func;
		private ClientTimingType(ToLongFunction<ApiClientTiming> func) {
			this.func=func;
		}
		public long applyAsLong(ApiClientTiming timing) {
			return this.func.applyAsLong(timing);
		}
		public LongSummaryStatistics flatApply(Collection<? extends Collection<ApiClientTiming>> coll) {
			return apply(coll.stream().flatMap(e->e.stream()));
		}
		
		public LongSummaryStatistics apply(Collection<ApiClientTiming> coll) {
			return apply(coll.stream());
		}
		
		public LongSummaryStatistics apply(Stream<ApiClientTiming> ls) {
			return ls.mapToLong(this::applyAsLong).summaryStatistics();
		}
		@Override
		public int compare(ApiClientTiming o1, ApiClientTiming o2) {
			long o1Val = this.applyAsLong(o1);
			long o2Val = this.applyAsLong(o2);
			return Long.compare(o1Val, o2Val);
		}
		
		
		
	}
	
	

}
