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
package io.gravitee.gateway.services.healthcheck.context;

import io.gravitee.el.TemplateVariableProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HealthCheckContextFactory {

    @Autowired
    private HealthCheckTemplateVariableProviderFactory templateVariableProviderFactory;

    private List<TemplateVariableProvider> providers;

    public HealthCheckContext create(ApiTemplateVariableProvider apiTemplateVariableProvider) {
        final ArrayList<TemplateVariableProvider> templateVariableProviders = getProviders();
        templateVariableProviders.add(apiTemplateVariableProvider);

        final HealthCheckContext healthCheckContext = new HealthCheckContext();
        healthCheckContext.setProviders(templateVariableProviders);
        return healthCheckContext;
    }

    private ArrayList<TemplateVariableProvider> getProviders() {
        providers = templateVariableProviderFactory.getTemplateVariableProviders();
        final ArrayList<TemplateVariableProvider> templateVariableProviders = new ArrayList<>(providers);
        return templateVariableProviders;
    }
}
