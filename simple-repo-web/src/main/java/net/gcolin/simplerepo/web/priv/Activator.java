package net.gcolin.simplerepo.web.priv;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import freemarker.template.Configuration;
import net.gcolin.simplerepo.web.OsgiTemplateLoader;

public class Activator implements BundleActivator {

	private ServiceTracker httptracker;
	private Logger log = Logger.getLogger(this.getClass().getName());
	private IndexServlet indexServlet = new IndexServlet();
	private Configuration configuration;
	
	@Override
	public void start(BundleContext context) throws Exception {
		configuration = OsgiTemplateLoader.buildConfig(context);
		
		httptracker = new ServiceTracker(context, HttpService.class.getName(), new ServiceTrackerCustomizer() {
			
			@Override
			public void removedService(ServiceReference reference, Object serv) {
				HttpService service = (HttpService) serv;
				service.unregister("");
			}
			
			@Override
			public void modifiedService(ServiceReference reference, Object service) {}
			
			@Override
			public Object addingService(ServiceReference reference) {
				HttpService service = (HttpService) context.getService(reference);
				try {
					service.registerServlet("", indexServlet, null, service.createDefaultHttpContext());
				} catch (ServletException | NamespaceException ex) {
					log.log(Level.SEVERE, ex.getMessage(), ex);
				}
				
				return service;
			}
		});
		httptracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		httptracker.close();
		OsgiTemplateLoader.close(context, configuration);
	}

}
