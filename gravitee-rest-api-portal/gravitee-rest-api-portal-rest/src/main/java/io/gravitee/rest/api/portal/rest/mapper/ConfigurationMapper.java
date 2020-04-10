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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.PortalConfigEntity;
import io.gravitee.rest.api.model.PortalConfigEntity.*;
import io.gravitee.rest.api.model.PortalConfigEntity.Analytics;
import io.gravitee.rest.api.model.PortalConfigEntity.Application;
import io.gravitee.rest.api.model.PortalConfigEntity.Application.ApplicationTypes;
import io.gravitee.rest.api.model.PortalConfigEntity.Plan;
import io.gravitee.rest.api.model.PortalConfigEntity.Portal.PortalRating;
import io.gravitee.rest.api.model.PortalConfigEntity.Portal.PortalRating.RatingComment;
import io.gravitee.rest.api.model.PortalConfigEntity.Portal.PortalUploadMedia;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Enabled;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ConfigurationMapper {

    public ConfigurationResponse convert(PortalConfigEntity configEntity) {
        ConfigurationResponse configuration = new ConfigurationResponse();
        configuration.setAnalytics(convert(configEntity.getAnalytics()));
        configuration.setApiReview(convert(configEntity.getApiReview().getEnabled()));
        configuration.setApplication(convert(configEntity.getApplication()));
        configuration.setAuthentication(convert(configEntity.getAuthentication()));
        configuration.setCompany(convert(configEntity.getCompany()));
        configuration.setDocumentation(convert(configEntity.getDocumentation()));
        configuration.setPlan(convert(configEntity.getPlan()));
        configuration.setPortal(convert(configEntity.getPortal(), configEntity.getApplication()));
        configuration.setScheduler(convert(configEntity.getScheduler()));
        return configuration;
    }

    private ConfigurationScheduler convert(Scheduler scheduler) {
        ConfigurationScheduler configuration = new ConfigurationScheduler();
        configuration.setNotificationsInSeconds(scheduler.getNotificationsInSeconds());
        return configuration;
    }

    private ConfigurationPortal convert(Portal portal, Application application) {
        ConfigurationPortal configuration = new ConfigurationPortal();
        configuration.setAnalytics(convert(portal.getAnalytics()));
        configuration.setApikeyHeader(portal.getApikeyHeader());
        configuration.setApis(convert(portal.getApis()));
        configuration.setEntrypoint(portal.getEntrypoint());
        configuration.setUploadMedia(convert(portal.getUploadMedia()));
        configuration.setRating(convert(portal.getRating()));
        configuration.setSupport(convert(portal.getSupport()));
        configuration.setTitle(portal.getTitle());
        configuration.setUserCreation(convert(portal.getUserCreation()));

        ApplicationTypes types = application.getTypes();
        if (!application.getRegistration().getEnabled() && !types.getSimpleType().isEnabled()
                || !types.getSimpleType().isEnabled() &&
                !types.getWebType().isEnabled() &&
                !types.getNativeType().isEnabled() &&
                !types.getBackendToBackendType().isEnabled() &&
                !types.getBrowserType().isEnabled()
        ) {
            configuration.setApplicationCreation(convert(false));
        } else {
            configuration.setApplicationCreation(convert(true));
        }

        return configuration;
    }

    private ConfigurationPortalRating convert(PortalRating rating) {
        ConfigurationPortalRating configuration = new ConfigurationPortalRating();
        configuration.setComment(convert(rating.getComment()));
        configuration.setEnabled(rating.isEnabled());
        return configuration;
    }

    private ConfigurationPortalRatingComment convert(RatingComment comment) {
        ConfigurationPortalRatingComment configuration = new ConfigurationPortalRatingComment();
        configuration.setMandatory(comment.isMandatory());
        return configuration;
    }

    private ConfigurationPortalMedia convert(PortalUploadMedia uploadMedia) {
        ConfigurationPortalMedia configuration = new ConfigurationPortalMedia();
        configuration.setEnabled(uploadMedia.getEnabled());
        configuration.setMaxSizeInBytes(uploadMedia.getMaxSizeInOctet());
        return configuration;
    }

    private ConfigurationPortalApis convert(PortalApis apis) {
        ConfigurationPortalApis configuration = new ConfigurationPortalApis();
        configuration.setApiHeaderShowTags(convert(apis.getApiHeaderShowTags()));
        configuration.setApiHeaderShowViews(convert(apis.getApiHeaderShowViews()));
        configuration.setTilesMode(convert(apis.getTilesMode()));
        configuration.setViewMode(convert(apis.getViewMode()));
        return configuration;
    }

    private ConfigurationPortalAnalytics convert(PortalAnalytics analytics) {
        ConfigurationPortalAnalytics configuration = new ConfigurationPortalAnalytics();
        configuration.setEnabled(analytics.isEnabled());
        configuration.setTrackingId(analytics.getTrackingId());
        return configuration;
    }

    private ConfigurationPlan convert(Plan plan) {
        ConfigurationPlan configuration = new ConfigurationPlan();
        configuration.setSecurity(convert(plan.getSecurity()));
        return configuration;
    }

    private ConfigurationPlanSecurity convert(PlanSecurity security) {
        ConfigurationPlanSecurity configuration = new ConfigurationPlanSecurity();
        configuration.setApikey(convert(security.getApikey()));
        configuration.setJwt(convert(security.getJwt()));
        configuration.setKeyless(convert(security.getKeyless()));
        configuration.setOauth2(convert(security.getOauth2()));
        return configuration;
    }

    private ConfigurationDocumentation convert(Documentation documentation) {
        ConfigurationDocumentation configuration = new ConfigurationDocumentation();
        configuration.setUrl(documentation.getUrl());
        return configuration;
    }

    private ConfigurationCompany convert(Company company) {
        ConfigurationCompany configuration = new ConfigurationCompany();
        configuration.setName(company.getName());
        return configuration;
    }

    private ConfigurationAuthentication convert(Authentication authentication) {
        ConfigurationAuthentication configuration = new ConfigurationAuthentication();
        configuration.setForceLogin(convert(authentication.getForceLogin()));
        configuration.setGithub(convert(authentication.getGithub()));
        configuration.setGoogle(convert(authentication.getGoogle()));
        configuration.setLocalLogin(convert(authentication.getLocalLogin()));
        configuration.setOauth2(convert(authentication.getOauth2()));
        return configuration;
    }

    private ConfigurationOAuth2Authentication convert(OAuth2Authentication oauth2) {
        ConfigurationOAuth2Authentication configuration = new ConfigurationOAuth2Authentication();
        configuration.setAuthorizationEndpoint(oauth2.getAuthorizationEndpoint());
        configuration.setClientId(oauth2.getClientId());
        configuration.setColor(oauth2.getColor());
        configuration.setName(oauth2.getName());
        configuration.setScope(oauth2.getScope());
        configuration.setUserLogoutEndpoint(oauth2.getUserLogoutEndpoint());
        return configuration;
    }

    private ConfigurationGoogleAuthentication convert(GoogleAuthentication google) {
        ConfigurationGoogleAuthentication configuration = new ConfigurationGoogleAuthentication();
        configuration.setClientId(google.getClientId());
        return configuration;
    }

    private ConfigurationGithubAuthentication convert(GithubAuthentication github) {
        ConfigurationGithubAuthentication configuration = new ConfigurationGithubAuthentication();
        configuration.setClientId(github.getClientId());
        return configuration;
    }

    private ConfigurationApplication convert(Application application) {
        ConfigurationApplication configuration = new ConfigurationApplication();
        configuration.setRegistration(convert(application.getRegistration().getEnabled()));
        configuration.setTypes(convert(application.getTypes()));
        return configuration;
    }

    private ConfigurationApplicationTypes convert(ApplicationTypes types) {
        ConfigurationApplicationTypes configuration = new ConfigurationApplicationTypes();
        configuration.setBackendToBackend(convert(types.getBackendToBackendType()));
        configuration.setBrowser(convert(types.getBrowserType()));
        configuration.setNative(convert(types.getNativeType()));
        configuration.setSimple(convert(types.getSimpleType()));
        configuration.setWeb(convert(types.getWebType()));

        return configuration;
    }

    private ConfigurationAnalytics convert(Analytics analytics) {
        ConfigurationAnalytics configuration = new ConfigurationAnalytics();
        configuration.setClientTimeout(analytics.getClientTimeout());
        return configuration;
    }

    private Enabled convert(Boolean enabled) {
        return new Enabled().enabled(enabled);
    }

    private Enabled convert(PortalConfigEntity.Enabled enabledEntity) {
        return new Enabled().enabled(enabledEntity.isEnabled());
    }
}
