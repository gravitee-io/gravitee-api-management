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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.apim.rest.api.common.apiservices.ManagementApiServiceConfiguration;
import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpMethod;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class HttpDynamicPropertiesServiceConfiguration implements ManagementApiServiceConfiguration {

    @JsonProperty("schedule")
    private String schedule;

    @JsonProperty("url")
    private String url;

    @JsonProperty("method")
    private HttpMethod method = HttpMethod.GET;

    @JsonProperty("headers")
    private List<HttpHeader> headers;

    @JsonProperty("body")
    private String body;

    @JsonProperty(value = "transformation", required = true)
    private String transformation;

    @JsonProperty("systemProxy")
    private boolean useSystemProxy;
}
