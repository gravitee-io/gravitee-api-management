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
package io.gravitee.apim.infra.converter.oai;

import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.CollectionUtils;

public class OAIServersConverter {

    public static OAIServersConverter INSTANCE = new OAIServersConverter();

    public List<String> convert(List<Server> servers) {
        if (servers == null || servers.isEmpty()) {
            return Collections.emptyList();
        }

        return servers
            .stream()
            .flatMap(server -> {
                var serverVariables = server.getVariables();
                var serverUrl = server.getUrl();
                if (CollectionUtils.isEmpty(serverVariables)) {
                    return Stream.of(serverUrl);
                } else {
                    var evaluatedUrls = Collections.singletonList(serverUrl);
                    for (var serverVar : serverVariables.entrySet()) {
                        evaluatedUrls = evaluateServerUrlsForOneVar(serverVar.getKey(), serverVar.getValue(), evaluatedUrls);
                    }
                    return evaluatedUrls.stream();
                }
            })
            .collect(Collectors.toList());
    }

    private List<String> evaluateServerUrlsForOneVar(String varName, ServerVariable serverVar, List<String> templateUrls) {
        if (varName == null || serverVar == null || CollectionUtils.isEmpty(templateUrls)) {
            return Collections.emptyList();
        }

        return templateUrls
            .stream()
            .flatMap(templateUrl -> {
                var matcher = Pattern.compile("\\{" + varName + "\\}").matcher(templateUrl);
                if (matcher.find()) {
                    if (CollectionUtils.isEmpty(serverVar.getEnum()) && serverVar.getDefault() != null) {
                        return Stream.of(templateUrl.replace(matcher.group(0), serverVar.getDefault()));
                    } else {
                        return serverVar
                            .getEnum()
                            .stream()
                            .map(enumValue -> templateUrl.replace(matcher.group(0), enumValue));
                    }
                } else {
                    return Stream.of(templateUrl);
                }
            })
            .collect(Collectors.toList());
    }
}
