package io.gravitee.gateway.core.reactor;

import com.google.common.net.HttpHeaders;
import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.api.Request;
import io.gravitee.model.Api;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RouteMatcherTest {

    @Mock
    private Registry registry;

    @Mock
    private Request request;

    @Mock
    private Map<String, String> headers;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void goodPathAndGoodHost() {
        when(registry.listAll()).thenReturn(apis());

        // Prepare request
        when(request.headers()).thenReturn(headers);
        when(headers.get(HttpHeaders.HOST)).thenReturn("public-api");
        when(request.path()).thenReturn("/v1/api");

        RouteMatcher matcher = new RouteMatcher(registry);
        Api found = matcher.match(request);

        Assert.assertNotNull(found);
        Assert.assertEquals(found.getName(), "my-api");
    }

    @Test
    public void goodPathAndBadHost() {
        when(registry.listAll()).thenReturn(apis());

        // Prepare request
        when(request.headers()).thenReturn(headers);
        when(headers.get(HttpHeaders.HOST)).thenReturn("public-api-host");
        when(request.path()).thenReturn("/v1/api");

        RouteMatcher matcher = new RouteMatcher(registry);
        Api found = matcher.match(request);

        Assert.assertNull(found);
    }

    @Test
    public void badPath() {
        when(registry.listAll()).thenReturn(apis());

        // Prepare request
        when(request.headers()).thenReturn(headers);
        when(headers.get(HttpHeaders.HOST)).thenReturn("public-api");
        when(request.path()).thenReturn("/v2/api");

        RouteMatcher matcher = new RouteMatcher(registry);
        Api found = matcher.match(request);

        Assert.assertNull(found);
    }

    private Set<Api> apis() {
        Set<Api> apis = new HashSet<>();

        Api api = new Api();
        api.setName("my-api");
        api.setTargetURI(URI.create("http://my-remote-api/v1/api"));
        api.setPublicURI(URI.create("http://public-api/v1/api"));
        apis.add(api);

        return apis;
    }
}
