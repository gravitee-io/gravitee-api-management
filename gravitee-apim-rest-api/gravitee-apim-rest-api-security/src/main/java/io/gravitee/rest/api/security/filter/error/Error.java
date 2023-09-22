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
package io.gravitee.rest.api.security.filter.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@JsonPropertyOrder({ Error.JSON_PROPERTY_HTTP_STATUS, Error.JSON_PROPERTY_MESSAGE })
@Builder
@Getter
@AllArgsConstructor
public class Error {

    public static final String JSON_PROPERTY_HTTP_STATUS = "httpStatus";
    public static final String JSON_PROPERTY_MESSAGE = "message";

    @JsonProperty(JSON_PROPERTY_HTTP_STATUS)
    private Integer httpStatus;

    @JsonProperty(JSON_PROPERTY_MESSAGE)
    private String message;
}
