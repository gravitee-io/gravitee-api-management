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
package io.gravitee.apim.gateway.tests.sdk.license;

import io.gravitee.node.api.license.License;

/**
 * A test {@link LicenseManager} that always returns a {@link UniverseTierLicense}.
 * Used for tests that require the "universe" license tier (e.g. API Products).
 * Automatically wired when the test class deploys API Products via
 * {@link io.gravitee.apim.gateway.tests.sdk.annotations.DeployApiProducts}.
 */
public class UniverseTierLicenseManager extends PermissiveLicenseManager {

    private static final UniverseTierLicense UNIVERSE_TIER_LICENSE = new UniverseTierLicense();

    @Override
    protected License getLicense() {
        return UNIVERSE_TIER_LICENSE;
    }
}
