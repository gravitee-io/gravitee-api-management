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
package fixtures.core.model;

import io.gravitee.apim.core.metadata.model.Metadata;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Supplier;

public class MetadataFixtures {

    private static final Supplier<Metadata.MetadataBuilder> BASE = () ->
        Metadata
            .builder()
            .key("my-key")
            .format(Metadata.MetadataFormat.MAIL)
            .value("my-value")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static Metadata anApiMetadata() {
        return BASE.get().referenceType(Metadata.ReferenceType.API).referenceId("api-id").build();
    }

    public static Metadata anApiMetadata(String apiId) {
        return BASE.get().referenceType(Metadata.ReferenceType.API).referenceId(apiId).build();
    }
}
