/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model;

import static io.gravitee.rest.api.model.PageType.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class PageTypeTest {

    @Test
    public void fromPageExtensionAndContent_should_return_swagger_called_with_valid_swagger_json_content() {
        String pageExtension = "json";
        String pageContent = "{\"swagger\":\"2.0\",\"info\":";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertSame(SWAGGER, type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_null_called_with_invalid_swagger_json_content() {
        String pageExtension = "json";
        String pageContent = "{\"swaXXXger\":\"2.0\",\"info\":";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertNull(type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_swagger_called_with_valid_swagger_yml_content() {
        String pageExtension = "yml";
        String pageContent = "swagger: 2.0\ninfo:";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertSame(SWAGGER, type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_swagger_called_with_valid_openapi_yml_content() {
        String pageExtension = "yml";
        String pageContent = "openapi: 2.0\ninfo:";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertSame(SWAGGER, type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_null_called_with_invalid_swagger_yml_content() {
        String pageExtension = "yml";
        String pageContent = "swagXXXger: 2.0\ninfo:";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertNull(type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_asyncapi_called_with_valid_asyncapi_json_content() {
        String pageExtension = "json";
        String pageContent = "{\"asyncapi\":\"2.0\",\"info\":";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertSame(ASYNCAPI, type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_null_called_with_invalid_asyncapi_json_content() {
        String pageExtension = "json";
        String pageContent = "{\"asyXXapi\":\"2.0\",\"info\":";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertNull(type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_asyncapi_called_with_valid_asyncapi_yml_content() {
        String pageExtension = "yml";
        String pageContent = "asyncapi: 2.0\ninfo:";
        pageContent = "asyncapi: 2.0\ninfo";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertSame(ASYNCAPI, type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_null_called_with_invalid_asyncapi_yml_content() {
        String pageExtension = "yml";
        String pageContent = "asyXXXpi: 2.0\ninfo:";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertNull(type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_asciidoc_called_with_asciidoc_extension() {
        String pageExtension = "adoc";
        String pageContent = "This is an asciidoc document";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertSame(ASCIIDOC, type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_markdown_called_with_markdown_extension() {
        String pageExtension = "md";
        String pageContent = "This is a markdown document";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertSame(MARKDOWN, type);
    }

    @Test
    public void fromPageExtensionAndContent_should_return_null_called_with_unknown_extension() {
        String pageExtension = "tzr";
        String pageContent = "This is an unknown format document";

        PageType type = PageType.fromPageExtensionAndContent(pageExtension, pageContent);

        assertNull(type);
    }

    @Test
    public void matchesExtension_should_return_true_when_it_matches() {
        assertTrue(MARKDOWN.matchesExtension("Md"));
    }

    @Test
    public void matchesExtension_should_return_false_when_it_doesnt_matches() {
        assertFalse(MARKDOWN.matchesExtension("mxd"));
    }
}
