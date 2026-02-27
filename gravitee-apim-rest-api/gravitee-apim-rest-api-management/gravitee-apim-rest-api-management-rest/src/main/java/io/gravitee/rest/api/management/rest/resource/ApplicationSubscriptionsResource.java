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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Application Subscriptions")
public class ApplicationSubscriptionsResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ParameterService parameterService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Subscribe to a plan", description = "User must have the APPLICATION_SUBSCRIPTION[CREATE] permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "Subscription successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.CREATE) })
    public Response createSubscriptionWithApplication(
        @Parameter(name = "plan", required = true) @NotNull @QueryParam("plan") String plan,
        @Parameter(name = "customApiKey") @QueryParam("customApiKey") String customApiKey,
        @Valid NewSubscriptionEntity newSubscriptionEntity
    ) {
        // If newSubscriptionEntity is null, initialize it
        if (newSubscriptionEntity == null) {
            newSubscriptionEntity = new NewSubscriptionEntity();
        }

        newSubscriptionEntity.setApplication(application);
        newSubscriptionEntity.setPlan(plan);

        SubscriptionEntity subscription = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity,
            customApiKey
        );
        return Response.created(URI.create("/applications/" + application + "/subscriptions/" + subscription.getId())).entity(subscription).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List subscriptions for the application",
        description = "User must have the APPLICATION_SUBSCRIPTION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Paged result of application's subscriptions",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageSubscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public PageSubscription getSubscriptionsForApplicationSubscription(
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("size") @DefaultValue("10") int size,
        @Parameter(
            name = "status",
            explode = Explode.FALSE,
            schema = @Schema(type = "array", implementation = SubscriptionStatus.class)
        ) @QueryParam("status") List<SubscriptionStatus> statuses,
        @QueryParam("api_key") String apiKey
    ) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApplication(application);
        subscriptionQuery.setStatuses(statuses);
        subscriptionQuery.setApiKey(apiKey);

        Page<SubscriptionEntity> subscriptions = subscriptionService.search(
            GraviteeContext.getExecutionContext(),
            subscriptionQuery,
            new PageableImpl(page, size)
        );

        PageSubscription pageSubscription = new PageSubscription();
        pageSubscription.setData(
            subscriptions.getContent().stream().map(this::convert).collect(Collectors.toList())
        );
        pageSubscription.setMetadata(subscriptionService.getMetadata(GraviteeContext.getExecutionContext(), subscriptions.getContent()).toMap());
        pageSubscription.setPage(new PageSubscription.PageItem());
        pageSubscription.getPage().setCurrent(subscriptions.getPageNumber());
        pageSubscription.getPage().setPerPage(subscriptions.getPageElements());
        pageSubscription.getPage().setSize((int) subscriptions.getPageElements());
        pageSubscription.getPage().setTotalElements(subscriptions.getTotalElements());

        return pageSubscription;
    }

    @GET
    @Path("/configuration")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get subscription configuration",
        description = "User must have the APPLICATION_SUBSCRIPTION[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Subscription configuration"
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Response getSubscriptionConfiguration() {
        boolean customApiKeyAllowed = parameterService.findAsBoolean(
            GraviteeContext.getExecutionContext(),
            Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
            ParameterReferenceType.ENVIRONMENT
        );
        return Response.ok(new SubscriptionConfigurationResponse(customApiKeyAllowed)).build();
    }

    @Path("{subscription}")
    public ApplicationSubscriptionResource getApplicationSubscriptionResource() {
        return resourceContext.getResource(ApplicationSubscriptionResource.class);
    }

    private Subscription convert(SubscriptionEntity subscriptionEntity) {
        Subscription subscription = new Subscription();

        subscription.setId(subscriptionEntity.getId());
        subscription.setApi(subscriptionEntity.getApi());
        subscription.setPlan(subscriptionEntity.getPlan());
        subscription.setProcessedAt(subscriptionEntity.getProcessedAt());
        subscription.setStatus(subscriptionEntity.getStatus());
        subscription.setProcessedBy(subscriptionEntity.getProcessedBy());
        subscription.setRequest(subscriptionEntity.getRequest());
        subscription.setReason(subscriptionEntity.getReason());
        subscription.setApplication(subscriptionEntity.getApplication());
        subscription.setStartingAt(subscriptionEntity.getStartingAt());
        subscription.setEndingAt(subscriptionEntity.getEndingAt());
        subscription.setCreatedAt(subscriptionEntity.getCreatedAt());
        subscription.setUpdatedAt(subscriptionEntity.getUpdatedAt());
        subscription.setSubscribedBy(subscriptionEntity.getSubscribedBy());
        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setConsumerStatus(subscriptionEntity.getConsumerStatus());

        return subscription;
    }

    private static class PageSubscription {
        private List<Subscription> data;
        private java.util.Map<String, Object> metadata;
        private PageItem page;

        public List<Subscription> getData() {
            return data;
        }

        public void setData(List<Subscription> data) {
            this.data = data;
        }

        public java.util.Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(java.util.Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public PageItem getPage() {
            return page;
        }

        public void setPage(PageItem page) {
            this.page = page;
        }

        private static class PageItem {
            private int current;
            private long perPage;
            private int size;
            private long totalElements;

            public int getCurrent() {
                return current;
            }

            public void setCurrent(int current) {
                this.current = current;
            }

            public long getPerPage() {
                return perPage;
            }

            public void setPerPage(long perPage) {
                this.perPage = perPage;
            }

            public int getSize() {
                return size;
            }

            public void setSize(int size) {
                this.size = size;
            }

            public long getTotalElements() {
                return totalElements;
            }

            public void setTotalElements(long totalElements) {
                this.totalElements = totalElements;
            }
        }
    }

    public static class SubscriptionConfigurationResponse {
        private boolean customApiKeyAllowed;

        public SubscriptionConfigurationResponse(boolean customApiKeyAllowed) {
            this.customApiKeyAllowed = customApiKeyAllowed;
        }

        public boolean isCustomApiKeyAllowed() {
            return customApiKeyAllowed;
        }

        public void setCustomApiKeyAllowed(boolean customApiKeyAllowed) {
            this.customApiKeyAllowed = customApiKeyAllowed;
        }
    }
}
