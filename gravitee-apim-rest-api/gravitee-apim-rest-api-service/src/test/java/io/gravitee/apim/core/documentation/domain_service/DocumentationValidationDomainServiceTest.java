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
package io.gravitee.apim.core.documentation.domain_service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.gravitee.apim.core.documentation.exception.InvalidPageNameException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DocumentationValidationDomainServiceTest {

    private DocumentationValidationDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new DocumentationValidationDomainService();
    }

    @Test
    void should_throw_an_exception() {
        assertThatThrownBy(() -> cut.sanitizeDocumentationName("")).isInstanceOf(InvalidPageNameException.class);
        assertThatThrownBy(() -> cut.sanitizeDocumentationName("  ")).isInstanceOf(InvalidPageNameException.class);
        assertThatThrownBy(() -> cut.sanitizeDocumentationName(null)).isInstanceOf(InvalidPageNameException.class);
    }

    @Test
    void should_sanitize_name() {
        assertThat(cut.sanitizeDocumentationName("foo")).isEqualTo("foo");
        assertThat(cut.sanitizeDocumentationName("bar     ")).isEqualTo("bar");
    }
}
