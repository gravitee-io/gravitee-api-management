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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.model.annotations.ParameterKey;
import io.gravitee.rest.api.model.parameters.Key;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @ParameterKey(Key.PORTAL_NEXT_MTLS_ENABLED)
    private Enabled mtls;

    @ParameterKey(Key.PORTAL_NEXT_ANALYTICS_ENABLED)
    private Enabled analytics;

    private Applications applications;

    private Banner banner;

    private Catalog catalog;

    public PortalNext() {
        this.applications = new Applications();
        this.banner = new Banner();
        this.catalog = new Catalog();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Applications {

        private Membership membership;

        public Applications() {
            this.membership = new Membership();
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Membership {

            @ParameterKey(Key.PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_ENABLED)
            private Boolean enabled;

            @ParameterKey(Key.PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_TRANSFER_OWNERSHIP_ENABLED)
            private Enabled transferOwnership;

            @ParameterKey(Key.PORTAL_NEXT_APPLICATIONS_MEMBERSHIP_INVITATIONS_ENABLED)
            private Enabled invitations;
        }
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

        @ParameterKey(Key.PORTAL_NEXT_SEARCH_FUZZY)
        private Enabled fuzzySearch;

        // Stored on Catalog itself so the existing settings loader, which only walks this object,
        // persists them. JSON still exposes the nested "tile" object the console and portal use.
        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ParameterKey(Key.PORTAL_NEXT_CATALOG_TILE_NAME_ENABLED)
        private Enabled tileName;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ParameterKey(Key.PORTAL_NEXT_CATALOG_TILE_TYPE_ENABLED)
        private Enabled tileType;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ParameterKey(Key.PORTAL_NEXT_CATALOG_TILE_IMAGE_ENABLED)
        private Enabled tileImage;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ParameterKey(Key.PORTAL_NEXT_CATALOG_TILE_VERSION_ENABLED)
        private Enabled tileVersion;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ParameterKey(Key.PORTAL_NEXT_CATALOG_TILE_DESCRIPTION_ENABLED)
        private Enabled tileDescription;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ParameterKey(Key.PORTAL_NEXT_CATALOG_TILE_STATUS_ENABLED)
        private Enabled tileStatus;

        @JsonIgnore
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ParameterKey(Key.PORTAL_NEXT_CATALOG_TILE_OWNER_ENABLED)
        private Enabled tileOwner;

        public Catalog() {
            this.tileName = new Enabled(true);
            this.tileType = new Enabled(true);
            this.tileImage = new Enabled(true);
            this.tileVersion = new Enabled(true);
            this.tileDescription = new Enabled(true);
            this.tileStatus = new Enabled(true);
            this.tileOwner = new Enabled(true);
        }

        @JsonProperty("tile")
        public Tile getTile() {
            Tile tile = new Tile();
            tile.setName(tileName);
            tile.setType(tileType);
            tile.setImage(tileImage);
            tile.setVersion(tileVersion);
            tile.setDescription(tileDescription);
            tile.setStatus(tileStatus);
            tile.setOwner(tileOwner);
            return tile;
        }

        @JsonProperty("tile")
        public void setTile(Tile tile) {
            if (tile == null) {
                return;
            }
            if (tile.getName() != null) {
                this.tileName = tile.getName();
            }
            if (tile.getType() != null) {
                this.tileType = tile.getType();
            }
            if (tile.getImage() != null) {
                this.tileImage = tile.getImage();
            }
            if (tile.getVersion() != null) {
                this.tileVersion = tile.getVersion();
            }
            if (tile.getDescription() != null) {
                this.tileDescription = tile.getDescription();
            }
            if (tile.getStatus() != null) {
                this.tileStatus = tile.getStatus();
            }
            if (tile.getOwner() != null) {
                this.tileOwner = tile.getOwner();
            }
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Tile {

            private Enabled name;
            private Enabled type;
            private Enabled image;
            private Enabled version;
            private Enabled description;
            private Enabled status;
            private Enabled owner;
        }
    }
}
