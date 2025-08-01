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
package io.gravitee.apim.core.api.use_case;

import fixtures.core.model.ApiCRDFixtures;
import inmemory.ApiCRDExportDomainServiceInMemory;
import io.gravitee.apim.core.api.model.crd.IDExportStrategy;
import io.gravitee.apim.core.audit.model.AuditInfo;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ExportApiCRDUseCaseTest {

    private final ExportApiCRDUseCase useCase = new ExportApiCRDUseCase(new ApiCRDExportDomainServiceInMemory());

    @Test
    void should_export_as_a_crd() {
        var input = new ExportApiCRDUseCase.Input(ApiCRDFixtures.API_ID, IDExportStrategy.ALL, AuditInfo.builder().build());
        var output = useCase.execute(input);
        var spec = output.spec();
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(spec.getId()).isEqualTo(ApiCRDFixtures.API_ID);
            soft.assertThat(spec.getCrossId()).isEqualTo(ApiCRDFixtures.API_CROSS_ID);
            soft.assertThat(spec.getName()).isEqualTo(ApiCRDFixtures.API_NAME);
            soft.assertThat(spec.getPlans()).hasSize(1);
            soft.assertThat(spec.getPlans()).containsKey(ApiCRDFixtures.PLAN_NAME);
            soft.assertThat(spec.getListeners()).hasSize(1);
            soft.assertThat(spec.getEndpointGroups()).hasSize(1);
        });
    }
}
