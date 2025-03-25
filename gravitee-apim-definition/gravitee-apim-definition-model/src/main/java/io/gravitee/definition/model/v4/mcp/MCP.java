package io.gravitee.definition.model.v4.mcp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.Plugin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@Schema(name = "MCPV4")
public class MCP implements Serializable {

    @Builder.Default
    protected boolean enabled = true;

    @Builder.Default
    private List<Tool> tools = new ArrayList<>();

    // TODO MCP: Return with correct information for plugins?
//    @JsonIgnore
//    public List<Plugin> getPlugins() {
//        return List.of(new Plugin("server", "mcp-server"));
//    }
}
