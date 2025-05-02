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
package io.gravitee.rest.api.model.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
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

    @ParameterKey(Key.PORTAL_TCP_PORT)
    @Min(1025)
    @Max(65535)
    private Integer tcpPort;

    @ParameterKey(Key.PORTAL_KAFKA_DOMAIN)
    @Size(max = 192)
    private String kafkaDomain;

    @ParameterKey(Key.PORTAL_KAFKA_PORT)
    @Min(1025)
    @Max(65535)
    private Integer kafkaPort;

    @ParameterKey(Key.PORTAL_KAFKA_SASL_MECHANISMS)
    private List<String> kafkaSaslMechanisms;

    private PortalApis apis;
    private PortalAnalytics analytics;
    private PortalRating rating;

    private PortalUploadMedia media;

    private PortalUserCreation userCreation;

    public PortalUploadMedia getUploadMedia() {
        return media;
    }

    public void setUploadMedia(PortalUploadMedia media) {
        this.media = media;
    }

    public Portal() {
        apis = new PortalApis();
        analytics = new PortalAnalytics();
        rating = new PortalRating();
        media = new PortalUploadMedia();
        userCreation = new PortalUserCreation();
    }

    // Classes
    @Setter
    public static class PortalAnalytics {

        @ParameterKey(Key.PORTAL_ANALYTICS_ENABLED)
        private Boolean enabled;

        @Getter
        @ParameterKey(Key.PORTAL_ANALYTICS_TRACKINGID)
        private String trackingId;

        public Boolean isEnabled() {
            return enabled;
        }
    }

    @Getter
    @Setter
    public static class PortalApis {

        @ParameterKey(Key.PORTAL_APIS_TILESMODE_ENABLED)
        private Enabled tilesMode;

        @ParameterKey(Key.PORTAL_APIS_DOCUMENTATIONONLYMODE_ENABLED)
        private Enabled documentationOnlyMode;

        @ParameterKey(Key.PORTAL_APIS_CATEGORY_ENABLED)
        private Enabled categoryMode;

        @ParameterKey(Key.PORTAL_APIS_PROMOTED_API_ENABLED)
        private Enabled promotedApiMode;

        @ParameterKey(Key.PORTAL_APIS_SHOW_TAGS_IN_APIHEADER)
        private Enabled apiHeaderShowTags;

        @ParameterKey(Key.PORTAL_APIS_SHOW_CATEGORIES_IN_APIHEADER)
        private Enabled apiHeaderShowCategories;
    }

    @Getter
    @Setter
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

        @Getter
        @Setter
        public static class RatingComment {

            @ParameterKey(Key.PORTAL_RATING_COMMENT_MANDATORY)
            private Boolean mandatory;

            public Boolean isMandatory() {
                return mandatory;
            }
        }
    }

    @Getter
    @Setter
    public static class PortalUploadMedia {

        @ParameterKey(Key.PORTAL_UPLOAD_MEDIA_ENABLED)
        private Boolean enabled;

        @ParameterKey(Key.PORTAL_UPLOAD_MEDIA_MAXSIZE)
        private Integer maxSizeInOctet;
    }

    @Getter
    @Setter
    public static class PortalUserCreation {

        @ParameterKey(Key.PORTAL_USERCREATION_ENABLED)
        private Boolean enabled;

        @ParameterKey(Key.PORTAL_USERCREATION_AUTOMATICVALIDATION_ENABLED)
        private Enabled automaticValidation;
    }
}
