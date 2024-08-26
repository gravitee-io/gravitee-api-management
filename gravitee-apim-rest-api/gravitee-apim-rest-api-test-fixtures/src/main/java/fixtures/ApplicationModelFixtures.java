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
package fixtures;

import io.gravitee.definition.model.Origin;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.application.ApplicationSettings;
import io.gravitee.rest.api.model.application.OAuthClientSettings;
import io.gravitee.rest.api.model.application.SimpleApplicationSettings;
import java.util.Date;
import java.util.Set;

public class ApplicationModelFixtures {

    private ApplicationModelFixtures() {}

    protected static final ApplicationSettings.ApplicationSettingsBuilder APPLICATION_SETTINGS_BUILDER = ApplicationSettings
        .builder()
        .app(SimpleApplicationSettings.builder().clientId("clientId").build())
        .oauth(OAuthClientSettings.builder().clientId("clientId").build());

    private static final ApplicationListItem.ApplicationListItemBuilder BASE_APPLICATION_LIST_ITEM = ApplicationListItem
        .builder()
        .id("my-application")
        .name("My application")
        .description("Description")
        .apiKeyMode(ApiKeyMode.EXCLUSIVE)
        .background("background")
        .backgroundUrl("https://background.gravitee.io")
        .createdAt(new Date())
        .updatedAt(new Date())
        .disableMembershipNotifications(true)
        .domain("domain")
        .groups(Set.of("group1", "group2"))
        .origin(Origin.MANAGEMENT)
        .picture("picture")
        .pictureUrl("https://picture.gravitee.io")
        .primaryOwner(PrimaryOwnerModelFixtures.aPrimaryOwnerEntity())
        .settings(APPLICATION_SETTINGS_BUILDER.build())
        .status("ACTIVE")
        .type("iOS");

    private static final ApplicationEntity.ApplicationEntityBuilder BASE_APPLICATION_ENTITY = ApplicationEntity
        .builder()
        .id("my-application")
        .name("My application")
        .description("Description")
        .apiKeyMode(ApiKeyMode.EXCLUSIVE)
        .background("background")
        .createdAt(new Date())
        .updatedAt(new Date())
        .disableMembershipNotifications(true)
        .domain("domain")
        .groups(Set.of("group1", "group2"))
        .origin(Origin.MANAGEMENT)
        .picture("picture")
        .primaryOwner(PrimaryOwnerModelFixtures.aPrimaryOwnerEntity())
        .settings(APPLICATION_SETTINGS_BUILDER.build())
        .status("ACTIVE")
        .type("iOS");

    public static ApplicationListItem anApplicationListItem() {
        return BASE_APPLICATION_LIST_ITEM.build();
    }

    public static ApplicationEntity anApplicationEntity() {
        return BASE_APPLICATION_ENTITY.build();
    }
}
