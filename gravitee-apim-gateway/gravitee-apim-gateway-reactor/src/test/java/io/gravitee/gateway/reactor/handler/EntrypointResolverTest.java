/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactor.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactor.handler.impl.DefaultEntrypointResolver;
import io.gravitee.gateway.reactor.handler.impl.HandlerEntryPointComparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointResolverTest {

    private static final Logger logger = LoggerFactory.getLogger(EntrypointResolverTest.class);

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @InjectMocks
    private DefaultEntrypointResolver handlerResolver;

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private HttpHeaders httpHeaders;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(context.request()).thenReturn(request);
        when(request.headers()).thenReturn(httpHeaders);
    }

    @After
    public void tearDown() {
        reactorHandlerRegistry.clear();
    }

    @Test
    public void test_uniqContextPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));

        final ConcurrentSkipListSet<HandlerEntrypoint> handlerEntrypoints = new ConcurrentSkipListSet<>(new HandlerEntryPointComparator());
        handlerEntrypoints.addAll(Collections.singletonList(entrypoint1));
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);
        when(request.path()).thenReturn("/teams");

        assertEquals(entrypoint1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), eq(entrypoint1));
    }

    @Test
    public void test_uniqContextPath_unknownRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));

        final ConcurrentSkipListSet<HandlerEntrypoint> handlerEntrypoints = new ConcurrentSkipListSet<>(new HandlerEntryPointComparator());
        handlerEntrypoints.addAll(Collections.singletonList(entrypoint1));
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);
        when(request.path()).thenReturn("/team");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_validRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        final List<HandlerEntrypoint> handlerEntrypoints = Arrays.asList(entrypoint1, entrypoint2);
        handlerEntrypoints.sort(new HandlerEntryPointComparator());
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);
        when(request.path()).thenReturn("/teams");

        assertEquals(entrypoint1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), eq(entrypoint1));
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        final List<HandlerEntrypoint> handlerEntrypoints = Arrays.asList(entrypoint1, entrypoint2);
        handlerEntrypoints.sort(new HandlerEntryPointComparator());
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);

        when(request.path()).thenReturn("/team");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath2() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        final ConcurrentSkipListSet<HandlerEntrypoint> handlerEntrypoints = new ConcurrentSkipListSet<>(new HandlerEntryPointComparator());
        handlerEntrypoints.addAll(Arrays.asList(entrypoint1, entrypoint2));
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);

        when(request.path()).thenReturn("/teamss");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_extraSeparatorRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        final List<HandlerEntrypoint> handlerEntrypoints = Arrays.asList(entrypoint1, entrypoint2);
        handlerEntrypoints.sort(new HandlerEntryPointComparator());
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);

        when(request.path()).thenReturn("/teams/");

        assertEquals(entrypoint1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), eq(entrypoint1));
    }

    @Test
    public void test_multipleContextPath_extraSeparatorUnknownRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        final List<HandlerEntrypoint> handlerEntrypoints = Arrays.asList(entrypoint1, entrypoint2);
        handlerEntrypoints.sort(new HandlerEntryPointComparator());
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);

        when(request.path()).thenReturn("/teamss/");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleVhosts() {
        final List<HandlerEntrypoint> noHosts = new ArrayList<>();
        final List<HandlerEntrypoint> withHostAndPathABC = new ArrayList<>();
        final List<HandlerEntrypoint> withHostAndNotPathABC = new ArrayList<>();

        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/a")));
        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/b")));
        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/d")));
        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/e")));
        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/f")));
        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/c1/sub")));
        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/c1/sub2")));
        noHosts.add(new DummyReactorandlerEntrypoint(new VirtualHost(null, "/b/c1/sub3")));

        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api1.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api2.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api3.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api4.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("apiX.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api10.gravitee.io", "/a/b/c")));
        withHostAndPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api11.gravitee.io", "/a/b/c")));

        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api.gravitee.io", "/a/b/a")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api1.gravitee.io", "/a/b/b")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api2.gravitee.io", "/a/b/d")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api3.gravitee.io", "/a/b/e")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api4.gravitee.io", "/a/b/f")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("apiX.gravitee.io", "/a/b/c1/sub")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api10.gravitee.io", "/a/b/c1/sub")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("api11.gravitee.io", "/a/b/c1/sub")));
        withHostAndNotPathABC.add(new DummyReactorandlerEntrypoint(new VirtualHost("apispecial.gravitee.io", "/a/b/special")));

        final List<HandlerEntrypoint> handlerEntrypoints = new ArrayList<>();
        handlerEntrypoints.addAll(noHosts);
        handlerEntrypoints.addAll(withHostAndPathABC);
        handlerEntrypoints.addAll(withHostAndNotPathABC);
        handlerEntrypoints.sort(new HandlerEntryPointComparator());
        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(handlerEntrypoints);

        // Cases without host.
        for (final HandlerEntrypoint expected : noHosts) {
            logger.info("Test case: resolve for host [{}] with unknown path.", expected.host());
            final List<String> pathsShouldNotMatch = Arrays.asList("/unknown", "/A/b/C", "/a/b/c", "/a/b/c/");

            // Test some path that should be resolved to null.
            // "unknown" -> path not declared at all
            // "/A/b/C"  -> path with this specific case not declared
            // "/a/b/c" and "/a/b/c/" -> declared but not matching with query without the appropriate host.
            pathsShouldNotMatch.forEach(
                path -> {
                    logger.info("Trying to resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(null);
                    when(request.path()).thenReturn(path);
                    assertNull(handlerResolver.resolve(context));
                }
            );

            // All this paths must match because they are exposed in path mode (no host).
            final List<String> pathsShouldMatch = Arrays.asList(
                expected.path(),
                expected.path() + "/",
                expected.path() + "/a/long/sub/path/i/want/it/to/match"
            );

            pathsShouldMatch.forEach(
                path -> {
                    logger.info("Test case: to resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(null);
                    when(request.path()).thenReturn(path);
                    assertEquals(expected, handlerResolver.resolve(context));
                }
            );
        }

        // Cases with host and path "/a/b/c"
        for (final HandlerEntrypoint expected : withHostAndPathABC) {
            // Test some path that should be resolved to null.
            // "unknown" -> path not declared at all
            // "/A/b/C"  -> path with this specific case not declared
            // "/a/b/special" -> declared but not matching for hosts where path is "/a/b/c"
            final List<String> pathsShouldNotMatch = Arrays.asList("/unknown", "/A/b/C", "/a/b/special");

            pathsShouldNotMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertNull(handlerResolver.resolve(context));
                }
            );

            // All this paths must match because they all starts with "/a/b/c".
            final List<String> pathsShouldMatch = Arrays.asList(
                "/a/b/c",
                "/a/b/c/",
                "/a/b/c/sub",
                "/a/b/c/sub/",
                "/a/b/c/sub/a/long/sub/path/i/want/it/to/match"
            );

            pathsShouldMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertEquals(expected, handlerResolver.resolve(context));
                }
            );
        }

        // Cases with host and path NOT "/a/b/c"
        for (final HandlerEntrypoint expected : withHostAndNotPathABC) {
            // Test some path that should be resolved to null.
            // "unknown" -> path not declared at all
            // All other paths are either on all host ('*') either for an host which is also exposing the paths we are testing (ex: "api1.gravitee.io" exposes multiples paths).
            final List<String> pathsShouldNotMatch = Collections.singletonList("/unknown");

            pathsShouldNotMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertNull(handlerResolver.resolve(context));
                }
            );

            // All this paths must match because
            final List<String> pathsShouldMatch = Arrays.asList(
                expected.path(),
                expected.path() + "/",
                expected.path() + "/a/long/sub/path/i/want/it/to/match"
            );

            pathsShouldMatch.forEach(
                path -> {
                    logger.info("Test case: resolve for host [{}] and path [{}].", expected.host(), path);
                    reset(request);
                    when(request.host()).thenReturn(expected.host());
                    when(request.path()).thenReturn(path);
                    assertEquals(expected, handlerResolver.resolve(context));
                }
            );
        }
    }

    private class DummyReactorandlerEntrypoint implements HandlerEntrypoint {

        private final Entrypoint entrypoint;

        public DummyReactorandlerEntrypoint(Entrypoint entrypoint) {
            this.entrypoint = entrypoint;
        }

        @Override
        public <T extends ReactorHandler> T target() {
            return null;
        }

        @Override
        public ExecutionMode executionMode() {
            return null;
        }

        @Override
        public String path() {
            return entrypoint.path();
        }

        @Override
        public String host() {
            return entrypoint.host();
        }

        @Override
        public int priority() {
            return entrypoint.priority();
        }

        @Override
        public boolean accept(Request request) {
            return entrypoint.accept(request);
        }

        @Override
        public boolean accept(String host, String path) {
            return entrypoint.accept(host, path);
        }
    }
}
