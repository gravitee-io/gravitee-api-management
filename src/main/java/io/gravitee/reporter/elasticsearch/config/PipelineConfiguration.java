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
package io.gravitee.reporter.elasticsearch.config;

import static java.util.stream.Collectors.toSet;

import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author GraviteeSource Team
 * @author Guillaume Gillon
 */
public class PipelineConfiguration {

    private static final List<String> RETAINED_INGEST_PLUGINS = Arrays.asList("geoip", "user_agent", "gravitee");

    private final String ingestPlugins;

    private final String userAgentRegexFile;

    private final FreeMarkerComponent freeMarkerComponent;

    private final String pipeline = "gravitee_pipeline";

    private boolean valid = false;

    public PipelineConfiguration(String ingestPlugins, String userAgentRegexFile, FreeMarkerComponent freeMarkerComponent) {
        this.ingestPlugins = ingestPlugins;
        this.userAgentRegexFile = userAgentRegexFile;
        this.freeMarkerComponent = freeMarkerComponent;
    }

    public String createPipeline() {
        if (ingestPlugins != null && !ingestPlugins.isEmpty()) {
            final Set<String> configuredPlugin = Stream.of(ingestPlugins.split(",")).map(String::trim).collect(toSet());
            configuredPlugin.retainAll(RETAINED_INGEST_PLUGINS);

            final Map<String, Object> data = new HashMap<>();
            data.put("userAgentRegexFile", userAgentRegexFile);

            final String processors = configuredPlugin
                .stream()
                .map(ingestPlug -> this.freeMarkerComponent.generateFromTemplate(ingestPlug + ".ftl", data))
                .collect(Collectors.joining(","));

            final Map<String, Object> processorsMap = new HashMap<>(1);
            processorsMap.put("processors", processors);
            return this.freeMarkerComponent.generateFromTemplate("pipeline.ftl", processorsMap);
        }

        return null;
    }

    public String getPipelineName() {
        return this.pipeline;
    }

    public String getPipeline() {
        return valid ? this.pipeline : null;
    }

    public void valid() {
        this.valid = true;
    }
}
