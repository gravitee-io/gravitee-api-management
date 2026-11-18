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
package io.gravitee.apim.infra.converter.oai;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class OAIPathMappingsTest {

    @Test
    void should_convert_path_parameters() {
        var spec = specWithPaths("/pets/{petId}", "/pets");
        assertThat(OAIPathMappings.from(spec)).containsExactly("/pets/:petId", "/pets");
    }

    @Test
    void should_skip_paths_outside_the_form_rule() {
        var spec = specWithPaths("/a~b", "/users/@me", "/", "/pets/:petId");
        assertThat(OAIPathMappings.from(spec)).containsExactly("/pets/:petId");
    }

    @Test
    void should_skip_uncompilable_paths() {
        var spec = specWithPaths("/a/**", "/ok");
        assertThat(OAIPathMappings.from(spec)).containsExactly("/ok");
    }

    @Test
    void should_leave_path_mappings_empty_when_spec_has_no_paths() {
        assertThat(OAIPathMappings.from(new OpenAPI())).isEmpty();
    }

    @Test
    void should_log_a_skipped_template_without_line_breaks() {
        var mappings = new ArrayList<String>();
        var spec = specWithPaths("/a/**\r\n\u0085\u2028\u2029INJECTED", "/ok");
        var events = captureInfoLogs(() -> mappings.addAll(OAIPathMappings.from(spec)));

        assertThat(mappings).containsExactly("/ok");
        assertThat(events).anySatisfy(event -> {
            assertThat(event.getFormattedMessage()).isEqualTo("Skipping invalid OpenAPI path mapping template [/a/**INJECTED]");
            assertThat(event.getFormattedMessage()).doesNotContain("\r", "\n", "\u0085", "\u2028", "\u2029");
        });
    }

    private static List<ILoggingEvent> captureInfoLogs(Runnable action) {
        var logger = (Logger) LoggerFactory.getLogger(OAIPathMappings.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list;
    }

    private static OpenAPI specWithPaths(String... paths) {
        var openAPI = new OpenAPI();
        var pathMap = new Paths();
        for (var path : paths) {
            pathMap.addPathItem(path, new PathItem());
        }
        openAPI.setPaths(pathMap);
        return openAPI;
    }
}
