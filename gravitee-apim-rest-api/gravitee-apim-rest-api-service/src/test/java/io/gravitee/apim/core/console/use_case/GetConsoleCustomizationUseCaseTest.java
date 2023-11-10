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
package io.gravitee.apim.core.console.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.console.exceptions.CustomizationNotAllowedException;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.node.api.license.NodeLicenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetConsoleCustomizationUseCaseTest {

    @Mock
    private NodeLicenseService nodeLicenseService;

    private GetConsoleCustomizationUseCase useCase;

    @BeforeEach
    void setup() {
        useCase = new GetConsoleCustomizationUseCase(new GraviteeLicenseDomainService(nodeLicenseService));
    }

    @Test
    void should_return_console_customization() {
        when(nodeLicenseService.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)).thenReturn(true);
        var res = useCase.execute();

        assertThat(res.consoleCustomization().title()).isEqualTo("Celigo");
    }

    @Test
    void should_return_null_if_license_doesnt_allow_customization() {
        when(nodeLicenseService.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)).thenReturn(false);
        var res = useCase.execute();

        assertThat(res.consoleCustomization()).isNull();
    }
}
