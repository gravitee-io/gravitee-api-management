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

import static io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer.FIELD_REFERENCE_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer.FIELD_REFERENCE_TYPE;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;

@Component
public class IndexableApiProductDocumentTransformer implements DocumentTransformer<IndexableApiProduct> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TYPE_VALUE = "api_product";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAME_LOWERCASE = "name_lowercase";
    public static final String FIELD_NAME_SORTED = "name_sorted";
    public static final String FIELD_NAME_SPLIT = "name_split";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DESCRIPTION_LOWERCASE = "description_lowercase";
    public static final String FIELD_DESCRIPTION_SPLIT = "description_split";
    public static final String FIELD_OWNER = "ownerName";
    public static final String FIELD_OWNER_LOWERCASE = "ownerName_lowercase";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    public static final Pattern SPECIAL_CHARS = Pattern.compile("[|\\-+!(){}^\"~*?:&\\/]");

    private final Collator collator = Collator.getInstance(Locale.ENGLISH);

    @Override
    public Document transform(IndexableApiProduct indexableApiProduct) {
        ApiProduct apiProduct = indexableApiProduct.getApiProduct();
        PrimaryOwnerEntity primaryOwner = indexableApiProduct.getPrimaryOwner();

        Document doc = new Document();
        doc.add(new StringField(FIELD_ID, apiProduct.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));

        if (apiProduct.getName() == null) {
            return doc;
        }

        if (indexableApiProduct.getReferenceId() != null) {
            doc.add(new StringField(FIELD_REFERENCE_TYPE, indexableApiProduct.getReferenceType(), Field.Store.NO));
            doc.add(new StringField(FIELD_REFERENCE_ID, indexableApiProduct.getReferenceId(), Field.Store.NO));
        }

        doc.add(new StringField(FIELD_NAME, apiProduct.getName(), Field.Store.NO));
        doc.add(new SortedDocValuesField(FIELD_NAME_SORTED, toSortedValue(apiProduct.getName())));
        doc.add(new StringField(FIELD_NAME_LOWERCASE, apiProduct.getName().toLowerCase(), Field.Store.NO));
        doc.add(new TextField(FIELD_NAME_SPLIT, apiProduct.getName(), Field.Store.NO));

        if (apiProduct.getDescription() != null) {
            doc.add(new StringField(FIELD_DESCRIPTION, apiProduct.getDescription(), Field.Store.NO));
            doc.add(new StringField(FIELD_DESCRIPTION_LOWERCASE, apiProduct.getDescription().toLowerCase(), Field.Store.NO));
            doc.add(new TextField(FIELD_DESCRIPTION_SPLIT, apiProduct.getDescription(), Field.Store.NO));
        }

        if (apiProduct.getCreatedAt() != null) {
            doc.add(new LongPoint(FIELD_CREATED_AT, apiProduct.getCreatedAt().toInstant().toEpochMilli()));
        }
        if (apiProduct.getUpdatedAt() != null) {
            doc.add(new LongPoint(FIELD_UPDATED_AT, apiProduct.getUpdatedAt().toInstant().toEpochMilli()));
        }

        if (primaryOwner != null && primaryOwner.displayName() != null) {
            doc.add(new StringField(FIELD_OWNER, primaryOwner.displayName(), Field.Store.NO));
            doc.add(new StringField(FIELD_OWNER_LOWERCASE, primaryOwner.displayName().toLowerCase(), Field.Store.NO));
        }

        return doc;
    }

    private BytesRef toSortedValue(String value) {
        if (value == null) return new BytesRef("");
        String cleaned = SPECIAL_CHARS.matcher(value).replaceAll("");
        collator.setStrength(Collator.SECONDARY);
        CollationKey key = collator.getCollationKey(cleaned);
        return new BytesRef(key.toByteArray());
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return IndexableApiProduct.class.isAssignableFrom(source);
    }
}
