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
package io.gravitee.apim.rest.api.mcp.core.model;

import io.gravitee.apim.rest.api.mcp.core.TriageException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * What traffic can be broken down by.
 */
public enum Dimension {
    API,
    APPLICATION,
    PLAN,
    GATEWAY,
    STATUS,
    PATH,
    METHOD,
    HOST,
    CONSUMER_IP;

    public static Dimension parse(String value) {
        if (value != null) {
            for (var dimension : values()) {
                if (dimension.name().equalsIgnoreCase(value.trim())) {
                    return dimension;
                }
            }
        }
        var accepted = Arrays.stream(values())
            .map(dimension -> dimension.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(", "));
        throw new TriageException("Invalid by '" + value + "'. Use one of: " + accepted + ".");
    }
}
