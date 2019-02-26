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

import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 *
 * @author Guillaume Gillon
 */
public class PipelineConfiguration {

    private static final List<String> INGEST_PLUGINS = Arrays.asList("geoip", "user_agent");

    @Value("${reporters.elasticsearch.pipeline.plugins.ingest:#{null}}")
    private String ingestorPlugins;

    /**
     * Templating tool.
     */
    @Autowired
    private FreeMarkerComponent freeMarkerComponent;

    private final String pipeline = "gravitee_pipeline";

    private boolean valid = false;

    public String createPipeline() {
        if (ingestorPlugins != null && ! ingestorPlugins.isEmpty()) {
            final Set<String> configuredPlugin = Stream.of(ingestorPlugins.split(",")).map(String::trim).collect(toSet());
            configuredPlugin.retainAll(INGEST_PLUGINS);

            final String processors =
                    configuredPlugin
                            .stream()
                            .map(ingestPlug -> this.freeMarkerComponent.generateFromTemplate(ingestPlug + ".ftl"))
                            .collect(Collectors.joining(","));

            final Map<String,Object> processorsMap = new HashMap<>(1);
            processorsMap.put("processors", processors);
            return this.freeMarkerComponent.generateFromTemplate("pipeline.ftl", processorsMap);
        }

        return null;
    }

    public String getPipelineName() { return this.pipeline; }

    public String getPipeline() { return valid ? this.pipeline : null; }

    public void valid() {
        this.valid = true;
    }

    public String getIngestorPlugins() {
        return ingestorPlugins;
    }

    public void setIngestorPlugins(String ingestorPlugins) {
        this.ingestorPlugins = ingestorPlugins;
    }
}
