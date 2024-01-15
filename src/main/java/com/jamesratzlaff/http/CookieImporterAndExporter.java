package com.jamesratzlaff.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CookieImporterAndExporter {

	public static final Path DEFAULT_PATH = Paths.get(System.getProperty("user.home"), "Downloads","cookies.txt");
	
	public static List<HttpCookie> importCookiesDotTxt(Path pathToCookies) {
		return importNetscapeCookies(pathToCookies).stream().map(NetscapeCookie::toHttpCookie).collect(Collectors.toList());
	}

	public static void importCookiesDotTxt(HttpClient client, Path pathToCookies) {
		List<HttpCookie> cookies = importCookiesDotTxt(pathToCookies);
		CookieStore cs = getCookieStoreFromClient(client);
		if(cs!=null) {
			cookies.forEach(cookie->cs.add(null, cookie));
		}
	}
	
	private static CookieManager getCookieManagerFromClient(HttpClient client) {
		CookieManager cm = null;
		if(client!=null) {
			CookieHandler ch = client.cookieHandler().orElse(null);
			if(ch!=null) {
				if(ch instanceof CookieManager) {
					cm=(CookieManager)ch;
				}
			}
		}
		return cm;
	}
	
	private static CookieStore getCookieStoreFromClient(HttpClient client) {
		CookieStore cs = null;
		CookieManager cm = getCookieManagerFromClient(client);
		if(cm!=null) {
			cs=cm.getCookieStore();
		}
		return cs;
		
	}
	public static synchronized void exportNetscapeCookie(HttpClient client, Path p) {
		if(client!=null) {
			CookieStore cs = getCookieStoreFromClient(client);
			if(cs!=null) {
				List<HttpCookie> httpCookies = new ArrayList<HttpCookie>(cs.getCookies());
				List<NetscapeCookie> nsCookies = new ArrayList<NetscapeCookie>(httpCookies.size());
				for(HttpCookie cookie : httpCookies) {
					NetscapeCookie ns = new NetscapeCookie(cookie);
					nsCookies.add(ns);
				}
				exportNetscapeCookies(nsCookies, p);
			}
		}
	}
	
	
	private static final String[] netscapeCookieHeaders = {
			"# Netscape HTTP Cookie File",
			"# https://curl.haxx.se/rfc/cookie_spec.html",
			"# This is a generated file! Do not edit."	
	};
	private static String createGeneratedTimestampLine() {
		return "# Generated at "+(System.currentTimeMillis()/1000);
	}
	public synchronized static void exportNetscapeCookies(Collection<NetscapeCookie> cookies, Path p) {
		if(p==null) {
			p=DEFAULT_PATH;
		}
		try(BufferedWriter bw = Files.newBufferedWriter(p, StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){
			for(String cookieHeader : netscapeCookieHeaders) {
				bw.write(cookieHeader);
				bw.newLine();
			}
			bw.write(createGeneratedTimestampLine());
			bw.newLine();
			for(NetscapeCookie cookie : cookies) {
				bw.newLine();
				String asString = NetscapeCookie.Part.toCookiesDotTxtString(cookie);
				bw.write(asString);
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
	}
	
	

	public static List<NetscapeCookie> importNetscapeCookies(Path p) {
		if(p==null) {
			p=DEFAULT_PATH;
		}
		try (Stream<String> lines = Files.lines(p)) {
			FileTime ft = Files.getLastModifiedTime(p);
			return lines.filter(line -> !(line.isBlank() || line.startsWith("#"))).map(NetscapeCookie.Part::create).collect(Collectors.toList());
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return Collections.emptyList();
	}

	public static class NetscapeCookie {
		private String domain;
		private boolean includeSubdomains;
		private String path;
		private boolean httpsOnly;
		private long expirySeconds;
		private String name;
		private String value;
		private LongSupplier cookieTime;
		public NetscapeCookie() {
			this((LongSupplier)null);
		}
		public NetscapeCookie(long val) {
			this(()->val);
		}
		public NetscapeCookie(LongSupplier cookieTime) {
			if(cookieTime==null) {
				cookieTime=()->System.currentTimeMillis()/1000;
			}
			this.cookieTime=cookieTime;
		}
		
		public NetscapeCookie(HttpCookie cookie) {
			this();
			if(cookie!=null) {
				fromHttpCookie(cookie);
			}
		}

		public void fromHttpCookie(HttpCookie cookie) {
			this.setName(cookie.getName());
			this.setValue(cookie.getValue());
			this.setDomain(cookie.getDomain());
			this.setIncludeSubdomains(cookie.getDomain().startsWith("."));
			this.setExpirySeconds((getCookieCreationTime(cookie)/1000)+cookie.getMaxAge());
			this.setHttpsOnly(cookie.getSecure());
			this.setPath(cookie.getPath());
		}
		
		public HttpCookie toHttpCookie() {
			HttpCookie result = new HttpCookie(getName(), getValue());
			result.setHttpOnly(false);
			result.setPath(getPath());
			result.setDomain(getDomain());
			result.setMaxAge(getExpirySeconds()-(Instant.now().getEpochSecond()));
			result.setSecure(isHttpsOnly());
			result.setVersion(0);
			return result;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public boolean isIncludeSubdomains() {
			return includeSubdomains;
		}

		public void setIncludeSubdomain(String includeSubdomains) {
			setIncludeSubdomains(Boolean.parseBoolean(includeSubdomains));
		}

		public void setIncludeSubdomains(boolean includeSubdomains) {
			this.includeSubdomains = includeSubdomains;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public boolean isHttpsOnly() {
			return httpsOnly;
		}

		public void setHttpsOnly(boolean httpsOnly) {
			this.httpsOnly = httpsOnly;
		}

		public void setHttpsOnly(String httpsOnly) {
			this.setHttpsOnly(Boolean.parseBoolean(httpsOnly));
		}

		public long getExpirySeconds() {
			return expirySeconds;
		}

		public void setExpirySeconds(String expirySeconds) {
			this.setExpirySeconds(Long.parseLong(expirySeconds));
		}

		public void setExpirySeconds(long expirySeconds) {
			this.expirySeconds = expirySeconds;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		public static enum Part implements BiConsumer<NetscapeCookie, String> {
			domain(NetscapeCookie::setDomain, NetscapeCookie::getDomain),
			includeSubdomains(NetscapeCookie::setIncludeSubdomain,
					toStringFunction(NetscapeCookie::isIncludeSubdomains)),
			path(NetscapeCookie::setPath, NetscapeCookie::getPath),
			httpsOnly(NetscapeCookie::setHttpsOnly, toStringFunction(NetscapeCookie::isHttpsOnly)),
			expirySeconds(NetscapeCookie::setExpirySeconds, toStringFunction(NetscapeCookie::getExpirySeconds)),
			name(NetscapeCookie::setName, NetscapeCookie::getName),
			value(NetscapeCookie::setValue, NetscapeCookie::getValue);

			private static final Function<NetscapeCookie, String> toStringFunction(Function<NetscapeCookie, ?> func) {
				return cookie -> {
					Object val = func.apply(cookie);
					return String.valueOf(val).toUpperCase();
				};
			}

			private final BiConsumer<NetscapeCookie, String> biConsumer;
			private final Function<NetscapeCookie, String> toStringFunction;

			private Part(BiConsumer<NetscapeCookie, String> biConsumer,
					Function<NetscapeCookie, String> toStringFunction) {
				this.biConsumer = biConsumer;
				this.toStringFunction = toStringFunction;
			}

			public void accept(NetscapeCookie t, String[] u) {
				
				this.accept(t, u[this.ordinal()]);
			}

			@Override
			public void accept(NetscapeCookie t, String u) {
				this.biConsumer.accept(t, u);
			}
			public static NetscapeCookie create(String line) {
				return create(line,Instant.now().getEpochSecond());
			}
			public static NetscapeCookie create(String line, long fileTimeSeconds) {
				return setAll(null, line, fileTimeSeconds);
			}

			public static NetscapeCookie setAll(NetscapeCookie cookie, String line, long fileTimeSeconds) {
				return setAll(cookie, line.split("[\t]"),fileTimeSeconds);
			}

			public static NetscapeCookie setAll(NetscapeCookie cookie, String[] parts, long fileTimeSeconds) {
				
				cookie = cookie != null ? cookie : new NetscapeCookie(fileTimeSeconds);
				String[] paddedParts = new String[Part.values().length];
				Arrays.fill(paddedParts, "");
				for(int i=0;i<parts.length;i++) {
					paddedParts[i]=parts[i];
				}
				parts=paddedParts;
				for (Part p : Part.values()) {
					p.accept(cookie, parts);
				}
				return cookie;
			}
			
			public static String toCookiesDotTxtString(NetscapeCookie cookie) {
				List<String> parts = new ArrayList<String>(Part.values().length);
				for(Part p:Part.values()) {
					parts.add(p.toStringFunction.apply(cookie));
				}
				
				return String.join("\t", parts);
			}

		}

	}

	public static class SerializableCookie implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4911010299428965037L;
		private String name; // NAME= ... "$Name" style is reserved
		private String value; // value of NAME
		private String comment; // Comment=VALUE ... describes cookie's use
		private String commentURL; // CommentURL="http URL" ... describes cookie's use
		private boolean toDiscard; // Discard ... discard cookie unconditionally
		private String domain; // Domain=VALUE ... domain that sees cookie
		private long maxAge = -1; // Max-Age=VALUE ... cookies auto-expire
		private String path; // Path=VALUE ... URLs that see the cookie
		private String portlist; // Port[="portlist"] ... the port cookie may be returned to
		private boolean secure; // Secure ... e.g. use SSL
		private boolean httpOnly; // HttpOnly ... i.e. not accessible to scripts
		private int version = 1; // Version=1 ... RFC 2965 style

		public SerializableCookie() {

		}

		public SerializableCookie(HttpCookie cookie) {
			this();
			set(cookie);
		}

		public void set(HttpCookie cookie) {
			if (cookie != null) {
				setName(cookie.getName());
				setValue(cookie.getValue());
				setComment(cookie.getComment());
				setCommentURL(cookie.getCommentURL());
				setToDiscard(cookie.getDiscard());
				setDomain(cookie.getDomain());
				setMaxAge(cookie.getMaxAge());
				setPath(cookie.getPath());
				setPortlist(cookie.getPortlist());
				setSecure(cookie.getSecure());
				setHttpOnly(cookie.isHttpOnly());
				setVersion(cookie.getVersion());
			}
		}

		public HttpCookie toHttpCookie() {
			HttpCookie asCookie = new HttpCookie(getName(), getValue());
			asCookie.setComment(getComment());
			asCookie.setCommentURL(getCommentURL());
			asCookie.setDiscard(isToDiscard());
			asCookie.setDomain(getDomain());
			asCookie.setMaxAge(getMaxAge());
			asCookie.setPath(getPath());
			asCookie.setPortlist(getPortlist());
			asCookie.setSecure(isSecure());
			asCookie.setHttpOnly(isHttpOnly());
			asCookie.setVersion(getVersion());

			return asCookie;

		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getComment() {
			return comment;
		}

		public void setComment(String comment) {
			this.comment = comment;
		}

		public String getCommentURL() {
			return commentURL;
		}

		public void setCommentURL(String commentURL) {
			this.commentURL = commentURL;
		}

		public boolean isToDiscard() {
			return toDiscard;
		}

		public void setToDiscard(boolean toDiscard) {
			this.toDiscard = toDiscard;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public long getMaxAge() {
			return maxAge;
		}

		public void setMaxAge(long maxAge) {
			this.maxAge = maxAge;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getPortlist() {
			return portlist;
		}

		public void setPortlist(String portlist) {
			this.portlist = portlist;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public boolean isHttpOnly() {
			return httpOnly;
		}

		public void setHttpOnly(boolean httpOnly) {
			this.httpOnly = httpOnly;
		}

		public int getVersion() {
			return version;
		}

		public void setVersion(int version) {
			this.version = version;
		}

		@Override
		public int hashCode() {
			return Objects.hash(comment, commentURL, domain, httpOnly, maxAge, name, path, portlist, secure, toDiscard,
					value, version);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SerializableCookie other = (SerializableCookie) obj;
			return Objects.equals(comment, other.comment) && Objects.equals(commentURL, other.commentURL)
					&& Objects.equals(domain, other.domain) && httpOnly == other.httpOnly && maxAge == other.maxAge
					&& Objects.equals(name, other.name) && Objects.equals(path, other.path)
					&& Objects.equals(portlist, other.portlist) && secure == other.secure
					&& toDiscard == other.toDiscard && Objects.equals(value, other.value) && version == other.version;
		}

		@Override
		public String toString() {
			return "SerializableCookie [name=" + name + ", value=" + value + ", comment=" + comment + ", commentURL="
					+ commentURL + ", toDiscard=" + toDiscard + ", domain=" + domain + ", maxAge=" + maxAge + ", path="
					+ path + ", portlist=" + portlist + ", secure=" + secure + ", httpOnly=" + httpOnly + ", version="
					+ version + "]";
		}

	}
	public static long getCookieCreationTime(HttpCookie cookie) {
		MethodHandle mh = tryToGetCookieCreationTimeMH();
		try {
			return (long) mh.invoke(cookie);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0l;
	}
	private static MethodHandle GET_CREATION_TIME=null;
	private static boolean TRIED_GET_CREATION_TIME=false;
	
	private static MethodHandle tryToGetCookieCreationTimeMH() {
		if(GET_CREATION_TIME==null&&!TRIED_GET_CREATION_TIME) {
			TRIED_GET_CREATION_TIME=true;
			GET_CREATION_TIME=createGetCookieCreationTimeMethodHandle();
		}
		return GET_CREATION_TIME;
	}
	
	private static MethodHandle createGetCookieCreationTimeMethodHandle() {
		try {
			Method m = HttpCookie.class.getDeclaredMethod("getCreationTime");
			m.trySetAccessible();
			return MethodHandles.privateLookupIn(HttpCookie.class, MethodHandles.lookup()).unreflect(m);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
			e.printStackTrace();
			System.err.println("Try adding '--add-opens java.base/java.net=ALL-UNNAMED' as a VM Arg");
		}
		return null;
	}

	public static void main(String[] args) {
		HttpCookie cookie = new HttpCookie("poop","loop");
		System.out.println(getCookieCreationTime(cookie));
	}
	
}
