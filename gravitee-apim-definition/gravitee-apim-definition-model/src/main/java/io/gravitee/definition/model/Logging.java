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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Logging implements Serializable {

    public static final LoggingMode DEFAULT_LOGGING_MODE = LoggingMode.NONE;
    public static final LoggingScope DEFAULT_LOGGING_SCOPE = LoggingScope.NONE;
    public static final LoggingContent DEFAULT_LOGGING_CONTENT = LoggingContent.NONE;

    @JsonProperty
    private LoggingMode mode = DEFAULT_LOGGING_MODE;

    @JsonProperty
    private LoggingScope scope = DEFAULT_LOGGING_SCOPE;

    @JsonProperty
    private LoggingContent content = DEFAULT_LOGGING_CONTENT;

    @JsonProperty
    private String condition;
}
