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
package io.gravitee.rest.api.service.impl.search.lucene.transformer;

import io.gravitee.rest.api.model.ApiPageEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PageDocumentTransformer implements DocumentTransformer<PageEntity> {

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_API = "api";
    private static final String FIELD_TYPE_VALUE = "page";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_NAME_LOWERCASE = "name_lowercase";
    private static final String FIELD_NAME_SPLIT = "name_split";
    private static final String FIELD_CONTENT = "content";

    @Override
    public Document transform(PageEntity page) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, page.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));

        if (page.getReferenceId() != null) {
            doc.add(new StringField(FIELD_REFERENCE_TYPE, page.getReferenceType(), Field.Store.NO));
            doc.add(new StringField(FIELD_REFERENCE_ID, page.getReferenceId(), Field.Store.NO));
        }

        if (page.getName() != null) {
            doc.add(new StringField(FIELD_NAME, page.getName(), Field.Store.NO));
            doc.add(new StringField(FIELD_NAME_LOWERCASE, page.getName().toLowerCase(), Field.Store.NO));
            doc.add(new TextField(FIELD_NAME_SPLIT, page.getName(), Field.Store.NO));
        }

        if (page.getContent() != null) {
            doc.add(new TextField(FIELD_CONTENT, page.getContent(), Field.Store.NO));
        }

        if (page instanceof ApiPageEntity && ((ApiPageEntity) page).getApi() != null) {
            doc.add(new StringField(FIELD_API, ((ApiPageEntity) page).getApi(), Field.Store.YES));
        }

        return doc;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return PageEntity.class.isAssignableFrom(source);
    }
}
