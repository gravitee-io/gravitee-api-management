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
package io.gravitee.gamma.authorization.service;

import io.gravitee.authz.engine.parser.AuthzParseException;
import io.gravitee.authz.engine.schema.AuthzSchemaParser;
import io.gravitee.authz.engine.schema.SchemaJsonExporter;
import java.util.List;

final class AuthzSchemaParsing {

    private static final String PARSE_ERROR_PREFIX = "authorization schema parse errors: ";

    private AuthzSchemaParsing() {}

    static List<String> validate(String schemaText) {
        try {
            AuthzSchemaParser.parse(schemaText);
            return List.of();
        } catch (IllegalArgumentException | IllegalStateException | AuthzParseException e) {
            return splitErrors(e.getMessage());
        }
    }

    static String toJson(String schemaText) {
        return SchemaJsonExporter.export(AuthzSchemaParser.parse(schemaText));
    }

    private static List<String> splitErrors(String message) {
        if (message == null || message.isBlank()) {
            return List.of("invalid schema");
        }
        String body = message.startsWith(PARSE_ERROR_PREFIX) ? message.substring(PARSE_ERROR_PREFIX.length()) : message;
        return List.of(body.split("; "));
    }
}
