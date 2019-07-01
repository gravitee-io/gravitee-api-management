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

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Path;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.handlers.api.path.PathResolver;
import io.gravitee.gateway.handlers.api.path.impl.ApiPathResolverImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

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

        final Map<String, Path> paths = new HashMap<>();

        paths.putAll(new HashMap<String, Path>() {
            {
                Path p1 = new Path();
                p1.setPath("/");
                put(p1.getPath(), p1);

                Path p2 = new Path();
                p2.setPath("/products");
                put(p2.getPath(), p2);

                Path p3 = new Path();
                p3.setPath("/stores");
                put(p3.getPath(), p3);

                Path p4 = new Path();
                p4.setPath("/stores/:storeId");
                put(p4.getPath(), p4);

                Path p5 = new Path();
                p5.setPath("/[0-9,;]+");
                put(p5.getPath(), p5);

                Path p6 = new Path();
                p6.setPath("/Stores/:storeId");
                put(p6.getPath(), p6);
            }
        });

        // API 1
        when(api.getPaths()).thenReturn(paths);
        pathResolver = new ApiPathResolverImpl(api);

        // API 2
        when(api2.getPaths()).thenReturn(paths);
        pathResolver2 = new ApiPathResolverImpl(api2);
    }

    @Test
    public void resolve_root() {
        when(request.path()).thenReturn("/myapi");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_exactPath() {
        when(request.path()).thenReturn("/stores");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath() {
        when(request.path()).thenReturn("/stores/99");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_checkChildren() {
        when(request.path()).thenReturn("/stores/99/data");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_noParameterizedPath_checkChildren() {
        when(request.path()).thenReturn("/products/99/data");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/products", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot() {
        when(request.path()).thenReturn("/mypath");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot2() {
        when(request.path()).thenReturn("/weather_invalidpath");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnRoot() {
        when(request.path()).thenReturn("/v1/products/");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath() {
        when(request.path()).thenReturn("/v1/products/stores/99");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath2() {
        when(request.path()).thenReturn("/v1/products/stores/file.txt");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath3() {
        when(request.path()).thenReturn("/v1/products/stores/file%20sqs/toto");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath4() {
        when(request.path()).thenReturn("/v1/products/stores/file;&,.=sqs/toto");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath5() {
        when(request.path()).thenReturn("/v1/products/2124%3B2125");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/[0-9,;]+", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath5_notEncoded() {
        when(request.path()).thenReturn("/v1/products/2124;2125");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/[0-9,;]+", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath_notCaseSensitive() {
        when(request.path()).thenReturn("/v1/products/Stores/1234");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/Stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath_notCaseSensitive1() {
        when(request.path()).thenReturn("/v1/products/stores/Stores");
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/", request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }
}
