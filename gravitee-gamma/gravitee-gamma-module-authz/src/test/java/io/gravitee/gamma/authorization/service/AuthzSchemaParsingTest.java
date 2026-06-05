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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthzSchemaParsingTest {

    private static final String NAMESPACED =
        """
        entity Subject {};
        namespace myapp {
          entity Report {};
          action "read" appliesTo { principal: [Subject], resource: [Report] };
        }
        """;

    private static final String FLAT =
        """
        entity User {};
        entity Document {};
        action "view" appliesTo { principal: [User], resource: [Document] };
        """;

    @Test
    void validate_returns_no_errors_for_valid_schema() {
        assertThat(AuthzSchemaParsing.validate("entity X {};")).isEmpty();
        assertThat(AuthzSchemaParsing.validate(NAMESPACED)).isEmpty();
    }

    @Test
    void validate_returns_errors_for_garbage() {
        assertThat(AuthzSchemaParsing.validate("@@@ not gapl")).isNotEmpty();
    }

    @Test
    void validate_strips_the_engine_prefix_and_splits_errors() {
        assertThat(AuthzSchemaParsing.validate("@@@ not gapl"))
            .allSatisfy(error -> assertThat(error).doesNotStartWith("authorization schema parse errors:"));
    }

    @Test
    void toJson_groups_types_under_their_namespace() {
        String json = AuthzSchemaParsing.toJson(NAMESPACED);
        assertThat(json).contains("\"myapp\"").contains("\"Report\"").contains("\"Subject\"");
    }

    @Test
    void toJson_exposes_action_applies_to_principal_and_resource_types() {
        String json = AuthzSchemaParsing.toJson(NAMESPACED);
        assertThat(json)
            .contains("\"actions\"")
            .contains("\"read\"")
            .contains("\"principalTypes\"")
            .contains("\"resourceTypes\"");
    }

    @Test
    void toJson_uses_empty_namespace_key_for_top_level_only_schema() {
        String json = AuthzSchemaParsing.toJson(FLAT);
        assertThat(json).contains("\"\"").contains("\"User\"").contains("\"Document\"");
    }

    @Test
    void toJson_exposes_entity_attributes_and_member_of_types() {
        String json = AuthzSchemaParsing.toJson("entity Group {};\nentity User in [Group] {\n  name: String\n};");
        assertThat(json)
            .contains("\"shape\"")
            .contains("\"name\"")
            .contains("\"memberOfTypes\"")
            .contains("\"Group\"");
    }

    @Test
    void toJson_keeps_top_level_decls_global_with_named_namespaces() {
        String mixed =
            """
            entity Glob {};
            namespace a {
              entity X {};
              action "ax" appliesTo { principal: [X], resource: [Glob] };
            }
            namespace b {
              entity Y {};
            }
            """;
        String json = AuthzSchemaParsing.toJson(mixed);
        // The top-level entity Glob is emitted under the empty "" namespace (not dropped, not
        // absorbed into a named namespace), and the action in `a` references it as a bare global.
        assertThat(json).contains("\"a\"").contains("\"b\"").contains("\"X\"").contains("\"Y\"").contains("\"ax\"");
        // Empty-namespace group present, and Glob declared as a global entity type within it.
        assertThat(json).contains("\"\" :").contains("\"Glob\"");
    }

    @Test
    void validate_treats_blank_input_as_valid_empty_schema() {
        assertThat(AuthzSchemaParsing.validate("")).isEmpty();
        assertThat(AuthzSchemaParsing.validate("   \n\t  ")).isEmpty();
    }
}
