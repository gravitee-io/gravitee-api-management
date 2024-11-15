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
package io.gravitee.reporter.elasticsearch.mapping;

import io.gravitee.common.templating.FreeMarkerComponent;
import io.gravitee.elasticsearch.client.Client;
import io.gravitee.reporter.elasticsearch.config.ReporterConfiguration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractIndexPreparer implements IndexPreparer {

    /** Logger. */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ReporterConfiguration configuration;

    @Autowired
    protected FreeMarkerComponent freeMarkerComponent;

    @Autowired
    protected Client client;

    protected Map<String, Object> getTemplateData() {
        final Map<String, Object> data = new HashMap<>();

        data.put("indexName", this.configuration.getIndexName());
        data.put("numberOfShards", this.configuration.getNumberOfShards());
        data.put("numberOfReplicas", this.configuration.getNumberOfReplicas());
        data.put("refreshInterval", this.configuration.getRefreshInterval());
        data.put("indexLifecyclePolicyPropertyName", this.configuration.getIndexLifecyclePolicyPropertyName());
        data.put("indexLifecycleRolloverAliasPropertyName", this.configuration.getIndexLifecycleRolloverAliasPropertyName());
        data.put("indexLifecyclePolicyHealth", this.configuration.getIndexLifecyclePolicyHealth());
        data.put("indexLifecyclePolicyMonitor", this.configuration.getIndexLifecyclePolicyMonitor());
        data.put("indexLifecyclePolicyRequest", this.configuration.getIndexLifecyclePolicyRequest());
        data.put("indexLifecyclePolicyLog", this.configuration.getIndexLifecyclePolicyLog());
        data.put("extendedRequestMappingTemplate", this.configuration.getExtendedRequestMappingTemplate());
        data.put("extendedSettingsTemplate", this.configuration.getExtendedSettingsTemplate());
        return data;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public ReporterConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ReporterConfiguration configuration) {
        this.configuration = configuration;
    }

    public FreeMarkerComponent getFreeMarkerComponent() {
        return freeMarkerComponent;
    }

    public void setFreeMarkerComponent(FreeMarkerComponent freeMarkerComponent) {
        this.freeMarkerComponent = freeMarkerComponent;
    }
}
