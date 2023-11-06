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

import io.gravitee.apim.core.console.exceptions.CustomizationNotAllowedException;
import io.gravitee.apim.core.console.model.ConsoleCustomization;
import io.gravitee.apim.core.console.model.ConsoleTheme;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;

public class GetConsoleCustomizationUseCase {

    private final GraviteeLicenseDomainService licenseDomainService;

    public GetConsoleCustomizationUseCase(GraviteeLicenseDomainService licenseDomainService) {
        this.licenseDomainService = licenseDomainService;
    }

    public Output execute() {
        if (this.licenseDomainService.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)) {
            return new Output(
                ConsoleCustomization
                    .builder()
                    .title("OEM title")
                    .favicon("favicon.ico")
                    .theme(ConsoleTheme.builder().menuActive("#fff").menuBackground("#fff").build())
                    .build()
            );
        }
        throw new CustomizationNotAllowedException("Your license does not include OEM customization feature");
    }

    public record Output(ConsoleCustomization consoleCustomization) {}
}
