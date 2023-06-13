/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.filter;

import io.gravitee.rest.api.management.rest.security.GraviteeLicenseFeature;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.v4.GraviteeLicenseService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeLicenseFilter implements ContainerRequestFilter {

    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    private GraviteeLicenseService graviteeLicenseService;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        findRequiredGraviteeLicenseFeature().ifPresent(this::checkGraviteeLicenseFeature);
    }

    private void checkGraviteeLicenseFeature(GraviteeLicenseFeature requiredFeature) {
        var featureName = requiredFeature.value();
        if (!graviteeLicenseService.isFeatureEnabled(featureName)) {
            throw new ForbiddenFeatureException(featureName);
        }
    }

    private Optional<GraviteeLicenseFeature> findRequiredGraviteeLicenseFeature() {
        var annotation = resourceInfo.getResourceMethod().getAnnotation(GraviteeLicenseFeature.class);
        return Optional.ofNullable(annotation);
    }
}
