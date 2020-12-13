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
package io.gravitee.rest.api.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Portal {
    @ParameterKey(Key.PORTAL_ENTRYPOINT)
    private String entrypoint;
    @ParameterKey(Key.PORTAL_APIKEY_HEADER)
    private String apikeyHeader;
    @ParameterKey(Key.PORTAL_SUPPORT_ENABLED)
    private ConsoleConfigEntity.Enabled support;
    @ParameterKey(Key.PORTAL_URL)
    private String url;

    private ConsoleConfigEntity.PortalApis apis;
    private ConsoleConfigEntity.PortalAnalytics analytics;
    private PortalRating rating;

    private PortalUploadMedia media;

    private PortalUserCreation userCreation;

    public Portal() {
        apis = new ConsoleConfigEntity.PortalApis();
        analytics = new ConsoleConfigEntity.PortalAnalytics();
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

    public ConsoleConfigEntity.PortalApis getApis() {
        return apis;
    }

    public void setApis(ConsoleConfigEntity.PortalApis apis) {
        this.apis = apis;
    }

    public ConsoleConfigEntity.Enabled getSupport() {
        return support;
    }

    public void setSupport(ConsoleConfigEntity.Enabled support) {
        this.support = support;
    }

    public PortalUserCreation getUserCreation() {
        return userCreation;
    }

    public void setUserCreation(PortalUserCreation userCreation) {
        this.userCreation = userCreation;
    }

    public ConsoleConfigEntity.PortalAnalytics getAnalytics() {
        return analytics;
    }

    public void setAnalytics(ConsoleConfigEntity.PortalAnalytics analytics) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class PortalRating {
        @ParameterKey(Key.PORTAL_RATING_ENABLED)
        private Boolean enabled;

        private PortalRating.RatingComment comment;

        public PortalRating() {
            comment = new PortalRating.RatingComment();
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public PortalRating.RatingComment getComment() {
            return comment;
        }

        public void setComment(PortalRating.RatingComment comment) {
            this.comment = comment;
        }

        public class RatingComment {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class PortalUploadMedia {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public class PortalUserCreation {
        @ParameterKey(Key.PORTAL_USERCREATION_ENABLED)
        private Boolean enabled;
        @ParameterKey(Key.PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED)
        private ConsoleConfigEntity.Enabled automaticValidation;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public ConsoleConfigEntity.Enabled getAutomaticValidation() {
            return automaticValidation;
        }

        public void setAutomaticValidation(ConsoleConfigEntity.Enabled automaticValidation) {
            this.automaticValidation = automaticValidation;
        }
    }
}
