package net.gcolin.simplerepo.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class OsgiTemplateSource {

	private URL url;
	private long lastModified;
	private InputStream in;

	public OsgiTemplateSource(URL url) {
		this.url = url;
		this.lastModified = System.currentTimeMillis();
	}

	public long getLastModified() {
		return lastModified;
	}

	public URL getUrl() {
		return url;
	}

	public InputStream getInputStream() throws IOException {
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				// Ignore; this is maybe because it was closed for the 2nd time now
			}
		}
		in = url.openStream();
		return in;
	}

	public void close() throws IOException {
		if (in != null) {
			try {
				in.close();
			} finally {
				in = null;
			}
		}
	}
}
