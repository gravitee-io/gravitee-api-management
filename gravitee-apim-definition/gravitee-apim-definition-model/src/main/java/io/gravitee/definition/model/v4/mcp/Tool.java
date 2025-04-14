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
package io.gravitee.definition.model.v4.mcp;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@Schema(name = "MCPToolV4")
public class Tool {

    private String name;
    private String description;
    private InputSchema inputSchema;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    @Schema(name = "MCPToolInputSchemaV4")
    public static class InputSchema {

        @Builder.Default
        private String type = "object";

        private Map<String, Object> properties;

        // Required property names
        private List<String> required;
    }
    //    @NoArgsConstructor
    //    @AllArgsConstructor
    //    @Builder(toBuilder = true)
    //    @Data
    //    @Schema(name = "MCPToolPropertyV4")
    //    public static class Property {
    //        private String type;
    //        private Map<String, Object> items;
    //    }
}
