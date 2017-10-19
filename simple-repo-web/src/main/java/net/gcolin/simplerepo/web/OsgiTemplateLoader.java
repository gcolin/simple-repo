package net.gcolin.simplerepo.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;

import freemarker.cache.CacheStorage;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public class OsgiTemplateLoader implements TemplateLoader, BundleListener {

	private String prefix;
	private String suffix;
	private Bundle bundle;
	private Configuration config;
	private ServiceRegistration registration;

	public OsgiTemplateLoader(String prefix, String suffix, Bundle bundle, Configuration config) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.bundle = bundle;
		this.config = config;
	}

	@Override
	public long getLastModified(Object templateSource) {
		return ((OsgiTemplateSource) templateSource).getLastModified();
	}

	@Override
	public Object findTemplateSource(String name) throws IOException {
		int split = name.indexOf('@');
		String bundleName = name.substring(0, split);
		for(Bundle b : bundle.getBundleContext().getBundles()) {
			if(b.getState() == Bundle.ACTIVE && b.getSymbolicName().equals(bundleName)) {
				URL url = b.getEntry(prefix + name.substring(split + 1) + suffix);
				if(url != null) {
					return new OsgiTemplateSource(url);
				}
			}
		}
		return null;
	}

	@Override
	public Reader getReader(Object templateSource, String encoding) throws IOException {
		return new InputStreamReader(((OsgiTemplateSource) templateSource).getInputStream(), encoding);
	}

	@Override
	public void closeTemplateSource(Object templateSource) throws IOException {
		((OsgiTemplateSource) templateSource).close();
	}

	public static Configuration buildConfig(BundleContext ctx) {
		Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
		OsgiTemplateLoader tmplLoader = new OsgiTemplateLoader("tmpl/", "ftl", ctx.getBundle(), configuration);
		configuration.setTemplateLoader(tmplLoader);
		configuration.setDefaultEncoding("UTF-8");
		configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		configuration.getCacheStorage().clear();
		
		tmplLoader.registration = ctx.registerService(Configuration.class.getName(), configuration, null);
		return configuration;
	}
	
	public static void close(BundleContext ctx, Configuration configuration) {
		OsgiTemplateLoader tmplLoader = (OsgiTemplateLoader) configuration.getTemplateLoader();
		tmplLoader.registration.unregister();
		tmplLoader.clear();
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		if (event.getType() != Bundle.ACTIVE) {
			clear();
		}
	}

	private void clear() {
		CacheStorage storage = config.getCacheStorage();
		if(storage != null) {
			storage.clear();
		}
	}

}
