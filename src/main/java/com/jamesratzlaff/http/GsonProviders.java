package com.jamesratzlaff.http;

import com.github.mizosoft.methanol.BodyAdapter;
import com.github.mizosoft.methanol.adapter.gson.GsonAdapterFactory;
import com.google.gson.Gson;

public class GsonProviders {
		  private static final Gson gson = new Gson();

		  public static class EncoderProvider {
		    public static BodyAdapter.Encoder provider() {
		      return GsonAdapterFactory.createEncoder(gson);
		    }
		  }

		  public static class DecoderProvider {
		    public static BodyAdapter.Decoder provider() {
		      return GsonAdapterFactory.createDecoder(gson);
		    }
		  }
}
