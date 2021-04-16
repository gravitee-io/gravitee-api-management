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

import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserDocumentTransformer implements DocumentTransformer<UserEntity> {

    private static final String FIELD_ID = "id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TYPE_VALUE = "user";
    private static final String FIELD_FIRSTNAME = "firstname";
    private static final String FIELD_LASTNAME = "lastname";
    private static final String FIELD_DISPLAYNAME = "displayname";
    private static final String FIELD_DISPLAYNAME_SPLIT = "displayname_split";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_REFERENCE = "reference";

    private final Logger logger = LoggerFactory.getLogger(UserDocumentTransformer.class);

    @Override
    public Document transform(UserEntity user) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, user.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));
        if (user.getSource() != null) {
            doc.add(new StringField(FIELD_SOURCE, user.getSource(), Field.Store.NO));
        }
        if (user.getSourceId() != null) {
            // For security reasons, we remove the domain part of the email
            final String sourceId;
            if (user.getSourceId().contains("@")) {
                sourceId = user.getSourceId().substring(0, user.getSourceId().indexOf('@'));
            } else {
                sourceId = user.getSourceId();
            }
            doc.add(new StringField(FIELD_REFERENCE, sourceId, Field.Store.NO));
        }

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
            if (user.getEmail().indexOf('@') < 0) {
                logger.warn("Email of the user {} is not valid", user.getId());
            } else {
                // For security reasons, we remove the domain part of the email
                doc.add(new StringField(FIELD_EMAIL, user.getEmail().substring(0, user.getEmail().indexOf('@')), Field.Store.NO));
            }
        }

        return doc;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return UserEntity.class.isAssignableFrom(source);
    }
}
