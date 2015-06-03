package io.gravitee.gateway.platforms.jetty;

import io.gravitee.gateway.core.PlatformContext;
import io.gravitee.gateway.platforms.servlet.DispatcherServlet;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyDispatcherServlet extends DispatcherServlet {

    public final static String GRAVITEE_CONTEXT_ATTRIBUTE = "gravitee.context";

    @Override
    protected PlatformContext getPlatformContext() {
        return (PlatformContext) getServletContext().getAttribute(GRAVITEE_CONTEXT_ATTRIBUTE);
    }
}
