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
package io.gravitee.rest.api.service.impl.search.lucene.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.search.model.IndexablePage;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IndexablePageDocumentTransformerTest {

    private static final String PAGE_ID = "page-id";
    private static final String PAGE_NAME = "hello.md";
    private static final String PAGE_CONTENT = "Hello, is it me you're looking for ?";
    private static final String API_ID = "api-id";

    IndexablePageDocumentTransformer cut = new IndexablePageDocumentTransformer();

    @Test
    void should_transform() {
        assertThat(cut.transform(given())).usingRecursiveComparison().isEqualTo(expected());
    }

    private static IndexablePage given() {
        return new IndexablePage(
            Page.builder()
                .id(PAGE_ID)
                .type(Page.Type.MARKDOWN)
                .referenceId(API_ID)
                .referenceType(Page.ReferenceType.API)
                .name(PAGE_NAME)
                .content(PAGE_CONTENT)
                .build()
        );
    }

    private static Document expected() {
        var document = new Document();
        document.add(new StringField("id", PAGE_ID, Field.Store.YES));
        document.add(new StringField("type", "page", Field.Store.YES));
        document.add(new StringField("reference_type", "api", Field.Store.NO));
        document.add(new StringField("reference_id", API_ID, Field.Store.YES));
        document.add(new StringField("name", PAGE_NAME, Field.Store.NO));
        document.add(new StringField("name_lowercase", PAGE_NAME.toLowerCase(), Field.Store.NO));
        document.add(new TextField("name_split", PAGE_NAME, Field.Store.NO));
        document.add(new TextField("content", PAGE_CONTENT, Field.Store.NO));
        return document;
    }
}
