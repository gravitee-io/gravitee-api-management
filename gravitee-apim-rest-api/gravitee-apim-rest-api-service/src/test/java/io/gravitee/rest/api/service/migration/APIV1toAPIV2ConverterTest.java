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
package io.gravitee.rest.api.service.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PolicyEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.jackson.ser.api.ApiCompositeSerializer;
import io.gravitee.rest.api.service.spring.ServiceConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class APIV1toAPIV2ConverterTest {

    private ObjectMapper objectMapper;
    private Set<PolicyEntity> policies;
    private APIV1toAPIV2Converter cut;

    @Before
    public void setUp() throws Exception {
        ServiceConfiguration serviceConfiguration = new ServiceConfiguration();
        objectMapper = serviceConfiguration.objectMapper();

        ApiCompositeSerializer apiSerializer = (ApiCompositeSerializer) serviceConfiguration.apiSerializer();
        apiSerializer.afterPropertiesSet();

        SimpleModule module = new SimpleModule();
        module.addSerializer(ApiEntity.class, apiSerializer);
        objectMapper.registerModule(module);

        policies = loadSet("/io/gravitee/rest/api/service/migration/policies.json", PolicyEntity.class);

        cut = new APIV1toAPIV2Converter();
    }

    @Test
    public void apiWithPathsAndPlans() throws Exception {
        ApiEntity toMigrate = load("/io/gravitee/rest/api/service/migration/api-withPathsAndPlans.json", ApiEntity.class);
        ApiEntity expected = load("/io/gravitee/rest/api/service/migration/api-withPathsAndPlans-migrated.json", ApiEntity.class);
        Set<PlanEntity> planEntities = loadSet("/io/gravitee/rest/api/service/migration/plans.json", PlanEntity.class);

        ApiEntity actual = cut.migrateToV2(toMigrate, policies, planEntities);

        assertEqualsApiEntity(expected, actual);
    }

    @Test
    public void apiWithMultiplePoliciesPerStep() throws Exception {
        ApiEntity toMigrate = load("/io/gravitee/rest/api/service/migration/api-withMultiplePolicies.json", ApiEntity.class);
        ApiEntity expected = load("/io/gravitee/rest/api/service/migration/api-withMultiplePolicies-migrated.json", ApiEntity.class);

        ApiEntity actual = cut.migrateToV2(toMigrate, policies, Collections.emptySet());

        assertEqualsApiEntity(expected, actual);
    }

    @Test
    public void apiWithOnlyPaths() throws Exception {
        ApiEntity toMigrate = load("/io/gravitee/rest/api/service/migration/api-withPathsAndPlans.json", ApiEntity.class);
        ApiEntity expected = load("/io/gravitee/rest/api/service/migration/api-withOnlyPaths-migrated.json", ApiEntity.class);

        ApiEntity actual = cut.migrateToV2(toMigrate, policies, Collections.emptySet());

        assertEqualsApiEntity(expected, actual);
    }

    @Test
    public void apiWithOnlyPlans() throws Exception {
        ApiEntity toMigrate = load("/io/gravitee/rest/api/service/migration/api-withOnlyPlans.json", ApiEntity.class);
        ApiEntity expected = load("/io/gravitee/rest/api/service/migration/api-withOnlyPlans-migrated.json", ApiEntity.class);
        Set<PlanEntity> planEntities = loadSet("/io/gravitee/rest/api/service/migration/plans.json", PlanEntity.class);

        ApiEntity actual = cut.migrateToV2(toMigrate, policies, planEntities);

        assertEqualsApiEntity(expected, actual);
    }

    private void assertEqualsApiEntity(ApiEntity expected, ApiEntity actual) {
        assertThat(actual).isEqualTo(expected);
        assertThat(expected.getFlowMode()).isEqualTo(FlowMode.BEST_MATCH);
        assertThat(actual.getPaths()).isEmpty();
        assertThat(actual.getFlows()).hasSameSizeAs(expected.getFlows());
        for (int i = 0; i < actual.getFlows().size(); i++) {
            assertThat(actual.getFlows().get(i)).isEqualToComparingFieldByFieldRecursively(expected.getFlows().get(i));
        }
        assertThat(actual.getPlans()).hasSameSizeAs(expected.getPlans());

        List<PlanEntity> actualPlans = actual
            .getPlans()
            .stream()
            .sorted(Comparator.comparing(PlanEntity::getId))
            .collect(Collectors.toList());
        List<PlanEntity> expectedPlans = expected
            .getPlans()
            .stream()
            .sorted(Comparator.comparing(PlanEntity::getId))
            .collect(Collectors.toList());

        for (int i = 0; i < actualPlans.size(); i++) {
            assertThat(actualPlans.get(i))
                .isEqualToComparingOnlyGivenFields(
                    expectedPlans.get(i),
                    "id",
                    "name",
                    "security",
                    "securityDefinition",
                    "api",
                    "selectionRule",
                    "status",
                    "tags",
                    "validation",
                    "description",
                    "type"
                );

            assertThat(actualPlans.get(i).getPaths()).isEmpty();

            assertThat(actualPlans.get(i).getFlows()).hasSameSizeAs(expectedPlans.get(i).getFlows());
            for (int j = 0; j < actualPlans.get(i).getFlows().size(); j++) {
                assertThat(actualPlans.get(i).getFlows().get(j))
                    .isEqualToComparingFieldByFieldRecursively(expectedPlans.get(i).getFlows().get(j));
            }
        }
    }

    private <T> Set<T> loadSet(String resource, Class<T> type) throws IOException {
        return objectMapper.readValue(read(resource), objectMapper.getTypeFactory().constructCollectionType(Set.class, type));
    }

    private <T> T load(String resource, Class<T> type) throws IOException {
        return objectMapper.readValue(read(resource), type);
    }

    private InputStream read(String resource) throws IOException {
        return this.getClass().getResourceAsStream(resource);
    }
}
