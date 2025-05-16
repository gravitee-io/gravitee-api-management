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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.service.v4.EntityConversionService;
import org.junit.jupiter.api.Test;

public class EntityConversionServiceImplTest {

    private final EntityConversionService service = new EntityConversionServiceImpl();

    @Test
    void testConvertV4ToPlanEntity_validPlanEntity2() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity input = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity security = new PlanSecurity();
        security.setType("api-key");
        input.setSecurity(security);

        PlanEntity result = service.convertV4ToPlanEntity(input);

        assertThat(result).isNotNull();
        assertThat(result.getSecurity()).isEqualTo(PlanSecurityType.API_KEY);
    }
}
