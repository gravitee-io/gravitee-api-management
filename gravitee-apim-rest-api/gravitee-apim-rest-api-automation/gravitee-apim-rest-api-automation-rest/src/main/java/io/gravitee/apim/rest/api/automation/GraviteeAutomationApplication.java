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
package io.gravitee.apim.rest.api.automation;

import io.gravitee.apim.rest.api.automation.exception.mapping.HRIDNotFoundMapper;
import io.gravitee.apim.rest.api.automation.exception.mapping.ManagementExceptionMapper;
import io.gravitee.apim.rest.api.automation.exception.mapping.ValidationDomainMapper;
import io.gravitee.apim.rest.api.automation.resource.ApisResource;
import io.gravitee.apim.rest.api.automation.resource.ApplicationsResource;
import io.gravitee.apim.rest.api.automation.resource.EnvironmentResource;
import io.gravitee.apim.rest.api.automation.resource.EnvironmentsResource;
import io.gravitee.apim.rest.api.automation.resource.OpenAPIResource;
import io.gravitee.apim.rest.api.automation.resource.OrganizationResource;
import io.gravitee.apim.rest.api.automation.resource.SharedPolicyGroupsResource;
import io.gravitee.rest.api.management.v2.rest.provider.ObjectMapperResolver;
import jakarta.inject.Inject;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class GraviteeAutomationApplication extends ResourceConfig {

    @Inject
    public GraviteeAutomationApplication() {
        register(OpenAPIResource.class);

        register(OrganizationResource.class);
        register(EnvironmentsResource.class);
        register(EnvironmentResource.class);
        register(ApisResource.class);
        register(ApplicationsResource.class);
        register(SharedPolicyGroupsResource.class);

        register(ValidationDomainMapper.class);
        register(HRIDNotFoundMapper.class);
        register(ManagementExceptionMapper.class);

        register(ObjectMapperResolver.class);

        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }
}
