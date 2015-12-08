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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
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

    public void setUp() {
        MockitoAnnotations.initMocks(this);
        pathResolver.setApi(api);
    }

    @Test
    public void resolve_useRoot() {
        final Map<String, Path> paths = new HashMap<>();
        paths.putAll(new HashMap<String, Path>() {
            {
                Path p1 = new Path();
                p1.setPath("/");

                put(p1.getPath(), p1);

                Path p2 = new Path();
                p2.setPath("/toto");
                put(p2.getPath(), p2);

                Path p3 = new Path();
                p3.setPath("/storess");
                put(p3.getPath(), p3);
            }
        });

        when(api.getPaths()).thenReturn(paths);
        Request req = mock(Request.class);
        when(req.path()).thenReturn("/stores");

        Path path = pathResolver.resolve(req);
        Assert.assertNotNull(path);

        Assert.assertEquals("/", path.getPath());
    }

    @Test
    public void resolve_useExactPath() {
        final Map<String, Path> paths = new HashMap<>();
        paths.putAll(new HashMap<String, Path>() {
            {
                Path p1 = new Path();
                p1.setPath("/");

                put(p1.getPath(), p1);

                Path p2 = new Path();
                p2.setPath("/toto");
                put(p2.getPath(), p2);

                Path p3 = new Path();
                p3.setPath("/stores");
                put(p3.getPath(), p3);
            }
        });

        when(api.getPaths()).thenReturn(paths);
        Request req = mock(Request.class);
        when(req.path()).thenReturn("/stores");

        Path path = pathResolver.resolve(req);
        Assert.assertNotNull(path);

        Assert.assertEquals("/stores", path.getPath());
    }
}
