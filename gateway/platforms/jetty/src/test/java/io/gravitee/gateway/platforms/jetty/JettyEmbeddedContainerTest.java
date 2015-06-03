package io.gravitee.gateway.platforms.jetty;

import io.gravitee.gateway.platforms.jetty.resource.ApiExternalResource;
import io.gravitee.gateway.platforms.jetty.servlet.ApiServlet;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyEmbeddedContainerTest {

    @ClassRule
    public static ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/*", null);

    @

    @Test
    public void callUrl() {

    }
}
