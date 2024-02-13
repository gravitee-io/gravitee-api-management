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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.PageDocumentTransformer.*;

import io.gravitee.apim.core.search.model.IndexablePage;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.springframework.stereotype.Component;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IndexablePageDocumentTransformer implements DocumentTransformer<IndexablePage> {

    @Override
    public Document transform(IndexablePage indexable) {
        var document = new Document();

        document.add(new StringField(FIELD_ID, indexable.getId(), Field.Store.YES));
        document.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));

        if (indexable.getReferenceId() != null) {
            document.add(new StringField(FIELD_REFERENCE_TYPE, indexable.getReferenceType().toLowerCase(), Field.Store.NO));
            document.add(new StringField(FIELD_REFERENCE_ID, indexable.getReferenceId(), Field.Store.YES));
        }

        var page = indexable.getPage();

        if (indexable.getPage().getName() != null) {
            document.add(new StringField(FIELD_NAME, page.getName(), Field.Store.NO));
            document.add(new StringField(FIELD_NAME_LOWERCASE, page.getName().toLowerCase(), Field.Store.NO));
            document.add(new TextField(FIELD_NAME_SPLIT, page.getName(), Field.Store.NO));
        }

        if (indexable.getPage().getContent() != null) {
            document.add(new TextField(FIELD_CONTENT, page.getContent(), Field.Store.NO));
        }

        return document;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return IndexablePage.class.isAssignableFrom(source);
    }
}
