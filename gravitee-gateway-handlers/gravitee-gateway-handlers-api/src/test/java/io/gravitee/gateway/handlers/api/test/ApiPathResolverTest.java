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
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.handlers.api.path.PathResolver;
import io.gravitee.gateway.handlers.api.path.impl.ApiPathResolverImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

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
            }
        });

        // API 1
        Proxy proxy = new Proxy();
        proxy.setContextPath("/");
        when(api.getProxy()).thenReturn(proxy);
        when(api.getPaths()).thenReturn(paths);

        pathResolver = new ApiPathResolverImpl(api);

        // API 2
        Proxy proxy2 = new Proxy();
        proxy2.setContextPath("/v1/products");
        when(api2.getProxy()).thenReturn(proxy2);
        when(api2.getPaths()).thenReturn(paths);

        pathResolver2 = new ApiPathResolverImpl(api2);
    }

    @Test
    public void resolve_root() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/myapi");
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getResolvedPath());
    }

    @Test
    public void resolve_exactPath() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/stores");
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores", path.getPath());
        Assert.assertEquals("/stores", path.getResolvedPath());
    }

    @Test
    public void resolve_pathParameterizedPath() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/stores/99");
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        Assert.assertEquals("/stores/:storeId", path.getResolvedPath());
    }

    @Test
    public void resolve_pathParameterizedPath_checkChildren() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/stores/99/data");
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
        Assert.assertEquals("/stores/:storeId", path.getResolvedPath());
    }

    @Test
    public void resolve_noParameterizedPath_checkChildren() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/products/99/data");
        Assert.assertNotNull(path);

        Assert.assertEquals("/products", path.getPath());
        Assert.assertEquals("/products", path.getResolvedPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/mypath");
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
        Assert.assertEquals("/", path.getResolvedPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot2() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver.resolve("/weather_invalidpath");
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
        Assert.assertEquals("/", path.getResolvedPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnRoot() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/");
        Assert.assertNotNull(path);

        Assert.assertEquals("/v1/products/", path.getPath());
        Assert.assertEquals("/", path.getResolvedPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/stores/99");
        Assert.assertNotNull(path);

        Assert.assertEquals("/v1/products/stores/:storeId", path.getPath());
        Assert.assertEquals("/stores/:storeId", path.getResolvedPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath2() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/stores/file.txt");
        Assert.assertNotNull(path);

        Assert.assertEquals("/v1/products/stores/:storeId", path.getPath());
        Assert.assertEquals("/stores/:storeId", path.getResolvedPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath3() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/stores/file%20sqs/toto");
        Assert.assertNotNull(path);

        Assert.assertEquals("/v1/products/stores/:storeId", path.getPath());
        Assert.assertEquals("/stores/:storeId", path.getResolvedPath());
    }

    @Test
    public void resolve_pathWithContextPath_mustReturnParameterizedPath4() {
        io.gravitee.gateway.handlers.api.path.Path path = pathResolver2.resolve("/v1/products/stores/file;&,.=sqs/toto");
        Assert.assertNotNull(path);

        Assert.assertEquals("/v1/products/stores/:storeId", path.getPath());
        Assert.assertEquals("/stores/:storeId", path.getResolvedPath());
    }
}
