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
import static org.mockito.Mockito.when;

import inmemory.ParametersDomainServiceInMemory;
import io.gravitee.apim.core.console.model.ConsoleCustomization;
import io.gravitee.apim.core.console.model.ConsoleTheme;
import io.gravitee.apim.core.console.model.CtaConfiguration;
import io.gravitee.apim.core.license.domain_service.GraviteeLicenseDomainService;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetConsoleCustomizationUseCaseTest {

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private License license;

    private final ParametersDomainServiceInMemory parametersDomainServiceInMemory = new ParametersDomainServiceInMemory();

    private GetConsoleCustomizationUseCase useCase;

    @BeforeEach
    void setup() {
        when(licenseManager.getPlatformLicense()).thenReturn(license);
        useCase = new GetConsoleCustomizationUseCase(new GraviteeLicenseDomainService(licenseManager), parametersDomainServiceInMemory);
    }

    @Test
    void should_return_console_customization() {
        parametersDomainServiceInMemory.initWith(
            List.of(
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_TITLE.key()).value("title").build(),
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_FAVICON.key()).value("favicon").build(),
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_LOGO.key()).value("logo").build(),
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_THEME_MENUACTIVE.key()).value("menuActive").build(),
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_THEME_MENUBACKGROUND.key()).value("menuBackground").build(),
                Parameter.builder()
                    .key(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_CUSTOMEENTERPRISENAME.key())
                    .value("enterprise name")
                    .build(),
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TITLE.key()).value("cta title").build(),
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_HIDEDAYS.key()).value("false").build(),
                Parameter.builder()
                    .key(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALBUTTONLABEL.key())
                    .value("trial button label")
                    .build(),
                Parameter.builder().key(Key.CONSOLE_CUSTOMIZATION_THEME_CTACONFIGURATION_TRIALURL.key()).value("trial URL").build()
            )
        );

        when(license.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)).thenReturn(true);
        var res = useCase.execute();

        assertThat(res.consoleCustomization()).isNotNull();
        assertThat(res.consoleCustomization())
            .extracting(ConsoleCustomization::title, ConsoleCustomization::favicon, ConsoleCustomization::logo)
            .containsExactly("title", "favicon", "logo");
        assertThat(res.consoleCustomization().theme())
            .extracting(ConsoleTheme::menuBackground, ConsoleTheme::menuActive)
            .containsExactly("menuBackground", "menuActive");
        assertThat(res.consoleCustomization().ctaConfiguration())
            .extracting(
                CtaConfiguration::title,
                CtaConfiguration::customEnterpriseName,
                CtaConfiguration::hideDays,
                CtaConfiguration::trialButtonLabel,
                CtaConfiguration::trialURL
            )
            .containsExactly("cta title", "enterprise name", false, "trial button label", "trial URL");
    }

    @Test
    void should_return_console_customization_default_values() {
        parametersDomainServiceInMemory.reset();

        when(license.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)).thenReturn(true);

        var res = useCase.execute();

        assertThat(res.consoleCustomization()).isNotNull();
        assertThat(res.consoleCustomization())
            .extracting(ConsoleCustomization::title, ConsoleCustomization::favicon, ConsoleCustomization::logo)
            .containsExactly(null, null, null);
        assertThat(res.consoleCustomization().theme())
            .extracting(ConsoleTheme::menuBackground, ConsoleTheme::menuActive)
            .containsExactly(null, null);
        assertThat(res.consoleCustomization().ctaConfiguration())
            .extracting(
                CtaConfiguration::title,
                CtaConfiguration::customEnterpriseName,
                CtaConfiguration::hideDays,
                CtaConfiguration::trialButtonLabel,
                CtaConfiguration::trialURL
            )
            .containsExactly(null, null, true, null, null);
    }

    @Test
    void should_return_null_if_license_doesnt_allow_customization() {
        when(license.isFeatureEnabled(GraviteeLicenseDomainService.OEM_CUSTOMIZATION_FEATURE)).thenReturn(false);
        var res = useCase.execute();

        assertThat(res.consoleCustomization()).isNull();
    }
}
