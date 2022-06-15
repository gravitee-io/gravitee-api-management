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
package io.gravitee.repository.mock.management;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanRepositoryMock extends AbstractRepositoryMock<PlanRepository> {

    public PlanRepositoryMock() {
        super(PlanRepository.class);
    }

    @Override
    protected void prepare(PlanRepository planRepository) throws Exception {
        final Plan plan = mock(Plan.class);
        when(plan.getName()).thenReturn("Plan name");
        when(plan.getDescription()).thenReturn("Description for the new plan");
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getApi()).thenReturn("api1");
        when(plan.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(plan.getUpdatedAt()).thenReturn(parse("12/02/2016"));
        when(plan.getPublishedAt()).thenReturn(parse("13/02/2016"));
        when(plan.getClosedAt()).thenReturn(parse("14/02/2016"));
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getSecurity()).thenReturn(Plan.PlanSecurityType.KEY_LESS);
        when(plan.getGeneralConditions()).thenReturn("general_conditions");

        final Plan plan2 = mock(Plan.class);
        when(plan2.getId()).thenReturn("my-plan");
        when(plan2.getName()).thenReturn("Free plan");
        when(plan2.getDescription()).thenReturn("Description of the free plan");
        when(plan2.getApi()).thenReturn("api1");
        when(plan2.getSecurity()).thenReturn(Plan.PlanSecurityType.API_KEY);
        when(plan2.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(plan2.getType()).thenReturn(Plan.PlanType.API);
        when(plan2.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan2.getOrder()).thenReturn(2);
        when(plan2.getCreatedAt()).thenReturn(new Date(1506964899000L));
        when(plan2.getUpdatedAt()).thenReturn(new Date(1507032062000L));
        when(plan2.getPublishedAt()).thenReturn(new Date(1506878460000L));
        when(plan2.getClosedAt()).thenReturn(new Date(1507611600000L));
        when(plan2.getNeedRedeployAt()).thenReturn(new Date(1507611670000L));
        when(plan2.getCharacteristics()).thenReturn(asList("charac 1", "charac 2"));
        when(plan2.getExcludedGroups()).thenReturn(singletonList("grp1"));
        when(plan2.getTags()).thenReturn(new HashSet<>(asList("tag1", "tag2")));
        when(plan2.isCommentRequired()).thenReturn(true);
        when(plan2.getCommentMessage()).thenReturn("What is your project code?");
        when(plan2.getSelectionRule()).thenReturn(null);
        when(plan2.getGeneralConditions()).thenReturn("GCU-my-plan");

        final Plan planOAuth2 = mock(Plan.class);
        when(planOAuth2.getName()).thenReturn("Plan oauth2 name");
        when(planOAuth2.getDescription()).thenReturn("Description for the new oauth2 plan");
        when(planOAuth2.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planOAuth2.getType()).thenReturn(Plan.PlanType.API);
        when(planOAuth2.getApi()).thenReturn("my-api");
        when(planOAuth2.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(planOAuth2.getUpdatedAt()).thenReturn(parse("12/02/2016"));
        when(planOAuth2.getPublishedAt()).thenReturn(parse("13/02/2016"));
        when(planOAuth2.getClosedAt()).thenReturn(parse("14/02/2016"));
        when(planOAuth2.getStatus()).thenReturn(Plan.Status.STAGING);
        when(planOAuth2.getSecurity()).thenReturn(Plan.PlanSecurityType.OAUTH2);
        when(planOAuth2.getSecurityDefinition())
            .thenReturn("{\"extractPayload\":false,\"checkRequiredScopes\":false,\"requiredScopes\":[],\"oauthResource\":\"OAuth\"}");
        when(planOAuth2.isCommentRequired()).thenReturn(true);

        final Plan createdPlanOAuth2 = mock(Plan.class);
        when(createdPlanOAuth2.getId()).thenReturn("plan-oauth2");
        when(createdPlanOAuth2.getName()).thenReturn("oauth2");
        when(createdPlanOAuth2.getDescription()).thenReturn("Description of oauth2");
        when(createdPlanOAuth2.getValidation()).thenReturn(Plan.PlanValidationType.MANUAL);
        when(createdPlanOAuth2.getType()).thenReturn(Plan.PlanType.API);
        when(createdPlanOAuth2.getApi()).thenReturn("4e0db366-f772-4489-8db3-66f772b48989");
        when(createdPlanOAuth2.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(createdPlanOAuth2.getUpdatedAt()).thenReturn(parse("12/02/2016"));
        when(createdPlanOAuth2.getStatus()).thenReturn(Plan.Status.STAGING);
        when(createdPlanOAuth2.getOrder()).thenReturn(0);
        when(createdPlanOAuth2.getExcludedGroups()).thenReturn(singletonList("7c546c6b-2f2f-4487-946c-6b2f2f648784"));
        when(createdPlanOAuth2.getSecurity()).thenReturn(Plan.PlanSecurityType.OAUTH2);
        when(createdPlanOAuth2.getSecurityDefinition())
            .thenReturn("{\"extractPayload\":false,\"checkRequiredScopes\":false,\"requiredScopes\":[],\"oauthResource\":\"OAuth\"}");
        when(createdPlanOAuth2.getDefinition())
            .thenReturn(
                "{  \"/\" : [ {    \"methods\" : [ \"GET\", \"POST\", \"PUT\", \"DELETE\", \"HEAD\", \"PATCH\", \"OPTIONS\", \"TRACE\", \"CONNECT\" ],    \"resource-filtering\" : {\"whitelist\":[{\"pattern\":\"/**\",\"methods\":[\"GET\"]}]},    \"enabled\" : true  } ]}"
            );
        when(createdPlanOAuth2.isCommentRequired()).thenReturn(true);
        when(createdPlanOAuth2.getSelectionRule()).thenReturn("#context.attributes['jwt'].claims['iss'] == 'toto'");

        final Plan updatedPlan = mock(Plan.class);
        when(updatedPlan.getId()).thenReturn("updated-plan");
        when(updatedPlan.getName()).thenReturn("New plan");
        when(updatedPlan.getDescription()).thenReturn("New description");
        when(updatedPlan.getTags()).thenReturn(singleton("tag1"));

        final Plan updatedOauth2Plan = mock(Plan.class);
        when(updatedOauth2Plan.getId()).thenReturn("updated-plan-oauth2");
        when(updatedOauth2Plan.getName()).thenReturn("New oauth2 plan");
        when(updatedOauth2Plan.getDescription()).thenReturn("New oauth2 description");

        when(planRepository.create(any(Plan.class))).thenReturn(plan);

        when(planRepository.findById("new-plan")).thenReturn(of(plan));
        when(planRepository.findById("my-plan")).thenReturn(of(plan2));
        when(planRepository.findById("new-oauth2-plan")).thenReturn(of(planOAuth2));
        when(planRepository.findById("plan-oauth2")).thenReturn(of(createdPlanOAuth2));
        when(planRepository.findById("updated-plan")).thenReturn(of(updatedPlan));
        when(planRepository.findById("updated-plan-oauth2")).thenReturn(of(updatedOauth2Plan));

        when(planRepository.findById("stores")).thenReturn(Optional.empty());

        when(planRepository.findByApi("api1")).thenReturn(new HashSet<>(asList(plan, plan2)));

        when(planRepository.findByApis(Arrays.asList("api1", "4e0db366-f772-4489-8db3-66f772b48989")))
            .thenReturn(asList(plan, plan2, createdPlanOAuth2));

        when(planRepository.findById("unknown")).thenReturn(empty());

        when(planRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(planRepository.findByIdIn(List.of("my-plan", "unknown-id"))).thenReturn(Set.of(plan2));
    }
}
