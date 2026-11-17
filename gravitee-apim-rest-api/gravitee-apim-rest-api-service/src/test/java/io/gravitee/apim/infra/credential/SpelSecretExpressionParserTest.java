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
package io.gravitee.apim.infra.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.credential.exception.InvalidCredentialException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SpelSecretExpressionParserTest {

    private final SpelSecretExpressionParser parser = new SpelSecretExpressionParser();

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{#secrets.get(#context.attributes['key-uri'])}",
            "{#secrets.get('/vault/secret/' + #context.attributes['tenant'] + ':token')}",
            "{#secrets.get(#api.properties['openai-uri'])}",
            "{#secrets.get(#context.attributes['uri'], 'token')}",
        }
    )
    void should_leave_a_path_read_from_context_data_to_the_gateway(String expression) {
        assertThat(parser.literalPath("token", expression)).isEmpty();
    }

    @Test
    void should_read_the_path_of_an_expression_made_of_text() {
        assertThat(parser.literalPath("token", "{#secrets.get('/vault/secret/openai', 'token')}")).contains("/vault/secret/openai:token");
        assertThat(parser.literalPath("token", "{#secrets.get('/vault/secret/openai:token')}")).contains("/vault/secret/openai:token");
    }

    @Test
    void should_read_the_path_named_in_text_when_only_the_key_comes_from_context_data() {
        assertThat(
            parser.literalPath("token", "{#secrets.get('/gravitee/credential.other.token', #context.attributes['key'])}")
        ).hasValueSatisfying(path -> assertThat(path).startsWith("/gravitee/credential.other.token:"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "#secrets.get('/vault/secret/openai:token')",
            "{#secrets.get('/vault/secret/openai:token')",
            "{#context.attributes['token']}",
            "{#secrets.get()}",
            "{#secrets.get('a', 'b', 'c')}",
            "{#secrets.get('/vault/a:b') + 'x'}",
            "{#secrets.put('/vault/a:b')}",
            "{#secrets.get(T(java.lang.System).getenv('TOKEN'))}",
            "{#secrets.get(new java.lang.String('/vault/a:b'))}",
            "{#secrets.get(@someBean)}",
            "{#secrets.get(#context.attributes['uri'].toString())}",
            "{#secrets.get(#context.class.classLoader)}",
            "{#secrets.get(#context.attributes)}",
            "{#secrets.get(#context.attributes['a']['b'])}",
            "{#secrets.get(#context.attributes[0])}",
            "{#secrets.get(#environment['x'])}",
            "{#secrets.get(#context.attributes['a'] = 'b')}",
            "{#secrets.get(#context.attributes.?[true])}",
            "{#secrets.get('unclosed)}",
            "{#secrets.get(#request.headers['X-Tenant'][0])}",
            "{#secrets.get(#request.params['tenant'])}",
            "{#secrets.get('/vault/secret/' + #request.pathParams['tenant'] + ':token')}",
            "{#secrets.get(#context.attributes['uri'], #context.attributes['key'])}",
            "{#secrets.get('/vault/secret/openai')}",
            "{#secrets.get('openai-secret')}",
        }
    )
    void should_reject_anything_but_a_secret_lookup_from_text_or_context_data(String expression) {
        assertThatThrownBy(() -> parser.literalPath("token", expression))
            .isInstanceOf(InvalidCredentialException.class)
            .hasMessageContaining("token");
    }
}
