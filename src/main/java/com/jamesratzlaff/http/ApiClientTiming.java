package com.jamesratzlaff.http;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.Date;


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
	
	

}
