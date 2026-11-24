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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.json.JsonDeserializer;
import io.gravitee.apim.core.resource.domain_service.ValidateResourceDomainService;
import io.gravitee.apim.core.validation.Validator.Error;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.ResourcePluginProvider;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Judges the resources a Guardian holds against the resource plugins the node installed.
 *
 * <p>Every enabled resource must be of an available type and carry a configuration that plugin's schema
 * accepts, as an API's resources must; a disabled one is not read. Two things an API does not check are
 * checked here:
 *
 * <ul>
 *   <li><strong>the type is available</strong> — installed, deployed by the node and enabled by the
 *       license of the organization, which is what the catalog reports as {@code deployed}. The shared
 *       service asks the plugin for a schema, gets {@code null} for a plugin the node does not have, and
 *       validates nothing, so a Guardian would deploy holding a resource the gateway cannot build;</li>
 *   <li><strong>no two enabled resources share a name</strong> — the gateway keeps the resources of a
 *       Guardian by name, so the second would silently replace the first and a guardrail policy naming it
 *       would be handed whichever loaded last.</li>
 * </ul>
 *
 * <p>That a step names a resource the Guardian holds is judged with the step, by
 * {@link ValidateGuardianDomainService}: the policy schema says which property holds the name.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateGuardianResourcesDomainService {

    private final ResourcePluginProvider resourcePluginProvider;
    private final EnvironmentCrudService environmentCrudService;
    private final ValidateResourceDomainService validateResource;
    private final JsonDeserializer jsonDeserializer;

    /**
     * @param environmentId the environment of the Guardian, whose organization decides which types its
     *     license enables
     * @param correction what to do with a resource whose configuration its plugin only accepts corrected
     * @return what is wrong with the resources, empty when nothing is
     */
    List<Error> validate(List<Resource> resources, String environmentId, Correction correction) {
        if (resources != null && resources.stream().anyMatch(Objects::isNull)) {
            return List.of(Error.severe("Guardian resources must not contain null entries."));
        }
        if (resources == null || resources.stream().noneMatch(Resource::isEnabled)) {
            return List.of();
        }
        var types = typesOf(environmentId);
        var names = new HashSet<String>();
        var sharedNames = new HashSet<String>();
        var errors = new ArrayList<Error>();
        for (var resource : resources) {
            if (!resource.isEnabled()) {
                continue;
            }
            // reported once per name, however many resources share it
            if (!names.add(resource.getName()) && sharedNames.add(resource.getName())) {
                errors.add(Error.severe("Resource name [%s] is used by more than one enabled resource.", resource.getName()));
            }
            // a stored resource may carry no type at all: only a write goes through the bean validation
            if (resource.getType() == null) {
                errors.add(Error.severe("Resource [%s] has no type.", resource.getName()));
                continue;
            }
            var available = types.get(resource.getType());
            if (available == null) {
                errors.add(
                    Error.severe(
                        "Resource [%s] references resource type [%s], which is not installed.",
                        resource.getName(),
                        resource.getType()
                    )
                );
                continue;
            }
            if (!available) {
                errors.add(
                    Error.severe(
                        "Resource [%s] references resource type [%s], which is not available: the node did not deploy it, or the license of the organization does not enable it.",
                        resource.getName(),
                        resource.getType()
                    )
                );
                continue;
            }
            validateConfigurationSchema(resource, errors, correction);
        }
        return errors;
    }

    /**
     * The resource types of the node, each with whether a Guardian of this environment may hold it: what the
     * catalog lists, read the way the catalog reads it, so the validation accepts exactly what it offers.
     */
    private Map<String, Boolean> typesOf(String environmentId) {
        var organizationId = environmentCrudService.get(environmentId).getOrganizationId();
        return resourcePluginProvider
            .findAll(organizationId)
            .stream()
            .collect(Collectors.toMap(PlatformPlugin::id, PlatformPlugin::deployed, (first, second) -> first));
    }

    /**
     * Judges one resource configuration against the schema of its plugin, the way a step's is judged
     * against its policy's. An absent configuration is judged as an empty object, so a schema with
     * required properties is answered to rather than skipped; when the plugin accepts it as that, nothing
     * was corrected and the resource is left without one, as a step is.
     */
    private void validateConfigurationSchema(Resource resource, List<Error> errors, Correction correction) {
        var sent = ConfigurationDocuments.orEmpty(resource.getConfiguration());
        var judged = ConfigurationDocuments.asObject(jsonDeserializer, sent);
        if (judged.isEmpty()) {
            // refused here rather than by the shared service, which accepts anything for a plugin that has
            // no schema: the gateway builds a resource from an object and from nothing else
            errors.add(Error.severe("Resource [%s] has an invalid configuration: it must be a JSON object.", resource.getName()));
            return;
        }
        String accepted;
        try {
            accepted = validateResource.validateAndSanitizeConfiguration(resource.getType(), sent);
        } catch (InvalidDataException e) {
            // the message carries the schema paths and keywords the validator reported, as for a step
            errors.add(
                Error.severe(
                    "Resource [%s] has an invalid configuration: %s",
                    resource.getName(),
                    ConfigurationDocuments.asSentence(
                        Objects.requireNonNullElse(e.getMessage(), "it does not match the schema of the resource type")
                    )
                )
            );
            return;
        }
        if (ConfigurationDocuments.isSame(jsonDeserializer, accepted, judged.get())) {
            return;
        }
        switch (correction) {
            case STORE -> resource.setConfiguration(accepted);
            case REFUSE -> errors.add(
                Error.severe(
                    "Resource [%s] has a configuration its plugin only accepts corrected: edit and save the Guardian to store the configuration the plugin accepts.",
                    resource.getName()
                )
            );
        }
    }
}
