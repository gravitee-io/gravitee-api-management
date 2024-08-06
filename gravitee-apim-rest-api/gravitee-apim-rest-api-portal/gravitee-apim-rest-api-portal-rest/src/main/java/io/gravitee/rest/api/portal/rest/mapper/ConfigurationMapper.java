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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.settings.*;
import io.gravitee.rest.api.model.settings.PlanSettings;
import io.gravitee.rest.api.model.settings.PortalApplicationSettings;
import io.gravitee.rest.api.model.settings.PortalNext;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Enabled;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ConfigurationMapper {

    public ConfigurationResponse convert(PortalSettingsEntity portalConfigEntity, ConsoleSettingsEntity consoleSettingsEntity) {
        ConfigurationResponse configuration = new ConfigurationResponse();
        configuration.setAnalytics(convert(portalConfigEntity.getAnalytics()));
        configuration.setApiReview(convert(portalConfigEntity.getApiReview().getEnabled()));
        configuration.setApplication(convert(portalConfigEntity.getApplication()));
        configuration.setAuthentication(convert(portalConfigEntity.getAuthentication()));
        configuration.setDocumentation(convert(portalConfigEntity.getDocumentation()));
        configuration.setPlan(convert(portalConfigEntity.getPlan()));
        configuration.setPortal(convert(portalConfigEntity.getPortal(), portalConfigEntity.getApplication()));
        configuration.setPortalNext(convert(portalConfigEntity.getPortalNext()));
        configuration.setScheduler(convert(portalConfigEntity.getScheduler()));
        configuration.setRecaptcha(convert(portalConfigEntity.getReCaptcha()));
        configuration.setAlert(convert(consoleSettingsEntity.getAlert().getEnabled()));
        return configuration;
    }

    ConfigurationPortalNext convert(PortalNext portalNext) {
        ConfigurationPortalNext configuration = new ConfigurationPortalNext();
        configuration.setSiteTitle(portalNext.getSiteTitle());
        configuration.setAccess(convert(portalNext.getAccess()));
        configuration.setBanner(convert(portalNext.getBanner()));
        return configuration;
    }

    private ConfigurationPortalNextBanner convert(PortalNext.Banner banner) {
        ConfigurationPortalNextBanner configuration = new ConfigurationPortalNextBanner();
        if (!Objects.isNull(banner)) {
            configuration.setEnabled(banner.getEnabled());
            configuration.setTitle(banner.getTitle());
            configuration.setSubtitle(banner.getSubtitle());
        }
        return configuration;
    }

    private ConfigurationAnalytics convert(Analytics analytics) {
        ConfigurationAnalytics configuration = new ConfigurationAnalytics();
        configuration.setClientTimeout(analytics.getClientTimeout());
        return configuration;
    }

    private ConfigurationScheduler convert(PortalScheduler scheduler) {
        ConfigurationScheduler configuration = new ConfigurationScheduler();
        configuration.setNotificationsInSeconds(scheduler.getNotificationsInSeconds());
        return configuration;
    }

    private ConfigurationPortal convert(Portal portal, PortalApplicationSettings application) {
        ConfigurationPortal configuration = new ConfigurationPortal();
        configuration.setAnalytics(convert(portal.getAnalytics()));
        configuration.setApikeyHeader(portal.getApikeyHeader());
        configuration.setApis(convert(portal.getApis()));
        configuration.setEntrypoint(portal.getEntrypoint());
        configuration.setUploadMedia(convert(portal.getUploadMedia()));
        configuration.setRating(convert(portal.getRating()));
        configuration.setSupport(convert(portal.getSupport()));
        configuration.setUserCreation(convert(portal.getUserCreation().getEnabled()));
        configuration.setHomepageTitle(portal.getHomepageTitle());

        PortalApplicationSettings.ApplicationTypes types = application.getTypes();
        if (
            !application.getRegistration().getEnabled() &&
            !types.getSimpleType().isEnabled() ||
            !types.getSimpleType().isEnabled() &&
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

    private ConfigurationPortalRating convert(Portal.PortalRating rating) {
        ConfigurationPortalRating configuration = new ConfigurationPortalRating();
        configuration.setComment(convert(rating.getComment()));
        configuration.setEnabled(rating.isEnabled());
        return configuration;
    }

    private ConfigurationPortalRatingComment convert(Portal.PortalRating.RatingComment comment) {
        ConfigurationPortalRatingComment configuration = new ConfigurationPortalRatingComment();
        configuration.setMandatory(comment.isMandatory());
        return configuration;
    }

    private ConfigurationPortalMedia convert(Portal.PortalUploadMedia uploadMedia) {
        ConfigurationPortalMedia configuration = new ConfigurationPortalMedia();
        configuration.setEnabled(uploadMedia.getEnabled());
        configuration.setMaxSizeInBytes(uploadMedia.getMaxSizeInOctet());
        return configuration;
    }

    private ConfigurationPortalApis convert(Portal.PortalApis apis) {
        ConfigurationPortalApis configuration = new ConfigurationPortalApis();
        configuration.setApiHeaderShowTags(convert(apis.getApiHeaderShowTags()));
        configuration.setApiHeaderShowCategories(convert(apis.getApiHeaderShowCategories()));
        configuration.setTilesMode(convert(apis.getTilesMode()));
        configuration.setCategoryMode(convert(apis.getCategoryMode()));
        configuration.setPromotedApiMode(convert(apis.getPromotedApiMode()));
        return configuration;
    }

    private ConfigurationPortalAnalytics convert(Portal.PortalAnalytics analytics) {
        ConfigurationPortalAnalytics configuration = new ConfigurationPortalAnalytics();
        configuration.setEnabled(analytics.isEnabled());
        configuration.setTrackingId(analytics.getTrackingId());
        return configuration;
    }

    private ConfigurationPlan convert(PlanSettings plan) {
        ConfigurationPlan configuration = new ConfigurationPlan();
        configuration.setSecurity(convert(plan.getSecurity()));
        return configuration;
    }

    private ConfigurationPlanSecurity convert(PlanSettings.PlanSecurity security) {
        ConfigurationPlanSecurity configuration = new ConfigurationPlanSecurity();
        configuration.setApikey(convert(security.getApikey()));
        configuration.setSharedApiKey(convert(security.getSharedApiKey()));
        configuration.setJwt(convert(security.getJwt()));
        configuration.setMtls(convert(security.getMtls()));
        configuration.setKeyless(convert(security.getKeyless()));
        configuration.setOauth2(convert(security.getOauth2()));
        return configuration;
    }

    private ConfigurationDocumentation convert(Documentation documentation) {
        ConfigurationDocumentation configuration = new ConfigurationDocumentation();
        configuration.setUrl(documentation.getUrl());
        return configuration;
    }

    private ConfigurationAuthentication convert(PortalAuthentication authentication) {
        ConfigurationAuthentication configuration = new ConfigurationAuthentication();
        configuration.setForceLogin(convert(authentication.getForceLogin()));
        configuration.setLocalLogin(convert(authentication.getLocalLogin()));
        return configuration;
    }

    private ConfigurationApplication convert(PortalApplicationSettings application) {
        ConfigurationApplication configuration = new ConfigurationApplication();
        configuration.setRegistration(convert(application.getRegistration().getEnabled()));
        configuration.setTypes(convert(application.getTypes()));
        return configuration;
    }

    private ConfigurationApplicationTypes convert(PortalApplicationSettings.ApplicationTypes types) {
        ConfigurationApplicationTypes configuration = new ConfigurationApplicationTypes();
        configuration.setBackendToBackend(convert(types.getBackendToBackendType()));
        configuration.setBrowser(convert(types.getBrowserType()));
        configuration.setNative(convert(types.getNativeType()));
        configuration.setSimple(convert(types.getSimpleType()));
        configuration.setWeb(convert(types.getWebType()));

        return configuration;
    }

    private ConfigurationReCaptcha convert(PortalReCaptcha reCaptcha) {
        ConfigurationReCaptcha configuration = new ConfigurationReCaptcha();
        configuration.setEnabled(reCaptcha.getEnabled());
        configuration.setSiteKey(reCaptcha.getSiteKey());
        return configuration;
    }

    private Enabled convert(Boolean enabled) {
        return new Enabled().enabled(enabled);
    }

    private Enabled convert(io.gravitee.rest.api.model.settings.Enabled enabledEntity) {
        return new Enabled().enabled(enabledEntity.isEnabled());
    }
}
