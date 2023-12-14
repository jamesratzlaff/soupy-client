package com.jamesratzlaff.http;

import com.github.mizosoft.methanol.BodyAdapter;
import com.github.mizosoft.methanol.adapter.ForwardingDecoder;
import com.github.mizosoft.methanol.adapter.ForwardingEncoder;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import com.google.auto.service.AutoService;
import com.google.gson.Gson;

public class GsonAdapters {
	private static final Gson gson = new Gson();

	  @AutoService(BodyAdapter.Encoder.class)
	  public static class Encoder extends ForwardingEncoder {
	    public Encoder() {
	      super(GsonAdapterFactory.createEncoder(gson));
	    }
	  }

	  @AutoService(BodyAdapter.Decoder.class)
	  public static class Decoder extends ForwardingDecoder {
	    public Decoder() {
	      super(GsonAdapterFactory.createDecoder(gson));
	    }
	  }
}
