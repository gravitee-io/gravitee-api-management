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
package io.gravitee.gamma.rest.core.observability.dashboard.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardModuleTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "aim", "apim", "module-2", "a", "0123456789012345678901234567890123456789012345678901234567890123" })
    void should_accept_an_absent_module_or_a_well_formed_one(String module) {
        assertThatCode(() -> DashboardModule.requireValidOrAbsent(module)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "AIM", "a_im", "a.im", "aim ", "01234567890123456789012345678901234567890123456789012345678901234" })
    void should_reject_a_malformed_module(String module) {
        assertThatThrownBy(() -> DashboardModule.requireValidOrAbsent(module)).isInstanceOf(InvalidDashboardException.class);
    }
}
