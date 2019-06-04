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
package io.gravitee.rest.api.portal.rest.resource;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.UpdateApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import io.gravitee.rest.api.portal.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.portal.rest.mapper.PlanMapper;
import io.gravitee.rest.api.portal.rest.model.Application;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;
    
    @Inject
    private ApplicationService applicationService;
    
    @Inject
    private PlanService planService;
    
    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private ApplicationMapper applicationMapper;

    @Inject
    private PlanMapper planMapper;
    
    private static final String INCLUDE_ANALYTICS = "analytics";
    private static final String INCLUDE_LOGS = "logs";
    private static final String INCLUDE_MEMBERS = "members";
    private static final String INCLUDE_METRICS = "metrics";
    private static final String INCLUDE_NOTIFICATIONS = "notifications";
    private static final String INCLUDE_PLANS = "plans";

    
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteApplicationByApplicationId(@PathParam("applicationId") String applicationId) {
        applicationService.archive(applicationId);
        return Response
                .noContent()
                .build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicationByApplicationId(@PathParam("applicationId") String applicationId, @QueryParam("include") List<String> include) {
        Application application = applicationMapper.convert(applicationService.findById(applicationId));
        
        // APIPortal : include datas
        if(include.contains(INCLUDE_ANALYTICS)) {

        }
        if(include.contains(INCLUDE_LOGS)) {
            
        }
        if(include.contains(INCLUDE_MEMBERS)) {
            
        }
        if(include.contains(INCLUDE_METRICS)) {
            
        }
        if(include.contains(INCLUDE_NOTIFICATIONS)) {
            
        }
        if(include.contains(INCLUDE_PLANS)) {
            List<Plan> plans = subscriptionService.findByApplicationAndPlan(applicationId, null).stream()
                .map(subscription -> planService.findById(subscription.getPlan()))
                .map(planMapper::convert)
                .distinct()
                .collect(Collectors.toList())
            ;
            application.setPlans(plans);
        }
        
        return Response
                .ok(addApplicationLinks(application))
                .build()
                ;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateApplicationByApplicationId(@PathParam("applicationId") String applicationId, @Valid Application application) {
        
        if(!application.getId().equalsIgnoreCase(applicationId)) {
            return Response
                     .status(Response.Status.BAD_REQUEST)
                     .entity("'applicationId' is not the same that the application in payload")
                     .build();
        }
        
        ApplicationEntity appEntity = applicationService.findById(applicationId);

        if(!getAuthenticatedUser().equals(appEntity.getPrimaryOwner().getId())) {
            throw new ForbiddenAccessException();
        }
        
        UpdateApplicationEntity updateApplicationEntity = new UpdateApplicationEntity();
        updateApplicationEntity.setDescription(application.getDescription());
        updateApplicationEntity.setGroups(new HashSet<String>(application.getGroups()));
        updateApplicationEntity.setName(application.getName());
        if(application.getSettings() != null) {
            ApplicationSettings settings = new ApplicationSettings();
            if(application.getSettings().getApp() != null) {
                SimpleApplicationSettings sas = new SimpleApplicationSettings();
                sas.setClientId(application.getSettings().getApp().getClientId());
                sas.setType(application.getSettings().getApp().getType());
                settings.setApp(sas);
            } else if(application.getSettings().getOauth() != null) {
                OAuthClientSettings oacs = new OAuthClientSettings();
                oacs.setApplicationType(application.getSettings().getOauth().getApplicationType());
                oacs.setClientId(application.getSettings().getOauth().getClientId());
                oacs.setClientSecret(application.getSettings().getOauth().getClientSecret());
                oacs.setClientUri(application.getSettings().getOauth().getClientUri());
                oacs.setGrantTypes(application.getSettings().getOauth().getGrantTypes());
                oacs.setLogoUri(application.getSettings().getOauth().getLogoUri());
                oacs.setRedirectUris(application.getSettings().getOauth().getRedirectUris());
                oacs.setRenewClientSecretSupported(application.getSettings().getOauth().getRenewClientSecretSupported().booleanValue());
                oacs.setResponseTypes(application.getSettings().getOauth().getResponseTypes());
                settings.setoAuthClient(oacs);
            }
            updateApplicationEntity.setSettings(settings);
        }
        
        Application updatedApp = applicationMapper.convert(applicationService.update(applicationId, updateApplicationEntity));
        return Response
                .ok(addApplicationLinks(updatedApp))
                .build();
    }
   
    
//    APIPortal: /picture ?
//    @GET
//    @Path("/picture")
//    @Produces({MediaType.WILDCARD, MediaType.APPLICATION_JSON})
//    public Response getApplicationPictureByApplicationId(@PathParam("applicationId") String applicationId) {
//        return delegate.getApplicationPictureByApplicationId(applicationId, securityContext);
//    }
//    
//    @PUT
//    @Path("/picture")
//    @Consumes(MediaType.WILDCARD)
//    @Produces({ MediaType.WILDCARD, MediaType.APPLICATION_JSON })
//    public Response updateApplicationPictureByApplicationId(@PathParam("applicationId") String applicationId, File body) {
//        return delegate.updateApplicationPictureByApplicationId(applicationId, body, securityContext);
//    }


    @POST
    @Path("/_renew_secret")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renewApplicationSecret(@PathParam("applicationId") String applicationId) {
        
        Application renwedApplication = applicationMapper.convert(applicationService.renewClientSecret(applicationId));

        String basePath = uriInfo.getAbsolutePathBuilder().build().toString();
        basePath = basePath.replaceFirst("/_renew_secret", "");
        
        return Response
                .ok(addApplicationLinks(renwedApplication, basePath))
                .build()
                ;
    }
    
    private Application addApplicationLinks(Application application) {
        String basePath = uriInfo.getAbsolutePathBuilder().build().toString();
        return addApplicationLinks(application, basePath);
    }
    
    private Application addApplicationLinks(Application application, String basePath) {
        return application.links(applicationMapper.computeApplicationLinks(basePath));
    }
    
    @Path("members")
    public ApplicationMembersResource getApplicationMembersResource() {
        return resourceContext.getResource(ApplicationMembersResource.class);
    }
 
//    APIPortal: @Path("notifications")
//    public ApplicationNotificationsResource getApplicationNotificationsResource() {
//        return resourceContext.getResource(ApplicationNotificationsResource.class);
//    }    
    
//  APIPortal: Logs, metrics, analytics

}
