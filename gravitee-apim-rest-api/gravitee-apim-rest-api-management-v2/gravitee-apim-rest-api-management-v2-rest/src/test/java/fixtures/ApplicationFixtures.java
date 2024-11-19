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

import io.gravitee.rest.api.management.v2.rest.model.ApplicationCRDMetadata;
import io.gravitee.rest.api.management.v2.rest.model.ApplicationCRDSettings;
import io.gravitee.rest.api.management.v2.rest.model.ApplicationCRDSpec;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationFixtures {

    private ApplicationFixtures() {}

    public static ApplicationListItem anApplicationListItem() {
        return ApplicationModelFixtures.anApplicationListItem();
    }

    public static ApplicationEntity anApplicationEntity() {
        return ApplicationModelFixtures.anApplicationEntity();
    }

    public static ApplicationCRDSpec anApplicationCRDSpec() {
        var spec = new ApplicationCRDSpec();
        spec.setId("application-id");
        spec.setName("test");
        spec.setDescription("description");
        spec.setSettings(new ApplicationCRDSettings(new ApplicationCRDSettings.SimpleApplicationSettings("WEB", "test"), null, null));

        ApplicationCRDMetadata metadata = new ApplicationCRDMetadata();
        metadata.setName("test");
        metadata.setValue("test");
        spec.setMetadata(List.of(metadata));

        return spec;
    }
}
