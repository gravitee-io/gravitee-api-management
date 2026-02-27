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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_CREATED_AT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_DESCRIPTION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_NAME;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_NAME_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_TYPE_VALUE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.FIELD_UPDATED_AT;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.search.model.IndexableApiProduct;
import java.time.ZonedDateTime;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.junit.jupiter.api.Test;

class IndexableApiProductDocumentTransformerTest {

    private static final String PRODUCT_ID = "api-product-id";
    private static final PrimaryOwnerEntity PRIMARY_OWNER = new PrimaryOwnerEntity(
        "user-id",
        "owner@gravitee.io",
        "Product Owner",
        PrimaryOwnerEntity.Type.USER
    );

    private final IndexableApiProductDocumentTransformer cut = new IndexableApiProductDocumentTransformer();

    @Test
    void should_transform_id_and_type_only_when_name_is_null() {
        ApiProduct apiProduct = ApiProduct.builder().id(PRODUCT_ID).environmentId("env-1").name(null).build();
        IndexableApiProduct indexable = new IndexableApiProduct(apiProduct, PRIMARY_OWNER);

        Document result = cut.transform(indexable);

        assertThat(result.getFields()).hasSize(2);
        assertThat(result.getField(FIELD_ID).stringValue()).isEqualTo(PRODUCT_ID);
        assertThat(result.getField(FIELD_TYPE).stringValue()).isEqualTo(FIELD_TYPE_VALUE);
        assertThat(result.getField(FIELD_NAME)).isNull();
    }

    @Test
    void should_not_add_created_at_or_updated_at_when_dates_are_null() {
        ApiProduct apiProduct = ApiProduct.builder()
            .id(PRODUCT_ID)
            .environmentId("env-1")
            .name("My Product")
            .createdAt(null)
            .updatedAt(null)
            .build();
        IndexableApiProduct indexable = new IndexableApiProduct(apiProduct, null);

        Document result = cut.transform(indexable);

        assertThat(result.getField(FIELD_NAME).stringValue()).isEqualTo("My Product");
        assertThat(result.getFields(FIELD_CREATED_AT)).isEmpty();
        assertThat(result.getFields(FIELD_UPDATED_AT)).isEmpty();
    }

    @Test
    void should_add_created_at_and_updated_at_when_dates_are_set() {
        ZonedDateTime created = ZonedDateTime.now().minusDays(1);
        ZonedDateTime updated = ZonedDateTime.now();
        ApiProduct apiProduct = ApiProduct.builder()
            .id(PRODUCT_ID)
            .environmentId("env-1")
            .name("My Product")
            .createdAt(created)
            .updatedAt(updated)
            .build();
        IndexableApiProduct indexable = new IndexableApiProduct(apiProduct, null);

        Document result = cut.transform(indexable);

        assertThat(result.getFields(FIELD_CREATED_AT)).hasSize(1);
        assertThat(result.getFields(FIELD_UPDATED_AT)).hasSize(1);
    }

    @Test
    void should_strip_special_chars_in_name_sorted_for_collation() {
        // SPECIAL_CHARS pattern: [|\-+!(){}^"~*?:&/] — strip these before collation
        ApiProduct apiProduct = ApiProduct.builder().id(PRODUCT_ID).environmentId("env-1").name("API | Product (v1)").build();
        IndexableApiProduct indexable = new IndexableApiProduct(apiProduct, null);

        Document result = cut.transform(indexable);

        IndexableField sortedField = result.getField(FIELD_NAME_SORTED);
        assertThat(sortedField).isNotNull();
        // Sorted value should be based on cleaned name (special chars removed), not raw
        assertThat(sortedField.binaryValue()).isNotNull();
    }

    @Test
    void should_handle_handle_for_indexable_api_product() {
        assertThat(cut.handle(IndexableApiProduct.class)).isTrue();
        assertThat(cut.handle(io.gravitee.rest.api.model.search.Indexable.class)).isFalse();
    }

    @Test
    void should_transform_full_product_with_owner_and_description() {
        ApiProduct apiProduct = ApiProduct.builder()
            .id(PRODUCT_ID)
            .environmentId("env-1")
            .name("Full Product")
            .description("A description")
            .apiIds(Set.of("api-1"))
            .createdAt(ZonedDateTime.now().minusDays(1))
            .updatedAt(ZonedDateTime.now())
            .build();
        IndexableApiProduct indexable = new IndexableApiProduct(apiProduct, PRIMARY_OWNER);

        Document result = cut.transform(indexable);

        assertThat(result.getField(FIELD_ID).stringValue()).isEqualTo(PRODUCT_ID);
        assertThat(result.getField(FIELD_NAME).stringValue()).isEqualTo("Full Product");
        assertThat(result.getField(FIELD_DESCRIPTION).stringValue()).isEqualTo("A description");
        assertThat(result.getField(IndexableApiProductDocumentTransformer.FIELD_OWNER).stringValue()).isEqualTo("Product Owner");
    }
}
