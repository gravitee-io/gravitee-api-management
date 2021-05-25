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
package io.gravitee.gateway.handlers.api.test;

import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Rule;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.handlers.api.path.PathParam;
import io.gravitee.gateway.handlers.api.path.PathResolver;
import io.gravitee.gateway.handlers.api.path.impl.ApiPathResolverImpl;
import java.util.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiPathResolverTest {

    private PathResolver pathResolver;
    private PathResolver pathResolver2;

    @Mock
    private Api api;

    @Mock
    private Api api2;

    @Mock
    private Request request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        final Map<String, List<Rule>> paths = new HashMap<>();

        paths.putAll(
            new HashMap<String, List<Rule>>() {
                {
                    put("/", new ArrayList<Rule>());
                    put("/products", new ArrayList<Rule>());
                    put("/stores", new ArrayList<Rule>());
                    put("/stores/:storeId", new ArrayList<Rule>());
                    put("/[0-9,;]+", new ArrayList<Rule>());
                    put("/Stores/:storeId", new ArrayList<Rule>());
                    put("/stores/:storeId/order/:orderId", new ArrayList<Rule>());
                }
            }
        );

        // API 1
        when(api.getPaths()).thenReturn(paths);
        pathResolver = new ApiPathResolverImpl(api);

        // API 2
        Map<String, List<Rule>> api2Paths = new HashMap<>(paths);
        api2Paths.remove("/");
        when(api2.getPaths()).thenReturn(api2Paths);
        pathResolver2 = new ApiPathResolverImpl(api2);
    }

    @Test
    public void resolve_root() {
        when(request.pathInfo()).thenReturn("/");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_unknown() {
        when(request.pathInfo()).thenReturn("/");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertNull(path.getPath());
        Assert.assertEquals(Collections.emptyList(), path.getRules());
    }

    @Test
    public void resolve_exactPath() {
        when(request.pathInfo()).thenReturn("/stores");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath() {
        when(request.pathInfo()).thenReturn("/stores/99");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_pathParameterizedPath_checkChildren() {
        when(request.pathInfo()).thenReturn("/stores/99/data");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_noParameterizedPath_checkChildren() {
        when(request.pathInfo()).thenReturn("/products/99/data");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/products", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot() {
        when(request.pathInfo()).thenReturn("/mypath");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot2() {
        when(request.pathInfo()).thenReturn("/weather_invalidpath");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnRoot() {
        when(request.pathInfo()).thenReturn("/");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath() {
        when(request.pathInfo()).thenReturn("/stores/99");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath2() {
        when(request.pathInfo()).thenReturn("/stores/file.txt");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath3() {
        when(request.pathInfo()).thenReturn("/stores/file%20sqs/toto");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath4() {
        when(request.pathInfo()).thenReturn("/stores/file;&,.=sqs/toto");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath5() {
        when(request.pathInfo()).thenReturn("/2124%3B2125");

        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/[0-9,;]+", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath5_notEncoded() {
        when(request.pathInfo()).thenReturn("/2124;2125");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/[0-9,;]+", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath_notCaseSensitive() {
        when(request.pathInfo()).thenReturn("/Stores/1234");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/Stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath_notCaseSensitive1() {
        when(request.pathInfo()).thenReturn("/stores/Stores");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        validatePathParams(path.getParameters(), Collections.singletonList(new PathParam("storeId", 2)));
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameteriedPath6() {
        when(request.pathInfo()).thenReturn("/stores/my_petstore/order/190783");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId/order/:orderId", path.getPath());
        validatePathParams(path.getParameters(), Arrays.asList(new PathParam("storeId", 2), new PathParam("orderId", 4)));
    }

    private void validatePathParams(List<PathParam> pathParameters, List<PathParam> expectedPathsParams) {
        Assert.assertNotNull(pathParameters);
        Assert.assertEquals(expectedPathsParams.size(), pathParameters.size());
        Assert.assertTrue(pathParameters.containsAll(expectedPathsParams));
    }
}
