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
package io.gravitee.management.service.impl.search.lucene.transformer;

import io.gravitee.management.model.UserEntity;
import io.gravitee.management.model.search.Indexable;
import io.gravitee.management.service.impl.search.lucene.DocumentTransformer;
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
public class UserDocumentTransformer implements DocumentTransformer<UserEntity> {

    private final static String FIELD_ID = "id";
    private final static String FIELD_TYPE = "type";
    private final static String FIELD_TYPE_VALUE = "user";
    private final static String FIELD_FIRSTNAME = "firstname";
    private final static String FIELD_LASTNAME = "lastname";
    private final static String FIELD_DISPLAYNAME = "displayname";
    private final static String FIELD_DISPLAYNAME_SPLIT = "displayname_split";
    private final static String FIELD_EMAIL = "email";
    private final static String FIELD_SOURCE = "source";
    private final static String FIELD_REFERENCE = "reference";

    @Override
    public Document transform(UserEntity user) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, user.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));
        doc.add(new StringField(FIELD_SOURCE, user.getSource(), Field.Store.NO));
        doc.add(new StringField(FIELD_REFERENCE, user.getSourceId(), Field.Store.NO));

        if (user.getDisplayName() != null) {
            doc.add(new StringField(FIELD_DISPLAYNAME, user.getDisplayName(), Field.Store.NO));
            doc.add(new TextField(FIELD_DISPLAYNAME_SPLIT, user.getDisplayName(), Field.Store.NO));
        }
        if (user.getFirstname() != null) {
            doc.add(new StringField(FIELD_FIRSTNAME, user.getFirstname(), Field.Store.NO));
        }

        if (user.getLastname() != null) {
            doc.add(new StringField(FIELD_LASTNAME, user.getLastname(), Field.Store.NO));
        }

        if (user.getEmail() != null) {
            doc.add(new StringField(FIELD_EMAIL, user.getEmail(), Field.Store.NO));
        }

        return doc;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return UserEntity.class.isAssignableFrom(source);
    }
}
