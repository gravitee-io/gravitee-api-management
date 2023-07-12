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
package io.gravitee.rest.api.service.v4.mapper;

import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.v4.FlowService;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class GenericPlanMapperTest {

    @Mock
    private PlanMapper planMapper;

    @Mock
    private FlowService flowServiceV4;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private io.gravitee.rest.api.service.configuration.flow.FlowService flowService;

    private GenericPlanMapper genericPlanMapper;

    @Before
    public void before() {
        genericPlanMapper = new GenericPlanMapper(planMapper, flowServiceV4, planConverter, flowService);
    }

    @Test
    public void shouldCallV4PlanMapperWhenDefinitionVersionV4() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V4);

        Plan plan = new Plan();
        genericPlanMapper.toGenericPlan(api, plan);
        verify(planMapper).toEntity(plan, List.of());
    }

    @Test
    public void shouldCallV2ApiMapperWhenV2DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V2);

        Plan plan = new Plan();
        genericPlanMapper.toGenericPlan(api, plan);
        verify(planConverter).toPlanEntity(plan, List.of());
    }

    @Test
    public void shouldCallV2ApiMapperWhenV1DefinitionVersion() {
        Api api = new Api();
        api.setDefinitionVersion(DefinitionVersion.V1);

        Plan plan = new Plan();
        genericPlanMapper.toGenericPlan(api, plan);
        verify(planConverter).toPlanEntity(plan, List.of());
    }

    @Test
    public void shouldCallV2ApiMapperWhenNoDefinitionVersion() {
        Api api = new Api();

        Plan plan = new Plan();
        genericPlanMapper.toGenericPlan(api, plan);
        verify(planConverter).toPlanEntity(plan, List.of());
    }
}
