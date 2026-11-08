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
package io.gravitee.gamma.module.platform.rest;

import io.gravitee.gamma.module.platform.rest.resource.am.AmSettingsResource;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import org.springframework.stereotype.Component;

/**
 * Root resource returned by {@code PlatformModule.restResource()}.
 *
 * Org and env scoping is handled upstream by {@code GammaRootResource} which mounts modules under
 * {@code /organizations/{orgId}/modules/{moduleId}}, so this resource is reached at
 * {@code /organizations/{orgId}/modules/platform/...}.
 */
@Component
public class PlatformRootResource {

    @Context
    private ResourceContext resourceContext;

    @Path("/am")
    public AmSettingsResource am() {
        return resourceContext.getResource(AmSettingsResource.class);
    }
}
