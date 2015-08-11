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
package io.gravitee.management.api.resource;

import io.gravitee.management.api.provider.ManagementExceptionMapper;
import io.gravitee.management.api.provider.UnrecognizedPropertyExceptionMapper;
import io.gravitee.management.api.resource.ApisResource;
import io.gravitee.management.api.resource.AuthenticatedUserResource;
import io.gravitee.management.api.resource.PolicyResource;
import io.gravitee.management.api.resource.UsersResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeApplication extends ResourceConfig {

    public GraviteeApplication() {
        register(AuthenticatedUserResource.class);
        register(ApisResource.class);
        register(UsersResource.class);
        register(PolicyResource.class);

        register(ManagementExceptionMapper.class);
        register(UnrecognizedPropertyExceptionMapper.class);

        register(JacksonFeature.class);
    }
}