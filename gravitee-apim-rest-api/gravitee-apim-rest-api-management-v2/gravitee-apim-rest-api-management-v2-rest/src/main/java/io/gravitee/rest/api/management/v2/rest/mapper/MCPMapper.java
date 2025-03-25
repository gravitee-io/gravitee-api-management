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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.definition.model.v4.mcp.MCP;
import io.gravitee.definition.model.v4.mcp.Tool;
import io.gravitee.rest.api.management.v2.rest.model.Group;
import io.gravitee.rest.api.management.v2.rest.model.GroupEvent;
import io.gravitee.rest.api.management.v2.rest.model.MCPTool;
import io.gravitee.rest.api.management.v2.rest.model.MCPToolProperty;
import io.gravitee.rest.api.management.v2.rest.model.MCPToolPropertyArray;
import io.gravitee.rest.api.management.v2.rest.model.MCPToolPropertyString;
import io.gravitee.rest.api.management.v2.rest.model.MCPToolPropertyType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.GroupEventRuleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public interface MCPMapper {
    Logger logger = LoggerFactory.getLogger(MCPMapper.class);
    MCPMapper INSTANCE = Mappers.getMapper(MCPMapper.class);

    MCP convert(io.gravitee.rest.api.management.v2.rest.model.MCP mcp);

    List<Tool> convert(List<MCPTool> mcpTools);

    io.gravitee.rest.api.management.v2.rest.model.MCP convertToRest(MCP mcp);
    List<MCPTool> convertToRest(List<Tool> mcpTools);

    @Mapping(source = "properties", target = "inputSchema.properties")
    @Mapping(source = "required", target = "inputSchema.required")
    Tool convert(MCPTool mcpTool);

    @Mapping(target = "items", ignore = true)
    Tool.Property convert(MCPToolPropertyString mcpToolPropertyString);

    Tool.Property convert(MCPToolPropertyArray mcpToolPropertyArray);

    @Mapping(source = "inputSchema.properties", target = "properties")
    @Mapping(source = "inputSchema.required", target = "required")
    MCPTool convert(Tool mcpTool);

    //
    //    @Mapping(target = "items", ignore = true)
    //    Tool.Property convert(MCPToolPropertyString mcpToolPropertyString);
    //    Tool.Property convert(MCPToolPropertyArray mcpToolPropertyArray);

    default Tool.Property convert(MCPToolProperty mcpToolProperty) {
        if (mcpToolProperty == null) {
            return null;
        }

        try {
            if (mcpToolProperty.getMCPToolPropertyArray() != null) {
                return this.convert(mcpToolProperty.getMCPToolPropertyArray());
            }

            return this.convert(mcpToolProperty.getMCPToolPropertyString());
        } catch (ClassCastException e) {
            logger.error("Unable to convert MCP Tool Property", e);
        }
        return null;
    }

    default MCPToolProperty convert(Tool.Property mcpToolProperty) {
        if (mcpToolProperty == null) {
            return null;
        }

        if (MCPToolPropertyType.ARRAY.getValue().equals(mcpToolProperty.getType())) {
            var arrayProperty = new MCPToolPropertyArray();
            arrayProperty.setType(MCPToolPropertyType.ARRAY);
            arrayProperty.setItems(mcpToolProperty.getItems());
            return new MCPToolProperty(arrayProperty);
        }

        var stringProperty = new MCPToolPropertyString();
        stringProperty.setType(MCPToolPropertyType.STRING);
        return new MCPToolProperty(stringProperty);
    }
}
