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

import io.gravitee.apim.core.metadata.model.Metadata;

@SuppressWarnings("rawtypes")
public class MetadataFixtures {

    private MetadataFixtures() {}

    public static Metadata aCoreMetadata(String apiId, String key, String name, Object value, Metadata.MetadataFormat format) {
        String referenceId;
        Metadata.ReferenceType referenceType;
        if (apiId != null) {
            referenceId = apiId;
            referenceType = Metadata.ReferenceType.API;
        } else {
            referenceId = "_";
            referenceType = Metadata.ReferenceType.DEFAULT;
        }

        return Metadata
            .builder()
            .referenceId(referenceId)
            .referenceType(referenceType)
            .key(key)
            .name(name)
            .value(value != null ? value.toString() : null)
            .format(format)
            .build();
    }

    public static io.gravitee.rest.api.management.v2.rest.model.Metadata aMapiV2Metadata(
        String key,
        String name,
        Object value,
        Object defaultValue,
        io.gravitee.rest.api.management.v2.rest.model.MetadataFormat format
    ) {
        return io.gravitee.rest.api.management.v2.rest.model.Metadata
            .builder()
            .key(key)
            .name(name)
            .value(value != null ? value.toString() : null)
            .defaultValue(defaultValue != null ? defaultValue.toString() : null)
            .format(format)
            .build();
    }
}
