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

import io.gravitee.common.service.AbstractService;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.NonNull;

public class PermissiveLicenseManager extends AbstractService<LicenseManager> implements LicenseManager {

    private static final PermissiveLicense PERMISSIVE_LICENSE = new PermissiveLicense();

    @Override
    public void registerOrganizationLicense(@NonNull String organizationId, License license) {
        // Nothing to register as PermissiveLicenseManager always return a PermissiveLicense.
    }

    @Override
    public void registerPlatformLicense(License license) {
        // Nothing to register as PermissiveLicenseManager always return a PermissiveLicense.
    }

    @Nullable
    @Override
    public License getOrganizationLicense(String organizationId) {
        return PERMISSIVE_LICENSE;
    }

    @Override
    public @NonNull License getOrganizationLicenseOrPlatform(String organizationId) {
        return PERMISSIVE_LICENSE;
    }

    @NonNull
    @Override
    public License getPlatformLicense() {
        return PERMISSIVE_LICENSE;
    }

    @Override
    public void validatePluginFeatures(String organizationId, Collection<Plugin> plugins) throws ForbiddenFeatureException {
        // Always consider the plugin features are valid.
    }

    @Override
    public void onLicenseExpires(Consumer<License> expirationListener) {
        // Don't do anything as there is no license pollInterval.
    }
}
