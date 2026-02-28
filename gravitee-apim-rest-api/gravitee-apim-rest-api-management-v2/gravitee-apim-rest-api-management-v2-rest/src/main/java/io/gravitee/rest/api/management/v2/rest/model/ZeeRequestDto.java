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
package io.gravitee.rest.api.management.v2.rest.model;

import io.gravitee.apim.core.zee.model.FileContent;
import io.gravitee.apim.core.zee.model.ZeeRequest;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;

/**
 * API request DTO for Zee resource generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZeeRequestDto {

    private String resourceType;
    private String prompt;
    private Map<String, Object> contextData;

    /**
     * Convert this DTO into the domain model, incorporating any uploaded multipart
     * file parts.
     */
    public ZeeRequest toDomain(List<FormDataBodyPart> files) {
        var fileContents = files == null
            ? List.<FileContent>of()
            : files
                .stream()
                .map(f -> new FileContent(f.getContentDisposition().getFileName(), f.getValueAs(String.class), f.getMediaType().toString()))
                .toList();

        return new ZeeRequest(ZeeResourceType.valueOf(resourceType), prompt, fileContents, contextData);
    }
}
