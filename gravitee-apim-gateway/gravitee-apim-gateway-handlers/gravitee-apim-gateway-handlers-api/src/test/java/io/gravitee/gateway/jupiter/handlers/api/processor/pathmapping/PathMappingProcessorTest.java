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
package io.gravitee.gateway.jupiter.handlers.api.processor.pathmapping;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class PathMappingProcessorTest extends AbstractProcessorTest {

    private static final String PATH_INFO = "/path/:id/info";
    private PathMappingProcessor pathMappingProcessor;

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockRequest.pathInfo()).thenReturn(PATH_INFO);
        pathMappingProcessor = PathMappingProcessor.instance();
    }

    @Test
    public void shouldNotAddMappedPathWithEmptyMapping() {
        api.getDefinition().setPathMappings(Map.of());
        pathMappingProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockMetrics);
    }

    @Test
    public void shouldAddMappedPathWithMapping() {
        api.getDefinition().setPathMappings(Map.of(PATH_INFO, Pattern.compile("/path/.*/info/")));
        pathMappingProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setMappedPath(PATH_INFO);
    }

    @Test
    public void shouldAddShortestMappedPathWithTwoMapping() {
        String shorterPath = "/path";
        api.getDefinition().setPathMappings(Map.of(PATH_INFO, Pattern.compile("/path/.*/info/"), shorterPath, Pattern.compile("/path.*")));
        pathMappingProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setMappedPath(shorterPath);
    }

    @Test
    public void shouldNotAddMappedPathWithMappingButEmptyPathInfo() {
        when(mockRequest.pathInfo()).thenReturn("");
        api.getDefinition().setPathMappings(Map.of(PATH_INFO, Pattern.compile("/path/.*/info/")));
        pathMappingProcessor.execute(ctx).test().assertResult();
        verifyNoInteractions(mockMetrics);
    }
}
