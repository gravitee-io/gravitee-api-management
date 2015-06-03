package io.gravitee.gateway.platforms.jetty;

import io.gravitee.gateway.core.PlatformContext;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.Registry;
import io.gravitee.gateway.core.impl.DefaultReactor;
import io.gravitee.gateway.core.registry.FileRegistry;
import io.gravitee.gateway.platforms.jetty.context.JettyPlatformContext;
import io.gravitee.gateway.platforms.jetty.resource.ApiExternalResource;
import io.gravitee.gateway.platforms.jetty.servlet.ApiServlet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.*;

import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class JettyEmbeddedContainerTest {

    @ClassRule
    public static ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/*", null);

    private JettyEmbeddedContainer container;

    @Before
    public void setUp() throws Exception {
        PlatformContext platformContext = prepareContext();
        container = new JettyEmbeddedContainer(platformContext);
        container.start();
    }

    private PlatformContext prepareContext() {
        URL url = JettyEmbeddedContainerTest.class.getResource("/conf/test01");

        Registry registry = new FileRegistry(url.getPath());
        Reactor reactor = new DefaultReactor();

        return new JettyPlatformContext(registry, reactor);
    }

    @After
    public void stop() throws Exception {
        container.stop();
    }

    @Test
    public void doHttpGet() throws IOException {
        Request request = Request.Get("http://localhost:8082/test");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_BAD_REQUEST, returnResponse.getStatusLine().getStatusCode());
    }
}
