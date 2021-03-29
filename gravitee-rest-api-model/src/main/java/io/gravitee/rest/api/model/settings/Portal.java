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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Portal {
    @ParameterKey(Key.PORTAL_ENTRYPOINT)
    private String entrypoint;
    @ParameterKey(Key.PORTAL_APIKEY_HEADER)
    private String apikeyHeader;
    @ParameterKey(Key.PORTAL_SUPPORT_ENABLED)
    private Enabled support;
    @ParameterKey(Key.PORTAL_URL)
    private String url;
    @ParameterKey(Key.PORTAL_HOMEPAGE_TITLE)
    private String homepageTitle;

    private PortalApis apis;
    private PortalAnalytics analytics;
    private PortalRating rating;

    private PortalUploadMedia media;

    private PortalUserCreation userCreation;

    public Portal() {
        apis = new PortalApis();
        analytics = new PortalAnalytics();
        rating = new PortalRating();
        media = new PortalUploadMedia();
        userCreation = new PortalUserCreation();
    }

    public String getEntrypoint() {
        return entrypoint;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    public String getApikeyHeader() {
        return apikeyHeader;
    }

    public void setApikeyHeader(String apikeyHeader) {
        this.apikeyHeader = apikeyHeader;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHomepageTitle() {
        return homepageTitle;
    }

    public void setHomepageTitle(String homepageTitle) {
        this.homepageTitle = homepageTitle;
    }

    public PortalApis getApis() {
        return apis;
    }

    public void setApis(PortalApis apis) {
        this.apis = apis;
    }

    public Enabled getSupport() {
        return support;
    }

    public void setSupport(Enabled support) {
        this.support = support;
    }

    public PortalUserCreation getUserCreation() {
        return userCreation;
    }

    public void setUserCreation(PortalUserCreation userCreation) {
        this.userCreation = userCreation;
    }

    public PortalAnalytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(PortalAnalytics analytics) {
        this.analytics = analytics;
    }

    public PortalRating getRating() {
        return rating;
    }

    public void setRating(PortalRating rating) {
        this.rating = rating;
    }

    public PortalUploadMedia getUploadMedia() {
        return media;
    }

    public void setUploadMedia(PortalUploadMedia media) {
        this.media = media;
    }

    // Classes
    public static class PortalAnalytics {

        @ParameterKey(Key.PORTAL_ANALYTICS_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.PORTAL_ANALYTICS_TRACKINGID)
        private String trackingId;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getTrackingId() {
            return trackingId;
        }

        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
        }

    }

    public static class PortalApis {
        @ParameterKey(Key.PORTAL_APIS_TILESMODE_ENABLED)
        private Enabled tilesMode;

        @ParameterKey(Key.PORTAL_APIS_CATEGORY_ENABLED)
        private Enabled categoryMode;

        @ParameterKey(Key.PORTAL_APIS_SHOW_TAGS_IN_APIHEADER)
        private Enabled apiHeaderShowTags;

        @ParameterKey(Key.PORTAL_APIS_SHOW_CATEGORIES_IN_APIHEADER)
        private Enabled apiHeaderShowCategories;

        public Enabled getTilesMode() {
            return tilesMode;
        }

        public void setTilesMode(Enabled tilesMode) {
            this.tilesMode = tilesMode;
        }

        public Enabled getCategoryMode() {
            return categoryMode;
        }

        public void setCategoryMode(Enabled categoryMode) {
            this.categoryMode = categoryMode;
        }

        public Enabled getApiHeaderShowTags() {
            return apiHeaderShowTags;
        }

        public void setApiHeaderShowTags(Enabled apiHeaderShowTags) {
            this.apiHeaderShowTags = apiHeaderShowTags;
        }

        public Enabled getApiHeaderShowCategories() {
            return apiHeaderShowCategories;
        }

        public void setApiHeaderShowCategories(Enabled apiHeaderShowCategories) {
            this.apiHeaderShowCategories = apiHeaderShowCategories;
        }
    }

    public static class PortalRating {
        @ParameterKey(Key.PORTAL_RATING_ENABLED)
        private Boolean enabled;

        private RatingComment comment;

        public PortalRating() {
            comment = new RatingComment();
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public RatingComment getComment() {
            return comment;
        }

        public void setComment(RatingComment comment) {
            this.comment = comment;
        }

        public static class RatingComment {
            @ParameterKey(Key.PORTAL_RATING_COMMENT_MANDATORY)
            private Boolean mandatory;

            public Boolean isMandatory() {
                return mandatory;
            }

            public void setMandatory(Boolean mandatory) {
                this.mandatory = mandatory;
            }
        }
    }

    public static class PortalUploadMedia {
        @ParameterKey(Key.PORTAL_UPLOAD_MEDIA_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.PORTAL_UPLOAD_MEDIA_MAXSIZE)
        private Integer maxSizeInOctet;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getMaxSizeInOctet() {
            return maxSizeInOctet;
        }

        public void setMaxSizeInOctet(Integer maxSizeInOctet) {
            this.maxSizeInOctet = maxSizeInOctet;
        }
    }

    public static class PortalUserCreation {
        @ParameterKey(Key.PORTAL_USERCREATION_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED)
        private Enabled automaticValidation;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Enabled getAutomaticValidation() {
            return automaticValidation;
        }

        public void setAutomaticValidation(Enabled automaticValidation) {
            this.automaticValidation = automaticValidation;
        }
    }
}
