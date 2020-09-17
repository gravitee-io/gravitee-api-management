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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 *
 * @author Guillaume Gillon
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class PipelineConfigurationTest {

    @Mock
    private ReporterConfiguration config;

    @Mock
    private FreeMarkerComponent freeMarkerComponent;

    @InjectMocks
    private PipelineConfiguration pipelineConfiguration;

    @Before
    public void init() {
        //MockitoAnnotations.initMocks(this);
        when(freeMarkerComponent.generateFromTemplate(eq("geoip.ftl"), anyMap())).thenReturn("{\"geoip\" : {\"field\" : \"remote-address\"}}");
        Map<String,Object> processorsMap = new HashMap<>(1);
        processorsMap.put("processors", freeMarkerComponent.generateFromTemplate("geoip.ftl", Collections.emptyMap()));
        when(freeMarkerComponent.generateFromTemplate("pipeline.ftl",processorsMap)).thenReturn("{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}");
    }

    @Test
    public void should_valid_pipeline_with_ingest_geoip() {
        pipelineConfiguration.setIngestorPlugins("geoip");

        String result = "{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}";

        String pipeline = pipelineConfiguration.createPipeline();

        Assert.assertEquals(result, pipeline);
    }

    @Test
    public void should_return_pipeline_name() {
        //String result = "{\"description\":\"Gravitee pipeline\",\"processors\":[{\"geoip\":{\"field\":\"remote-address\"}}]}";

        String builder2 = pipelineConfiguration.createPipeline();
        pipelineConfiguration.valid();

        Assert.assertEquals("gravitee_pipeline", pipelineConfiguration.getPipeline());
        Assert.assertEquals("gravitee_pipeline", pipelineConfiguration.getPipelineName());
    }

    @Test
    public void should_not_return_pipeline_name_when_not_valided() {

        String builder2 = pipelineConfiguration.createPipeline();

        Assert.assertNull(pipelineConfiguration.getPipeline());
        Assert.assertEquals("gravitee_pipeline", pipelineConfiguration.getPipelineName());

    }
}
