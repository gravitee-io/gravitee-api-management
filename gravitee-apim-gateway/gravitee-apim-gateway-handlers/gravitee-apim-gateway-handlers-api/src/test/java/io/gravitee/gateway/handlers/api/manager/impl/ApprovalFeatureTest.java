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
package io.gravitee.gateway.handlers.api.manager.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApprovalFeatureTest {

    @Mock
    private LicenseManager licenseManager;

    @Mock
    private License license;

    @Test
    void should_be_enabled_for_a_licensed_organization_when_gamma_is_enabled() {
        when(licenseManager.getOrganizationLicenseOrPlatform("org-1")).thenReturn(license);
        when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(true);

        assertThat(new ApprovalFeature(licenseManager, true).isEnabledFor("org-1")).isTrue();
    }

    @Test
    void should_be_disabled_without_reading_the_license_when_gamma_is_disabled() {
        ApprovalFeature feature = new ApprovalFeature(licenseManager, false);

        assertThat(feature.isEnabledFor("org-1")).isFalse();
        verifyNoInteractions(licenseManager);
    }

    @Test
    void should_be_disabled_for_an_organization_whose_license_lacks_the_feature() {
        when(licenseManager.getOrganizationLicenseOrPlatform("org-1")).thenReturn(license);
        when(license.isFeatureEnabled("gamma-aim-module")).thenReturn(false);

        assertThat(new ApprovalFeature(licenseManager, true).isEnabledFor("org-1")).isFalse();
    }

    @Test
    void should_be_disabled_for_an_organization_without_license() {
        when(licenseManager.getOrganizationLicenseOrPlatform("org-1")).thenReturn(null);

        assertThat(new ApprovalFeature(licenseManager, true).isEnabledFor("org-1")).isFalse();
    }
}
