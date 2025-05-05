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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import org.junit.jupiter.api.Test;

public class PlanEntityMapperTest {

    @Test
    void testConvertV4ToPlanEntity_withJwt() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity input = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity security = new PlanSecurity();
        security.setType("jwt");
        input.setSecurity(security);

        PlanEntity result = PlanEntityMapper.INSTANCE.convertV4ToPlanEntity(input);

        assertThat(result).isNotNull();
        assertThat(result.getSecurity()).isEqualTo(PlanSecurityType.JWT);
    }

    @Test
    void testConvertV4ToPlanEntity_withAPIKEY() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity input = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity security = new PlanSecurity();
        security.setType("api-key");
        input.setSecurity(security);

        PlanEntity result = PlanEntityMapper.INSTANCE.convertV4ToPlanEntity(input);

        assertThat(result).isNotNull();
        assertThat(result.getSecurity()).isEqualTo(PlanSecurityType.API_KEY);
    }

    @Test
    void testConvertV4ToPlanEntity_withOUTH2() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity input = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity security = new PlanSecurity();
        security.setType("outh2");
        input.setSecurity(security);

        PlanEntity result = PlanEntityMapper.INSTANCE.convertV4ToPlanEntity(input);

        assertThat(result).isNotNull();
        assertThat(result.getSecurity()).isEqualTo(PlanSecurityType.OAUTH2);
    }

    @Test
    void testConvertV4ToPlanEntity_withKEYLESS() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity input = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity security = new PlanSecurity();
        security.setType("key-less");
        input.setSecurity(security);

        PlanEntity result = PlanEntityMapper.INSTANCE.convertV4ToPlanEntity(input);

        assertThat(result).isNotNull();
        assertThat(result.getSecurity()).isEqualTo(PlanSecurityType.KEY_LESS);
    }

    @Test
    void testConvertV4ToPlanEntity_withInvalidType() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity input = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity security = new PlanSecurity();
        security.setType("invalid");
        input.setSecurity(security);

        assertThatThrownBy(() -> PlanEntityMapper.INSTANCE.convertV4ToPlanEntity(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown status: invalid");
    }

    @Test
    void testConvertV4ToPlanEntity_withNullSecurity() {
        io.gravitee.rest.api.model.v4.plan.PlanEntity input = new io.gravitee.rest.api.model.v4.plan.PlanEntity(); // no security

        PlanEntity result = PlanEntityMapper.INSTANCE.convertV4ToPlanEntity(input);

        assertThat(result).isNotNull();
        assertThat(result.getSecurity()).isNull();
    }
}
