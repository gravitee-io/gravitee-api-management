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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.portal.rest.resource.ApplicationsOrderParam.ApplicationsOrder.NB_SUBSCRIPTIONS;
import static io.gravitee.rest.api.portal.rest.resource.ApplicationsOrderParam.ApplicationsOrder.NB_SUBSCRIPTIONS_DESC;
import static java.util.stream.Collectors.groupingBy;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.model.application.TlsSettings;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.ApplicationInput;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.TlsClientSettings;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.notification.ApplicationHook;
import io.gravitee.rest.api.service.notification.Hook;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationsResource extends AbstractResource<Application, String> {

    protected static final String METADATA_SUBSCRIPTIONS_KEY = "subscriptions";

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private FilteringService filteringService;

    @Inject
    private ApplicationMapper applicationMapper;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private SubscriptionMapper subscriptionMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.CREATE) })
    public Response createApplication(@Valid @NotNull(message = "Input must not be null.") ApplicationInput applicationInput) {
        NewApplicationEntity newApplicationEntity = new NewApplicationEntity();
        newApplicationEntity.setDescription(applicationInput.getDescription());
        newApplicationEntity.setDomain(applicationInput.getDomain());
        newApplicationEntity.setGroups(
            applicationInput.getGroups() != null ? new HashSet<>(applicationInput.getGroups()) : new HashSet<>()
        );
        newApplicationEntity.setName(applicationInput.getName());
        newApplicationEntity.setPicture(applicationInput.getPicture());

        final io.gravitee.rest.api.portal.rest.model.ApplicationSettings settings = applicationInput.getSettings();
        ApplicationSettings newApplicationEntitySettings = new ApplicationSettings();

        if (settings == null || (settings.getApp() == null && settings.getOauth() == null && settings.getTls() == null)) {
            newApplicationEntity.setSettings(newApplicationEntitySettings);
        } else {
            final io.gravitee.rest.api.portal.rest.model.SimpleApplicationSettings simpleAppInput = settings.getApp();
            if (simpleAppInput != null) {
                SimpleApplicationSettings sas = new SimpleApplicationSettings();
                sas.setClientId(simpleAppInput.getClientId());
                sas.setType(simpleAppInput.getType());
                newApplicationEntitySettings.setApp(sas);
            }

            final io.gravitee.rest.api.portal.rest.model.OAuthClientSettings oauthAppInput = settings.getOauth();
            if (oauthAppInput != null) {
                OAuthClientSettings ocs = new OAuthClientSettings();
                ocs.setApplicationType(oauthAppInput.getApplicationType());
                ocs.setGrantTypes(oauthAppInput.getGrantTypes());
                ocs.setRedirectUris(oauthAppInput.getRedirectUris());
                newApplicationEntitySettings.setOAuthClient(ocs);
            }

            final TlsClientSettings tlsAppInput = settings.getTls();
            if (tlsAppInput != null) {
                newApplicationEntitySettings.setTls(TlsSettings.builder().clientCertificate(tlsAppInput.getClientCertificate()).build());
            }
        }
        newApplicationEntity.setSettings(newApplicationEntitySettings);
        if (applicationInput.getApiKeyMode() != null) {
            newApplicationEntity.setApiKeyMode(ApiKeyMode.valueOf(applicationInput.getApiKeyMode().name()));
        } else {
            newApplicationEntity.setApiKeyMode(ApiKeyMode.UNSPECIFIED);
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApplicationEntity createdApplicationEntity = applicationService.create(
            executionContext,
            newApplicationEntity,
            getAuthenticatedUser()
        );

        return Response
            .created(this.getLocationHeader(createdApplicationEntity.getId()))
            .entity(applicationMapper.convert(executionContext, createdApplicationEntity, uriInfo))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_APPLICATION, acls = RolePermissionAction.READ) })
    public Response getApplications(
        @BeanParam PaginationParam paginationParam,
        @QueryParam("forSubscription") final boolean forSubscription,
        @QueryParam("order") @DefaultValue("name") final ApplicationsOrderParam applicationsOrderParam
    ) {
        if (!paginationParam.hasPagination()) {
            return getAllApplications(forSubscription, applicationsOrderParam);
        }
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Collection<String> applicationIds;
        if (forSubscription) {
            applicationIds =
                applicationService.findIdsByUserAndPermission(
                    executionContext,
                    getAuthenticatedUser(),
                    applicationsOrderParam.toSortable(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    RolePermissionAction.CREATE
                );
        } else {
            applicationIds =
                applicationService.findIdsByUser(executionContext, getAuthenticatedUser(), applicationsOrderParam.toSortable());
        }

        if (NB_SUBSCRIPTIONS_DESC.equals(applicationsOrderParam.getValue()) || NB_SUBSCRIPTIONS.equals(applicationsOrderParam.getValue())) {
            applicationIds =
                filteringService.getApplicationsOrderByNumberOfSubscriptions(
                    applicationIds,
                    applicationsOrderParam.getValue().isAsc ? Order.ASC : Order.DESC
                );
        }

        return createListResponse(executionContext, applicationIds, paginationParam);
    }

    private Response getAllApplications(boolean forSubscription, ApplicationsOrderParam applicationsOrderParam) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Collection<ApplicationListItem> applications;
        if (forSubscription) {
            applications =
                applicationService.findByUserAndPermission(
                    executionContext,
                    getAuthenticatedUser(),
                    applicationsOrderParam.toSortable(),
                    RolePermission.APPLICATION_SUBSCRIPTION,
                    RolePermissionAction.CREATE
                );
        } else {
            applications = applicationService.findByUser(executionContext, getAuthenticatedUser(), applicationsOrderParam.toSortable());
        }
        List<Application> applicationList = applications
            .stream()
            .map(applicationListItem -> applicationMapper.convert(executionContext, applicationListItem, uriInfo, false))
            .collect(Collectors.toList());

        return Response
            .ok(
                new DataResponse()
                    .data(applicationList)
                    .links(this.computePaginatedLinks(1, applicationList.size(), applicationList.size()))
            )
            .build();
    }

    @Override
    protected Map fillMetadata(ExecutionContext executionContext, Map metadata, List<String> pageContent) {
        final String userId = getAuthenticatedUser();

        Set<String> subscriptionApplicationIds = applicationService.findIdsByUserAndPermission(
            executionContext,
            userId,
            null,
            RolePermission.APPLICATION_SUBSCRIPTION,
            RolePermissionAction.READ
        );

        subscriptionApplicationIds.retainAll(pageContent);

        SubscriptionQuery query = new SubscriptionQuery();
        query.setApplications(subscriptionApplicationIds);
        query.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED));

        final Map<String, List<Subscription>> subscriptions = subscriptionService
            .search(executionContext, query)
            .stream()
            .map(subscriptionMapper::convert)
            .collect(groupingBy(Subscription::getApplication));
        if (!subscriptions.isEmpty()) {
            metadata.put(METADATA_SUBSCRIPTIONS_KEY, subscriptions);
        }
        return metadata;
    }

    @Override
    protected List<Application> transformPageContent(final ExecutionContext executionContext, List<String> pageContent) {
        if (pageContent.isEmpty()) {
            return Collections.emptyList();
        }

        Set<ApplicationListItem> applicationListItems = applicationService.findByIds(executionContext, pageContent);
        Comparator<String> orderingComparator = Comparator.comparingInt(pageContent::indexOf);

        return applicationListItems
            .stream()
            .map(applicationListItem -> {
                Application application = applicationMapper.convert(executionContext, applicationListItem, uriInfo);
                return addApplicationLinks(application);
            })
            .sorted((o1, o2) -> orderingComparator.compare(o1.getId(), o2.getId()))
            .collect(Collectors.toList());
    }

    private Application addApplicationLinks(Application application) {
        String basePath = uriInfo.getAbsolutePathBuilder().path(application.getId()).build().toString();
        return application.links(applicationMapper.computeApplicationLinks(basePath, application.getUpdatedAt()));
    }

    @GET
    @Path("/hooks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHooks() {
        return Response.ok(Arrays.stream(ApplicationHook.values()).toArray(Hook[]::new)).build();
    }

    @Path("{applicationId}")
    public ApplicationResource getApplicationResource() {
        return resourceContext.getResource(ApplicationResource.class);
    }
}
