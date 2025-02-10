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
package io.gravitee.rest.api.service.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.api.ApiCRDEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlanAdapterTest {

    @Nested
    class PlanSecurityType_2_PlanSecurity {

        @ParameterizedTest
        @ValueSource(strings = { "key-less", "api-key", "oauth2", "jwt", "mtls" })
        void goodValue(String type) {
            PlanSecurity input = PlanSecurity.builder().type(type).build();
            var map = PlanAdapter.INSTANCE.map(input);
            assertThat(map).isNotNull();
        }

        @Test
        void badValue() {
            PlanSecurity input = PlanSecurity.builder().type("bad").build();
            Throwable throwable = catchThrowable(() -> PlanAdapter.INSTANCE.map(input));
            assertThat(throwable).isNotNull();
        }
    }

    @Nested
    class PlanEntity_2_GenericPlanEntity {

        static String ID = "id";

        @ParameterizedTest
        @MethodSource
        void mapping(GenericPlanEntity input) {
            var map = PlanAdapter.INSTANCE.map(input);
            assertThat(map.getId()).isEqualTo(ID);
        }

        static Stream<Arguments> mapping() {
            return Stream.of(
                arguments(PlanEntity.builder().id(ID).build()),
                arguments(io.gravitee.rest.api.model.v4.plan.PlanEntity.builder().id(ID).build()),
                arguments(NativePlanEntity.builder().id(ID).build())
            );
        }

        @Test
        void notManagedType() {
            ApiCRDEntity.PlanEntity input = ApiCRDEntity.PlanEntity.builder().build();
            var map = PlanAdapter.INSTANCE.map(input);
            assertThat(map).isNull();
        }

        @Test
        void null2null() {
            var map = PlanAdapter.INSTANCE.map((GenericPlanEntity) null);
            assertThat(map).isNull();
        }
    }
}
