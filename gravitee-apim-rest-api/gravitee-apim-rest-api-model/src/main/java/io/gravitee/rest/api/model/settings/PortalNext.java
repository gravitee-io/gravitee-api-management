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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Sergii ILLICHEVSKYI (sergii.illichevskyi at graviteesource.com)
 * @author GraviteeSource Team
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalNext {

    @ParameterKey(Key.PORTAL_NEXT_SITE_TITLE)
    private String siteTitle;

    @ParameterKey(Key.PORTAL_NEXT_ACCESS_ENABLED)
    private Enabled access;

    private Banner banner;

    private Catalog catalog;

    public PortalNext() {
        this.banner = new Banner();
        this.catalog = new Catalog();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Banner {

        @ParameterKey(Key.PORTAL_NEXT_BANNER_CONFIG_TITLE)
        private String title;

        @ParameterKey(Key.PORTAL_NEXT_BANNER_CONFIG_SUBTITLE)
        private String subtitle;

        @ParameterKey(Key.PORTAL_NEXT_BANNER_CONFIG_ENABLED)
        private Boolean enabled;

        private PrimaryButton primaryButton;

        private SecondaryButton secondaryButton;

        public Banner() {
            super();
            primaryButton = new PrimaryButton();
            secondaryButton = new SecondaryButton();
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PrimaryButton {

            @ParameterKey(Key.PORTAL_NEXT_BANNER_PRIMARY_BUTTON_LABEL)
            private String label;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_PRIMARY_BUTTON_TARGET)
            private String target;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_PRIMARY_BUTTON_ENABLED)
            private Boolean enabled;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_PRIMARY_BUTTON_TYPE)
            private String type;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_PRIMARY_BUTTON_VISIBILITY)
            private String visibility;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SecondaryButton {

            @ParameterKey(Key.PORTAL_NEXT_BANNER_SECONDARY_BUTTON_LABEL)
            private String label;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_SECONDARY_BUTTON_TARGET)
            private String target;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_SECONDARY_BUTTON_ENABLED)
            private Boolean enabled;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_SECONDARY_BUTTON_TYPE)
            private String type;

            @ParameterKey(Key.PORTAL_NEXT_BANNER_SECONDARY_BUTTON_VISIBILITY)
            private String visibility;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Catalog {

        @ParameterKey(Key.PORTAL_NEXT_CATALOG_VIEW_MODE)
        private String viewMode;
    }
}
