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
package io.gravitee.rest.api.service.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Synchronization Service should")
public class SynchronizationServiceTest {

    private SynchronizationService synchronizationService;

    @BeforeEach
    public void setup() {
        this.synchronizationService = new SynchronizationService();
    }

    @Nested
    @DisplayName("return `true` when")
    class ThruthySynchronizationTest {

        @Test
        public void shouldReturnTrueWhenApiIdsAreTheSame() {
            ApiEntity deployedEntity = new ApiEntity();
            deployedEntity.setId("deployedEntity");
            deployedEntity.setCrossId("deployedEntity");

            ApiEntity entityToDeploy = new ApiEntity();
            entityToDeploy.setId("entityToDeploy");
            entityToDeploy.setCrossId("entityToDeploy");

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenPlansAreTheSame() {
            ApiEntity deployedEntity = new ApiEntity();
            PlanEntity deployedPlan = new PlanEntity();
            deployedPlan.setId("deployedPlan");
            deployedPlan.setCrossId("deployedPlan");
            Flow deployedFlow = new Flow();
            deployedFlow.setName("deployedFlow");
            Step deployedStep = new Step();
            deployedStep.setConfiguration("{ \"configuration\": \"foobar\" }");
            deployedFlow.setPre(List.of(deployedStep));
            deployedFlow.setId("deployedFlow");
            deployedPlan.setFlows(List.of(deployedFlow));
            deployedEntity.setPlans(Set.of(deployedPlan));

            ApiEntity entityToDeploy = new ApiEntity();
            PlanEntity entityToDeployPlan = new PlanEntity();
            entityToDeployPlan.setId("entityToDeployPlan");
            entityToDeployPlan.setCrossId("entityToDeployPlan");
            Flow entityToDeployFlow = new Flow();
            entityToDeployFlow.setName("deployedFlow");
            Step entityToDeployStep = new Step();
            entityToDeployStep.setConfiguration("{ \"configuration\": \"foobar\" }");
            entityToDeployFlow.setPre(List.of(entityToDeployStep));
            entityToDeployFlow.setId("deployedFlow");
            entityToDeployPlan.setFlows(List.of(entityToDeployFlow));
            entityToDeploy.setPlans(Set.of(entityToDeployPlan));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenPlanFlowIdsAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            Flow deployedFlow = new Flow();
            deployedFlow.setId("deployedFlow");
            PlanEntity deployedPlan = new PlanEntity();
            deployedPlan.setFlows(List.of(deployedFlow));
            deployedEntity.setPlans(Set.of(deployedPlan));

            ApiEntity entityToDeploy = new ApiEntity();
            Flow entityToDeployFlow = new Flow();
            entityToDeployFlow.setId("entityToDeployFlow");
            PlanEntity entityToDeployPlan = new PlanEntity();
            entityToDeployPlan.setFlows(List.of(entityToDeployFlow));
            entityToDeploy.setPlans(Set.of(entityToDeployPlan));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenFlowIdsAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            Flow deployedFlow = new Flow();
            deployedFlow.setId("deployedFlow");
            deployedEntity.setFlows(List.of(deployedFlow));

            ApiEntity entityToDeploy = new ApiEntity();
            Flow entityToDeployFlow = new Flow();
            entityToDeployFlow.setId("entityToDeployFlow");
            entityToDeploy.setFlows(List.of(entityToDeployFlow));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenFlowPoliciesDescriptionAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            Flow deployedFlow = new Flow();
            Step deployedStep = new Step();
            deployedStep.setDescription("foobar");
            deployedFlow.setPre(List.of(deployedStep));
            deployedEntity.setFlows(List.of(deployedFlow));

            ApiEntity entityToDeploy = new ApiEntity();
            Flow entityToDeployFlow = new Flow();
            Step entityToDeployStep = new Step();
            entityToDeployStep.setDescription("check");
            entityToDeployFlow.setPre(List.of(entityToDeployStep));
            entityToDeploy.setFlows(List.of(entityToDeployFlow));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenPathPoliciesDescriptionAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            Rule deployedRule = new Rule();
            deployedRule.setDescription("foobar");
            deployedEntity.setPaths(Map.of("/", List.of(deployedRule)));

            ApiEntity entityToDeploy = new ApiEntity();
            Rule entityToDeployRule = new Rule();
            entityToDeployRule.setDescription("path");
            entityToDeploy.setPaths(Map.of("/", List.of(entityToDeployRule)));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenPathsAreTheSame() {
            ApiEntity deployedEntity = new ApiEntity();
            Rule deployedRule = new Rule();
            Policy deployedPolicy = new Policy();
            deployedPolicy.setName("foobar");
            deployedPolicy.setConfiguration("{ \"configuration\": \"foobar\" }");
            deployedRule.setPolicy(deployedPolicy);
            deployedEntity.setPaths(Map.of("/", List.of(deployedRule)));

            ApiEntity entityToDeploy = new ApiEntity();
            Rule entityToDeployRule = new Rule();
            Policy entityToDeployPolicy = new Policy();
            entityToDeployPolicy.setName("foobar");
            entityToDeployPolicy.setConfiguration("{ \"configuration\": \"foobar\" }");
            entityToDeployRule.setPolicy(entityToDeployPolicy);

            entityToDeploy.setPaths(Map.of("/", List.of(entityToDeployRule)));

            boolean isSynchronized = synchronizationService.check(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenPropertiesAreTheSame() {
            ApiEntity deployedEntity = new ApiEntity();
            Properties deployedProperties = new Properties();
            Property deployedProperty = new Property();
            deployedProperty.setValue("foobar");
            deployedProperty.setDynamic(true);
            deployedProperty.setEncrypted(false);
            deployedProperties.setProperties(List.of(deployedProperty));
            deployedEntity.setProperties(deployedProperties);

            ApiEntity entityToDeploy = new ApiEntity();
            Properties entityToDeployProperties = new Properties();
            Property entityToDeployProperty = new Property();
            entityToDeployProperty.setValue("foobar");
            entityToDeployProperty.setDynamic(true);
            entityToDeployProperty.setEncrypted(false);
            entityToDeployProperties.setProperties(List.of(entityToDeployProperty));
            entityToDeploy.setProperties(entityToDeployProperties);

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }

        @Test
        public void shouldReturnTrueWhenResourcesAreTheSame() {
            ApiEntity deployedEntity = new ApiEntity();
            Resource deployedResource = new Resource();
            deployedResource.setName("foobar");
            deployedEntity.setResources(List.of(deployedResource));

            ApiEntity entityToDeploy = new ApiEntity();
            Resource entityToDeployResource = new Resource();
            entityToDeployResource.setName("foobar");
            entityToDeploy.setResources(List.of(entityToDeployResource));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertTrue(isSynchronized);
        }
    }

    @Nested
    @DisplayName("return `false` when")
    class FalsySynchronizationTest {

        @Test
        public void shouldReturnFalseWhenFlowsAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            PlanEntity deployedPlan = new PlanEntity();
            deployedPlan.setId("deployedPlan");
            deployedPlan.setCrossId("deployedPlan");
            Flow deployedFlow = new Flow();
            deployedFlow.setName("deployedFlow");
            Step deployedStep = new Step();
            deployedStep.setConfiguration("{ \"configuration\": \"foobar\" }");
            deployedFlow.setPre(List.of(deployedStep));
            deployedFlow.setId("deployedFlow");
            deployedPlan.setFlows(List.of(deployedFlow));
            deployedEntity.setPlans(Set.of(deployedPlan));

            ApiEntity entityToDeploy = new ApiEntity();
            PlanEntity entityToDeployPlan = new PlanEntity();
            entityToDeployPlan.setId("entityToDeployPlan");
            entityToDeployPlan.setCrossId("entityToDeployPlan");
            Flow entityToDeployFlow = new Flow();
            entityToDeployFlow.setName("deployedFlow");
            Step entityToDeployStep = new Step();
            entityToDeployStep.setConfiguration("{ \"configuration\": \"updated\" }");
            entityToDeployFlow.setPre(List.of(entityToDeployStep));
            entityToDeployFlow.setId("deployedFlow");
            entityToDeployPlan.setFlows(List.of(entityToDeployFlow));
            entityToDeploy.setPlans(Set.of(entityToDeployPlan));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertFalse(isSynchronized);
        }

        @Test
        public void shouldReturnFalseWhenPathsAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            Rule deployedRule = new Rule();
            Policy deployedPolicy = new Policy();
            deployedPolicy.setName("foobar");
            deployedPolicy.setConfiguration("{ \"configuration\": \"foobar\" }");
            deployedRule.setPolicy(deployedPolicy);
            deployedEntity.setPaths(Map.of("/", List.of(deployedRule)));

            ApiEntity entityToDeploy = new ApiEntity();
            Rule entityToDeployRule = new Rule();
            Policy entityToDeployPolicy = new Policy();
            entityToDeployPolicy.setName("check");
            entityToDeployPolicy.setConfiguration("{ \"configuration\": \"check\" }");
            entityToDeployRule.setPolicy(entityToDeployPolicy);

            entityToDeploy.setPaths(Map.of("/", List.of(entityToDeployRule)));

            boolean isSynchronized = synchronizationService.check(deployedEntity, entityToDeploy);

            assertFalse(isSynchronized);
        }

        @Test
        public void shouldReturnFalseWhenPropertiesAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            Properties deployedProperties = new Properties();
            Property deployedProperty = new Property();
            deployedProperty.setValue("foobar");
            deployedProperty.setDynamic(false);
            deployedProperty.setEncrypted(false);
            deployedProperties.setProperties(List.of(deployedProperty));
            deployedEntity.setProperties(deployedProperties);

            ApiEntity entityToDeploy = new ApiEntity();
            Properties entityToDeployProperties = new Properties();
            Property entityToDeployProperty = new Property();
            entityToDeployProperty.setValue("check");
            entityToDeployProperty.setDynamic(false);
            entityToDeployProperty.setEncrypted(false);
            entityToDeployProperties.setProperties(List.of(entityToDeployProperty));
            entityToDeploy.setProperties(entityToDeployProperties);

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertFalse(isSynchronized);
        }

        @Test
        public void shouldReturnFalseWhenResourcesAreDifferent() {
            ApiEntity deployedEntity = new ApiEntity();
            Resource deployedResource = new Resource();
            deployedResource.setName("deployedResource");
            deployedEntity.setResources(List.of(deployedResource));

            ApiEntity entityToDeploy = new ApiEntity();
            Resource entityToDeployResource = new Resource();
            entityToDeployResource.setName("entityToDeployResource");
            entityToDeploy.setResources(List.of(entityToDeployResource));

            boolean isSynchronized = synchronizationService.checkSynchronization(deployedEntity, entityToDeploy);

            assertFalse(isSynchronized);
        }
    }
}
