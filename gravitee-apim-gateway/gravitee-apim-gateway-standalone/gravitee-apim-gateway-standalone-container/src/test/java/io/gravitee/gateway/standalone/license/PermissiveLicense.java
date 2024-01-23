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
package io.gravitee.gateway.standalone.license;

import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.License;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PermissiveLicense implements License {

    @Override
    public @Nonnull String getReferenceType() {
        return REFERENCE_TYPE_PLATFORM;
    }

    @Override
    public @Nonnull String getReferenceId() {
        return REFERENCE_ID_PLATFORM;
    }

    @Override
    public String getTier() {
        return null;
    }

    @Override
    public @Nonnull Set<String> getPacks() {
        return Collections.emptySet();
    }

    @Override
    public @Nonnull Set<String> getFeatures() {
        return Collections.emptySet();
    }

    @Override
    public boolean isFeatureEnabled(String feature) {
        return true;
    }

    @Override
    public void verify() throws InvalidLicenseException {
        // Always consider the license valid.
    }

    @Override
    public Date getExpirationDate() {
        return null;
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    @Override
    public @Nonnull Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public @Nonnull Map<String, String> getRawAttributes() {
        return Collections.emptyMap();
    }
}
