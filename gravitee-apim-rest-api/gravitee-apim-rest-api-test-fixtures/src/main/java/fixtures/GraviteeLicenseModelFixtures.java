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

import io.gravitee.rest.api.model.v4.license.GraviteeLicenseEntity;
import io.gravitee.rest.api.model.v4.license.GraviteeLicenseEntity.GraviteeLicenseEntityBuilder;
import java.util.Set;

public class GraviteeLicenseModelFixtures {

    private GraviteeLicenseModelFixtures() {}

    private static final GraviteeLicenseEntityBuilder BASE_GRAVITEE_LICENSE_ENTITY = GraviteeLicenseEntity.builder()
        .tier("tier-galaxy")
        .features(Set.of("feature-datadog-reporter"))
        .packs(Set.of("pack-observability"));

    public static GraviteeLicenseEntity aGraviteeLicenseEntity() {
        return BASE_GRAVITEE_LICENSE_ENTITY.build();
    }
}
