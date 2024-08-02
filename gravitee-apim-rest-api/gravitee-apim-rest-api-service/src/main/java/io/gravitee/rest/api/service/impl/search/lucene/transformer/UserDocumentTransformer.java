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

import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.AttributeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final String FIELD_DISPLAYNAME = "displayname";
    private static final String FIELD_DISPLAYNAME_REVERTED = "displayname_reverted";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_REFERENCE = "reference";
    private static final String FIELD_CUSTOM = "custom";
    private static final String FIELD_CUSTOM_SPLIT = "custom_split";

    private final Logger logger = LoggerFactory.getLogger(UserDocumentTransformer.class);

    @Autowired
    private Analyzer defaultAnalyzer;

    private Analyzer userFieldAnalyzer() {
        try {
            return CustomAnalyzer
                .builder()
                .withTokenizer(SingleTokenTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(ASCIIFoldingFilterFactory.class)
                .build();
        } catch (IOException e) {
            return defaultAnalyzer;
        }
    }

    @Override
    public Document transform(UserEntity user) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, user.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));

        if (user.getReferenceId() != null) {
            doc.add(new StringField(FIELD_REFERENCE_TYPE, user.getReferenceType(), Field.Store.NO));
            doc.add(new StringField(FIELD_REFERENCE_ID, user.getReferenceId(), Field.Store.NO));
        }

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
            TextField displayName = new TextField(FIELD_DISPLAYNAME, toLowerCaseAndStripAccents(user.getDisplayName()), Field.Store.NO);
            displayName.setTokenStream(
                userFieldAnalyzer().tokenStream(FIELD_DISPLAYNAME, toLowerCaseAndStripAccents(user.getDisplayName()))
            );
            doc.add(displayName);

            if (!StringUtils.isEmpty(user.getLastname()) && !StringUtils.isEmpty(user.getFirstname())) {
                String reverted = toLowerCaseAndStripAccents(String.format("%s %s", user.getLastname(), user.getFirstname()));
                TextField displayNameReverted = new TextField(FIELD_DISPLAYNAME_REVERTED, reverted, Field.Store.NO);
                displayNameReverted.setTokenStream(userFieldAnalyzer().tokenStream(FIELD_DISPLAYNAME_REVERTED, reverted));
                doc.add(displayNameReverted);
            }
        }

        if (user.getEmail() != null && user.getEmail().length() > 0) {
            if (user.getEmail().indexOf('@') < 0) {
                logger.warn("Email of the user {} is not valid", user.getId());
            } else {
                // For security reasons, we remove the domain part of the email
                doc.add(new StringField(FIELD_EMAIL, user.getEmail().substring(0, user.getEmail().indexOf('@')), Field.Store.NO));
            }
        }

        if (user.getCustomFields() != null && !user.getCustomFields().isEmpty()) {
            user
                .getCustomFields()
                .values()
                .stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(Predicate.not(String::isEmpty))
                .forEach(customValue -> {
                    doc.add(new StringField(FIELD_CUSTOM, toLowerCaseAndStripAccents(customValue), Field.Store.NO));
                    doc.add(new TextField(FIELD_CUSTOM_SPLIT, toLowerCaseAndStripAccents(customValue), Field.Store.NO));
                });
        }

        return doc;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return UserEntity.class.isAssignableFrom(source);
    }

    /**
     * Return the field content as single token
     */
    public static class SingleTokenTokenizerFactory extends TokenizerFactory {

        public SingleTokenTokenizerFactory(Map<String, String> args) {
            super(args);
        }

        @Override
        public Tokenizer create(AttributeFactory attributeFactory) {
            return new CharTokenizer() {
                @Override
                protected boolean isTokenChar(int i) {
                    return true;
                }
            };
        }
    }

    private String toLowerCaseAndStripAccents(String toTransform) {
        if (!StringUtils.isEmpty(toTransform)) {
            return StringUtils.stripAccents(toTransform.toLowerCase());
        }
        return toTransform;
    }
}
