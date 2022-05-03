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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 *
 * @author GraviteeSource Team
 * @author Guillaume Gillon
 */
@RunWith(MockitoJUnitRunner.class)
public class PipelineConfigurationTest {

    @InjectMocks
    private PipelineConfiguration pipelineConfiguration;

    @Mock
    private FreeMarkerComponent freeMarkerComponent;

    @Test
    public void createPipeline_should_not_create_pipeline_if_no_plugin() {
        pipelineConfiguration.setIngestPlugins("");
        String pipeline = pipelineConfiguration.createPipeline();

        assertNull(pipeline);
    }

    @Test
    public void createPipeline_should_create_pipeline_with_geoip_plugin() {
        mockPluginJson("geoip", "{my-geoip-plugin}");
        mockPipelineJsonForPlugins("{my-geoip-plugin}", "{my-pipeline}");

        pipelineConfiguration.setIngestPlugins("geoip");
        String pipeline = pipelineConfiguration.createPipeline();

        assertEquals("{my-pipeline}", pipeline);
    }

    @Test
    public void createPipeline_should_create_pipeline_with_geoip_and_useragent_plugin() {
        mockPluginJson("geoip", "{my-geoip-plugin}");
        mockPluginJson("user_agent", "{my-useragent-plugin}");
        mockPipelineJsonForPlugins("{my-geoip-plugin},{my-useragent-plugin}", "{my-pipeline}");

        pipelineConfiguration.setIngestPlugins("geoip, user_agent");
        String pipeline = pipelineConfiguration.createPipeline();

        assertEquals("{my-pipeline}", pipeline);
    }

    @Test
    public void createPipeline_should_create_pipeline_ignoring_additional_unknown_plugins() {
        mockPluginJson("geoip", "{my-geoip-plugin}");
        mockPluginJson("user_agent", "{my-useragent-plugin}");
        mockPipelineJsonForPlugins("{my-geoip-plugin},{my-useragent-plugin}", "{my-pipeline}");

        pipelineConfiguration.setIngestPlugins("geoip, testAnother, unknown, user_agent, this does not exists");
        String pipeline = pipelineConfiguration.createPipeline();

        assertEquals("{my-pipeline}", pipeline);
    }

    @Test
    public void createPipeline_should_return_pipeline_name() {
        pipelineConfiguration.createPipeline();
        pipelineConfiguration.valid();

        assertEquals("gravitee_pipeline", pipelineConfiguration.getPipeline());
        assertEquals("gravitee_pipeline", pipelineConfiguration.getPipelineName());
    }

    @Test
    public void createPipeline_should_not_return_pipeline_name_when_not_valided() {
        pipelineConfiguration.createPipeline();

        Assert.assertNull(pipelineConfiguration.getPipeline());
        assertEquals("gravitee_pipeline", pipelineConfiguration.getPipelineName());

    }

    private void mockPluginJson(String pluginName, String pluginJson) {
        when(freeMarkerComponent.generateFromTemplate(eq(pluginName + ".ftl"), any())).thenReturn(pluginJson);
    }

    private void mockPipelineJsonForPlugins(String pluginsJson, String pipelineJson) {
        when(freeMarkerComponent.generateFromTemplate("pipeline.ftl", Map.of("processors", pluginsJson))).thenReturn(pipelineJson);
    }
}
