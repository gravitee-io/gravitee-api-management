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

import io.gravitee.apim.core.console.model.ConsoleCustomization;
import io.gravitee.apim.core.console.model.ConsoleTheme;
import io.gravitee.apim.core.console.model.CtaConfiguration;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;

public class GetConsoleCustomizationUseCase {

    private final GraviteeLicenseDomainService licenseDomainService;
    private final ParametersQueryService parametersQueryService;
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
        ParametersQueryService parametersQueryService
    ) {
        this.licenseDomainService = licenseDomainService;
        this.parametersQueryService = parametersQueryService;
    }

    public Output execute(Input input) {
        if (this.licenseDomainService.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)) {
            Map<Key, List<String>> parameters = parametersQueryService.findAll(
                input.executionContext,
                CONSOLE_CUSTOMIZATION_KEYS,
                ParameterReferenceType.SYSTEM
            );

            List<String> title = parameters.get(Key.CONSOLE_CUSTOMIZATION_TITLE);
            List<String> favicon = parameters.get(Key.CONSOLE_CUSTOMIZATION_FAVICON);
            List<String> logo = parameters.get(Key.CONSOLE_CUSTOMIZATION_LOGO);
            List<String> menuActive = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_MENUACTIVE);
            List<String> menuBackground = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_MENUBACKGROUND);
            List<String> customEnterpriseName = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_CUSTOMEENTERPRISENAME);
            List<String> ctaTitle = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TITLE);
            List<String> hideDays = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_HIDEDAYS);
            List<String> trialButtonLabel = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALBUTTONLABEL);
            List<String> trialURL = parameters.get(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALURL);

            return new Output(
                ConsoleCustomization
                    .builder()
                    .title(isNotEmpty(title) ? title.get(0) : null)
                    // The ParameterService, by default, uses ";" as the separator when splitting values.
                    // Since the Base64 encoding of icons may contain this separator, we need to join the array to reconstruct the default value accurately.
                    .favicon(isNotEmpty(favicon) ? String.join(";", favicon) : null)
                    .logo(isNotEmpty(logo) ? String.join(";", logo) : null)
                    .theme(
                        ConsoleTheme
                            .builder()
                            .menuActive(isNotEmpty(menuActive) ? menuActive.get(0) : null)
                            .menuBackground(isNotEmpty(menuBackground) ? menuBackground.get(0) : null)
                            .build()
                    )
                    .ctaConfiguration(
                        CtaConfiguration
                            .builder()
                            .customEnterpriseName(isNotEmpty(customEnterpriseName) ? customEnterpriseName.get(0) : null)
                            .title(isNotEmpty(ctaTitle) ? ctaTitle.get(0) : null)
                            .hideDays(!isNotEmpty(hideDays) || Boolean.parseBoolean(hideDays.get(0)))
                            .trialButtonLabel(isNotEmpty(trialButtonLabel) ? trialButtonLabel.get(0) : null)
                            .trialURL(isNotEmpty(trialURL) ? trialURL.get(0) : null)
                            .build()
                    )
                    .build()
            );
        }
        return new Output(null);
    }

    private boolean isNotEmpty(List<String> collection) {
        return collection != null && !collection.isEmpty();
    }

    public record Input(ExecutionContext executionContext) {}

    public record Output(ConsoleCustomization consoleCustomization) {}
}
