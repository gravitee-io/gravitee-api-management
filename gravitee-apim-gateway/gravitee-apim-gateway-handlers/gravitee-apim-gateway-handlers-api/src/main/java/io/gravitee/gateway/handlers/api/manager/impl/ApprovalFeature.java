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

import io.gravitee.node.api.license.LicenseManager;

/**
 * Whether human approvals run for an organization on this gateway: Gamma is enabled, and the organization is licensed
 * for them. The approval rules and the approval states are both kept only when it is.
 */
public class ApprovalFeature {

    static final String LICENSE_FEATURE = "gamma-aim-module";

    private final LicenseManager licenseManager;
    private final boolean gammaEnabled;

    public ApprovalFeature(LicenseManager licenseManager, boolean gammaEnabled) {
        this.licenseManager = licenseManager;
        this.gammaEnabled = gammaEnabled;
    }

    public boolean gammaEnabled() {
        return gammaEnabled;
    }

    public boolean isEnabledFor(String organizationId) {
        if (!gammaEnabled) {
            return false;
        }
        var license = licenseManager.getOrganizationLicenseOrPlatform(organizationId);
        return license != null && license.isFeatureEnabled(LICENSE_FEATURE);
    }
}
