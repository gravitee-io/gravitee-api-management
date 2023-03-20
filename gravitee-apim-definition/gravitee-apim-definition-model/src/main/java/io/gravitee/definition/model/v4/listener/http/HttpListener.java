/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model.v4.listener.http;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.definition.model.Cors;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(name = "HttpListenerV4")
public class HttpListener extends Listener {

    @NotEmpty
    @NotNull
    private List<Path> paths;

    private Set<String> pathMappings;

    @JsonIgnore
    private Map<String, Pattern> pathMappingsPattern;

    private Cors cors;

    public HttpListener() {
        super(ListenerType.HTTP);
    }

    public void setPathMappings(final Set<String> pathMappings) {
        this.pathMappings = pathMappings;
        if (pathMappings != null) {
            setPathMappingsPattern(
                pathMappings
                    .stream()
                    .collect(
                        toMap(pathMapping -> pathMapping, pathMapping -> Pattern.compile(pathMapping.replaceAll(":\\w*", "[^\\/]*") + "/*"))
                    )
            );
        }
    }
}
