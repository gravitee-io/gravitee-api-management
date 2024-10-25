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
package io.gravitee.rest.api.management.v2.rest.resource.installation;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
// FIXME: Kafka Gateway - remove this, it is a fake resource to help us quickly create a Native API
public class NativeEventsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EventRepository eventRepository;

    @Inject
    private EventLatestRepository eventLatestRepository;

    @Inject
    private EventService eventService;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = { RolePermissionAction.CREATE }) })
    public Response createNativeApiEvent(String apiDefinitionStr) {
        var executionContext = GraviteeContext.getExecutionContext();
        final EventEntity apiEvent;
        final NativeApi nativeApiDefinition;
        try {
            nativeApiDefinition = GraviteeJacksonMapper.getInstance().readValue(apiDefinitionStr, NativeApi.class);
        } catch (JsonProcessingException e) {
            return Response.status(400).entity(e.getMessage()).build();
        }
        try {
            apiEvent =
                eventService.createApiEvent(
                    executionContext,
                    Set.of(executionContext.getEnvironmentId()),
                    executionContext.getOrganizationId(),
                    EventType.PUBLISH_API,
                    aApi(nativeApiDefinition, executionContext.getEnvironmentId()),
                    Map.of()
                );
        } catch (JsonProcessingException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
        return Response.created(this.getLocationHeader(apiEvent.getId())).entity(apiEvent).build();
    }

    private Api aApi(NativeApi definition, String environmentId) throws JsonProcessingException {
        final String apiId = UUID.randomUUID().toString();
        final String crossId = UUID.randomUUID().toString();
        return Api
            .builder()
            .id(apiId)
            .crossId(crossId)
            .createdAt(new Date())
            .lifecycleState(LifecycleState.STARTED)
            .definitionVersion(DefinitionVersion.V4)
            .type(ApiType.NATIVE)
            .deployedAt(new Date())
            .environmentId(environmentId)
            .definition(
                GraviteeJacksonMapper
                    .getInstance()
                    .writeValueAsString(
                        definition != null
                            ? definition.toBuilder().id(apiId).build()
                            : NativeApi
                                .builder()
                                .id(apiId)
                                .definitionVersion(DefinitionVersion.V4)
                                .type(ApiType.NATIVE)
                                .name("Native API!")
                                .listeners(List.of(KafkaListener.builder().host("localhost").port(9092).build()))
                                .endpointGroups(
                                    List.of(
                                        NativeEndpointGroup
                                            .builder()
                                            .type("native-kafka")
                                            .name("default-native")
                                            .endpoints(
                                                List.of(
                                                    NativeEndpoint
                                                        .builder()
                                                        .type("native-kafka")
                                                        .name("default-native")
                                                        .configuration("{}")
                                                        .build()
                                                )
                                            )
                                            .build()
                                    )
                                )
                                .plans(
                                    Map.of(
                                        "10a07215-5369-4826-a072-1553695826bf",
                                        NativePlan
                                            .builder()
                                            .id("10a07215-5369-4826-a072-1553695826bf")
                                            .name("Default Keyless (UNSECURED)")
                                            .security(PlanSecurity.builder().type("key-less").build())
                                            .mode(PlanMode.STANDARD)
                                            .status(PlanStatus.PUBLISHED)
                                            .build()
                                    )
                                )
                                .build()
                    )
            )
            .build();
    }
}
