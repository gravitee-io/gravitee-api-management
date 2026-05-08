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
package io.gravitee.apim.core.portal_page.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.async_api.AsyncApiValidator;
import io.gravitee.apim.core.async_api.exception.AsyncApiContentEmptyException;
import io.gravitee.apim.core.portal_page.model.AsyncApiPageContent;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AsyncApiPortalPageContentValidatorServiceTest {

    private AsyncApiValidator asyncApiValidator;
    private AsyncApiPortalPageContentValidatorService validator;

    @BeforeEach
    void setUp() {
        asyncApiValidator = new AsyncApiValidator();
        validator = new AsyncApiPortalPageContentValidatorService(asyncApiValidator);
    }

    @Test
    void should_apply_to_asyncapi_content() {
        PortalPageContent<?> content = AsyncApiPageContent.create("org", "env", "asyncapi: '3.0.0'");
        assertThat(validator.appliesTo(content)).isTrue();
    }

    @Test
    void should_not_apply_to_gravitee_markdown_content() {
        PortalPageContent<?> content = GraviteeMarkdownPageContent.create("org", "env", "content");
        assertThat(validator.appliesTo(content)).isFalse();
    }

    @Test
    void should_validate_asyncapi_content() {
        PortalPageContent<?> content = GraviteeMarkdownPageContent.create("org", "env", "content");
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("asyncapi: '3.0.0'").build();

        assertThatCode(() -> validator.validate(content, updateContent)).doesNotThrowAnyException();
    }

    @Test
    void should_throw_when_content_is_empty() {
        PortalPageContent<?> content = GraviteeMarkdownPageContent.create("org", "env", "content");
        UpdatePortalPageContent updateContent = UpdatePortalPageContent.builder().content("").build();

        assertThatThrownBy(() -> validator.validate(content, updateContent))
            .isInstanceOf(AsyncApiContentEmptyException.class)
            .hasMessage("Content must not be null or empty");
    }
}
