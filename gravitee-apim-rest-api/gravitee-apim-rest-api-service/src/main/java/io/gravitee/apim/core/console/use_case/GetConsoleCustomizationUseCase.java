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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.console.model.ConsoleCustomization;
import io.gravitee.apim.core.console.model.ConsoleTheme;
import io.gravitee.apim.core.console.model.CtaConfiguration;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.apim.core.parameters.domain_service.ParametersDomainService;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.List;
import java.util.Map;

@UseCase
public class GetConsoleCustomizationUseCase {

    private final GraviteeLicenseDomainService licenseDomainService;
    private final ParametersDomainService parametersDomainService;
    private static final List<Key> CONSOLE_CUSTOMIZATION_KEYS = List.of(
        Key.CONSOLE_CUSTOMIZATION_TITLE,
        Key.CONSOLE_CUSTOMIZATION_FAVICON,
        Key.CONSOLE_CUSTOMIZATION_LOGO,
        Key.CONSOLE_CUSTOMIZATION_THEME_MENUACTIVE,
        Key.CONSOLE_CUSTOMIZATION_THEME_MENUBACKGROUND,
        Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_CUSTOMEENTERPRISENAME,
        Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TITLE,
        Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_HIDEDAYS,
        Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALBUTTONLABEL,
        Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALURL
    );

    public GetConsoleCustomizationUseCase(
        GraviteeLicenseDomainService licenseDomainService,
        ParametersDomainService parametersDomainService
    ) {
        this.licenseDomainService = licenseDomainService;
        this.parametersDomainService = parametersDomainService;
    }

    public Output execute() {
        if (this.licenseDomainService.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)) {
            Map<Key, String> parameters = parametersDomainService.getSystemParameters(CONSOLE_CUSTOMIZATION_KEYS);
            String hideDays = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_HIDEDAYS);

            return new Output(
                ConsoleCustomization.builder()
                    .title(parameters.get(Key.CONSOLE_CUSTOMIZATION_TITLE))
                    .favicon(parameters.get(Key.CONSOLE_CUSTOMIZATION_FAVICON))
                    .logo(parameters.get(Key.CONSOLE_CUSTOMIZATION_LOGO))
                    .theme(
                        ConsoleTheme.builder()
                            .menuActive(parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_MENUACTIVE))
                            .menuBackground(parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_MENUBACKGROUND))
                            .build()
                    )
                    .ctaConfiguration(
                        CtaConfiguration.builder()
                            .customEnterpriseName(parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_CUSTOMEENTERPRISENAME))
                            .title(parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TITLE))
                            .hideDays(null == hideDays || Boolean.parseBoolean(hideDays))
                            .trialButtonLabel(parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALBUTTONLABEL))
                            .trialURL(parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALURL))
                            .build()
                    )
                    .build()
            );
        }
        return new Output(null);
    }

    public record Output(ConsoleCustomization consoleCustomization) {}
}
