/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.handlers.api.processor.pathmapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.PathMapping;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.gravitee.reporter.api.v4.log.Log;
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
        api.setPathMappings(Map.of());
        pathMappingProcessor.execute(spyCtx).test().assertResult();
        verify(spyCtx, never()).metrics();
    }

    @Test
    public void shouldAddMappedPathWithMapping() {
        api.setPathMappings(Map.of(PATH_INFO, Pattern.compile("/path/.*/info/")));
        pathMappingProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyCtx.metrics().getMappedPath()).isEqualTo(PATH_INFO);
    }

    @Test
    public void shouldAddShortestMappedPathWithTwoMapping() {
        String shorterPath = "/path";
        api.setPathMappings(Map.of(PATH_INFO, Pattern.compile("/path/.*/info/"), shorterPath, Pattern.compile("/path.*")));
        pathMappingProcessor.execute(spyCtx).test().assertResult();
        assertThat(spyCtx.metrics().getMappedPath()).isEqualTo(shorterPath);
    }

    @Test
    public void shouldNotAddMappedPathWithMappingButEmptyPathInfo() {
        when(mockRequest.pathInfo()).thenReturn("");
        api.setPathMappings(Map.of(PATH_INFO, Pattern.compile("/path/.*/info/")));
        pathMappingProcessor.execute(spyCtx).test().assertResult();
        verify(spyCtx, never()).metrics();
    }

    @Test
    public void shouldBuildPatternWithParenthesesWithoutError() {
        String path = "/Trunks(Pbx)/:id/Export()";
        Pattern pattern = PathMapping.buildPattern(path);
        assertDoesNotThrow(() -> pattern.matcher("/Trunks(Pbx)/123/Export()").matches());
    }

    @Test
    public void shouldBuildPatternWithDotsAndMultipleParams() {
        String path = "/api/:version/user.:id/info";
        Pattern pattern = PathMapping.buildPattern(path);
        assertTrue(pattern.matcher("/api/v1/user.42/info").matches());
    }

    @Test
    public void shouldBuildPatternWithNestedParamsAndTrailingSlash() {
        String path = "/root/:param1/:param2/";
        Pattern pattern = PathMapping.buildPattern(path);

        assertTrue(pattern.matcher("/root/a/b/").matches());
        assertFalse(pattern.matcher("/root/a/b").matches()); // trailing slash required, as before
    }

    @Test
    public void shouldNotThrowWhenSpecialCharactersPresent() {
        String path = "/weird(path)/file(name):id";
        assertDoesNotThrow(() -> PathMapping.buildPattern(path));
    }

    @Test
    public void shouldNotThrowForExportTrunkLikePath() {
        String path = "/Trunks([^/]*/Pbx.ExportTrunk()/*";
        assertDoesNotThrow(() -> PathMapping.buildPattern(path));
    }
}
