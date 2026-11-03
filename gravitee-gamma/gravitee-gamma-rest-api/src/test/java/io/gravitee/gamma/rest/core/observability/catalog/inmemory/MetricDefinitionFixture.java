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
package io.gravitee.gamma.rest.core.observability.catalog.inmemory;

import io.gravitee.gamma.rest.core.observability.catalog.domain_service.AdditionalMetricCatalogDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.MetricDefinitionKeyDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalog;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.DeclareMetricDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.DeleteMetricDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.GetMetricDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.ListMetricDefinitionsUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.SaveMetricDefinitionUseCase;
import io.gravitee.gamma.rest.infra.adapter.AdditionalMetricGrammarPortAdapter;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;

/** The catalog use cases wired to in-memory stores, the real built-in filter catalog and configurable platform ports. */
public class MetricDefinitionFixture {

    public static final String ORGANIZATION = "org";
    public static final String ENVIRONMENT = "env";
    public static final String OTHER_ENVIRONMENT = "other-env";

    public final InMemoryAdditionalMetricDefinitionRepository repository = new InMemoryAdditionalMetricDefinitionRepository();
    public final InMemoryCatalogPorts ports = new InMemoryCatalogPorts();
    public final MetricDefinitionKeyDomainService keys = new MetricDefinitionKeyDomainService(
        new AdditionalMetricGrammarPortAdapter(),
        ports
    );
    public final AdditionalMetricCatalogDomainService catalog = new AdditionalMetricCatalogDomainService(keys);
    public final ListMetricDefinitionsUseCase list = new ListMetricDefinitionsUseCase(
        repository,
        ports,
        ports,
        ports,
        new SpiFilterRegistry(),
        ports,
        catalog
    );
    public final GetMetricDefinitionUseCase get = new GetMetricDefinitionUseCase(repository, keys);
    public final DeclareMetricDefinitionUseCase declare = new DeclareMetricDefinitionUseCase(repository, keys);
    public final SaveMetricDefinitionUseCase save = new SaveMetricDefinitionUseCase(repository, keys);
    public final DeleteMetricDefinitionUseCase delete = new DeleteMetricDefinitionUseCase(repository, keys);

    public AdditionalMetricDefinition givenDeclared(String environmentId, String name, String label, String unit, boolean exposed) {
        return declare.execute(new DeclareMetricDefinitionUseCase.Input(environmentId, name, label, null, unit, exposed)).definition();
    }

    public AdditionalMetricDefinition givenDeclared(String name) {
        return givenDeclared(ENVIRONMENT, name, null, null, true);
    }

    public AdditionalMetricCatalog listed() {
        return list.execute(new ListMetricDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT)).catalog();
    }
}
