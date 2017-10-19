package net.gcolin.simplerepo.web.priv;

import java.io.IOException;
import java.io.Writer;

import net.gcolin.simplerepo.Parameters;
import net.gcolin.simplerepo.web.AbstractDisplayServlet;

public class IndexServlet extends AbstractDisplayServlet {

	private static final long serialVersionUID = 8684601066682570114L;

	@Override
	protected void doContent(Parameters params, Writer writer) throws IOException {
		for (int i = 0; i < listeners.size(); i++) {
			listeners.get(i).onIndex(params, writer);
		}
	}

	@Override
	protected String getTitle() {
		return "Simple repo";
	}

}
