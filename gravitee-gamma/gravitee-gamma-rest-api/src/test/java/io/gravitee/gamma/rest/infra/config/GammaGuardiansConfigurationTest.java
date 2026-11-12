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
package io.gravitee.gamma.rest.infra.config;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import inmemory.EventCrudInMemory;
import inmemory.EventLatestCrudInMemory;
import inmemory.EventLatestQueryServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventPublicationDomainService;
import io.gravitee.apim.core.generic_event.domain_service.GenericEventWriterDomainService;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.json.JsonSerializer;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.use_case.DeployGuardianUseCase;
import io.gravitee.repository.management.api.GuardianRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class GammaGuardiansConfigurationTest {

    @Test
    void should_wire_the_gamma_deployment_use_case_to_the_apim_parent_context() throws Exception {
        var repository = mock(GuardianRepository.class);
        var mapper = GraviteeJacksonMapper.getInstance();
        var writer = new GenericEventWriterDomainService(new EventCrudInMemory(), new EventLatestCrudInMemory(), mapper);
        try (var parent = new AnnotationConfigApplicationContext(); var gamma = new AnnotationConfigApplicationContext()) {
            parent.registerBean(GuardianRepository.class, () -> repository);
            parent.registerBean(GenericEventWriterDomainService.class, () -> writer);
            parent.registerBean(AuditDomainService.class, () -> mock(AuditDomainService.class));
            parent.registerBean(PolicyPluginProvider.class, () -> mock(PolicyPluginProvider.class));
            parent.registerBean(EventLatestQueryService.class, EventLatestQueryServiceInMemory::new);
            parent.registerBean(GenericEventPublicationDomainService.class, () -> new GenericEventPublicationDomainService(writer, mapper));
            parent.registerBean(JsonSerializer.class, () -> new JacksonJsonSerializer());
            parent.registerBean(JsonDeserializer.class, () -> new JacksonJsonDeserializer());
            parent.refresh();
            gamma.setParent(parent);
            gamma.register(GammaGuardiansConfiguration.class);
            gamma.refresh();

            var input = new DeployGuardianUseCase.Input(
                AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId(ENVIRONMENT_ID).build(),
                GUARDIAN_ID,
                null
            );
            assertThatThrownBy(() -> gamma.getBean(DeployGuardianUseCase.class).execute(input)).isInstanceOf(
                GuardianNotFoundException.class
            );
            verify(repository).findById(GUARDIAN_ID);
        }
    }
}
