package io.gravitee.definition.model.v4.mcp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

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
        private List<Property> properties;

        // Required property names
        private List<String> required;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(toBuilder = true)
    @Data
    @Schema(name = "MCPToolPropertyV4")
    public static class Property {
        private String type;
        private Map<String, Object> items;
    }
}
