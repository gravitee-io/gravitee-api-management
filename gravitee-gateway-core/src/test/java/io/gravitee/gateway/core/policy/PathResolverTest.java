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
package io.gravitee.gateway.core.policy;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Path;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.impl.PathResolverImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class PathResolverTest {

    @InjectMocks
    private PathResolverImpl pathResolver;

    @Mock
    private Api api;

    @Mock
    private Request request;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        pathResolver.setApi(api);

        final Map<String, Path> paths = new TreeMap<>((Comparator<String>) (path1, path2) -> path2.compareTo(path1));

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

        when(api.getPaths()).thenReturn(paths);
    }

    @Test
    public void resolve_root() {
        when(request.path()).thenReturn("/myapi");

        Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_exactPath() {
        when(request.path()).thenReturn("/stores");

        Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath() {
        when(request.path()).thenReturn("/stores/99");

        Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_checkChildren() {
        when(request.path()).thenReturn("/stores/99/data");

        Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores/:storeId", path.getPath());
    }

    @Test
    public void resolve_noParameterizedPath_checkChildren() {
        when(request.path()).thenReturn("/products/99/data");

        Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/products", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot() {
        when(request.path()).thenReturn("/mypath");

        Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_pathParameterizedPath_mustReturnRoot2() {
        when(request.path()).thenReturn("/weather_invalidpath");

        Path path = pathResolver.resolve(request);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }
}
