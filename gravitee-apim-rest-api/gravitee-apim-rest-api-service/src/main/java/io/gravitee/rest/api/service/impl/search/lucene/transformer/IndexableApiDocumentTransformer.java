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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CATEGORIES;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CATEGORIES_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_CREATED_AT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DEFINITION_VERSION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_DESCRIPTION_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_HOSTS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_HOSTS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ID;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_LABELS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_LABELS_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_LABELS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_METADATA;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_METADATA_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_NAME_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_ORIGIN;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_OWNER;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_OWNER_LOWERCASE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_OWNER_MAIL;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_PATHS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_PATHS_SORTED;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_PATHS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TAGS_SPLIT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_TYPE_VALUE;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.FIELD_UPDATED_AT;
import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.SPECIAL_CHARS;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Base64;
import java.util.Locale;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;

@Component
public class IndexableApiDocumentTransformer implements DocumentTransformer<IndexableApi> {

    private final Collator collator = Collator.getInstance(Locale.ENGLISH);

    @Override
    public Document transform(IndexableApi indexableApi) {
        var api = indexableApi.getApi();
        var primaryOwner = indexableApi.getPrimaryOwner();
        var metadata = indexableApi.getDecodedMetadata();
        var categories = indexableApi.getCategoryKeys();

        if (!accept(indexableApi)) {
            throw new TechnicalDomainException("Unsupported definition version: " + api.getDefinitionVersion());
        }

        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, api.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));

        // If no definition version or name, the api is being deleted. No need for more info in doc.
        if (api.getDefinitionVersion() == null && api.getName() == null) {
            return doc;
        }

        if (api.getDefinitionVersion() != null) {
            doc.add(new StringField(FIELD_DEFINITION_VERSION, api.getDefinitionVersion().getLabel(), Field.Store.NO));
        }

        if (indexableApi.getReferenceId() != null) {
            doc.add(new StringField(FIELD_REFERENCE_TYPE, indexableApi.getReferenceType(), Field.Store.NO));
            doc.add(new StringField(FIELD_REFERENCE_ID, indexableApi.getReferenceId(), Field.Store.NO));
        }

        if (api.getName() != null) {
            doc.add(new StringField(FIELD_NAME, api.getName(), Field.Store.NO));
            doc.add(new SortedDocValuesField(FIELD_NAME_SORTED, toSortedValue(api.getName())));
            doc.add(new StringField(FIELD_NAME_LOWERCASE, api.getName().toLowerCase(), Field.Store.NO));
            doc.add(new TextField(FIELD_NAME_SPLIT, api.getName(), Field.Store.NO));
        }
        if (api.getDescription() != null) {
            doc.add(new StringField(FIELD_DESCRIPTION, api.getDescription(), Field.Store.NO));
            doc.add(new StringField(FIELD_DESCRIPTION_LOWERCASE, api.getDescription().toLowerCase(), Field.Store.NO));
            doc.add(new TextField(FIELD_DESCRIPTION_SPLIT, api.getDescription(), Field.Store.NO));
        }
        if (primaryOwner != null) {
            doc.add(new StringField(FIELD_OWNER, primaryOwner.displayName(), Field.Store.NO));
            doc.add(new StringField(FIELD_OWNER_LOWERCASE, primaryOwner.displayName().toLowerCase(), Field.Store.NO));
            if (primaryOwner.email() != null) {
                doc.add(new TextField(FIELD_OWNER_MAIL, primaryOwner.email(), Field.Store.NO));
            }
        }

        // labels
        if (api.getLabels() != null) {
            for (String label : api.getLabels()) {
                doc.add(new StringField(FIELD_LABELS, label, Field.Store.YES));
                doc.add(new StringField(FIELD_LABELS_LOWERCASE, label.toLowerCase(), Field.Store.NO));
                doc.add(new TextField(FIELD_LABELS_SPLIT, label, Field.Store.NO));
            }
        }

        // categories
        if (categories != null) {
            for (String category : categories) {
                doc.add(new StringField(FIELD_CATEGORIES, category, Field.Store.NO));
                doc.add(new TextField(FIELD_CATEGORIES_SPLIT, category, Field.Store.NO));
            }
        }

        if (api.getCreatedAt() != null) {
            doc.add(new LongPoint(FIELD_CREATED_AT, api.getCreatedAt().toInstant().toEpochMilli()));
        }
        if (api.getUpdatedAt() != null) {
            doc.add(new LongPoint(FIELD_UPDATED_AT, api.getUpdatedAt().toInstant().toEpochMilli()));
        }

        // metadata
        if (metadata != null) {
            metadata
                .values()
                .forEach(metadataValue -> {
                    doc.add(new StringField(FIELD_METADATA, metadataValue, Field.Store.NO));
                    doc.add(new TextField(FIELD_METADATA_SPLIT, metadataValue, Field.Store.NO));
                });
        }

        if (api.getOriginContext() != null && api.getOriginContext().name() != null) {
            doc.add(new StringField(FIELD_ORIGIN, api.getOriginContext().name(), Field.Store.NO));
        }

        if (api.getDefinitionVersion() == DefinitionVersion.V4) {
            transformV4Api(doc, indexableApi);
        }

        return doc;
    }

    private boolean accept(IndexableApi indexableApi) {
        Api api = indexableApi.getApi();

        if (api.getDefinitionVersion() != null) {
            return switch (api.getDefinitionVersion()) {
                case V4, FEDERATED, FEDERATED_AGENT -> true;
                case V1, V2 -> false;
            };
        }
        return true;
    }

    private void transformV4Api(Document doc, IndexableApi api) {
        var apiDefinitionV4 = api.getApi().getType() == ApiType.NATIVE
            ? api.getApi().getApiDefinitionNativeV4()
            : api.getApi().getApiDefinitionHttpV4();

        if (api.getApi().getType() == ApiType.NATIVE) {
            transformV4ApiNativeListeners(doc, api.getApi().getApiDefinitionNativeV4());
        } else {
            transformV4ApiHttpListeners(doc, api.getApi().getApiDefinitionHttpV4());
        }

        // tags
        if (apiDefinitionV4.getTags() != null) {
            for (String tag : apiDefinitionV4.getTags()) {
                doc.add(new StringField(FIELD_TAGS, tag, Field.Store.NO));
                doc.add(new TextField(FIELD_TAGS_SPLIT, tag, Field.Store.NO));
            }
        }
    }

    private void transformV4ApiHttpListeners(Document doc, io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        if (apiDefinitionV4 != null && apiDefinitionV4.getListeners() != null) {
            final int[] pathIndex = { 0 };
            apiDefinitionV4
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.HTTP)
                .flatMap(listener -> {
                    HttpListener httpListener = (HttpListener) listener;
                    return httpListener.getPaths().stream();
                })
                .forEach(path -> {
                    appendPath(doc, pathIndex, path.getPath());
                    appendHost(doc, path.getHost());
                });
        }
    }

    private void transformV4ApiNativeListeners(Document doc, io.gravitee.definition.model.v4.nativeapi.NativeApi apiDefinitionV4) {
        if (apiDefinitionV4 != null && apiDefinitionV4.getListeners() != null) {
            apiDefinitionV4
                .getListeners()
                .stream()
                .filter(listener -> listener.getType() == ListenerType.KAFKA)
                .forEach(listener -> {
                    if (listener instanceof KafkaListener kafkaListener) appendHost(doc, kafkaListener.getHost());
                });
        }
    }

    private void appendPath(final Document doc, final int[] pathIndex, final String path) {
        doc.add(new StringField(FIELD_PATHS, path, Field.Store.NO));
        doc.add(new TextField(FIELD_PATHS_SPLIT, path, Field.Store.NO));
        if (pathIndex[0]++ == 0) {
            doc.add(new SortedDocValuesField(FIELD_PATHS_SORTED, toSortedValue(path)));
        }
    }

    private void appendHost(Document doc, String host) {
        if (host != null && !host.isEmpty()) {
            doc.add(new StringField(FIELD_HOSTS, host, Field.Store.NO));
            doc.add(new TextField(FIELD_HOSTS_SPLIT, host, Field.Store.NO));
        }
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
        return IndexableApi.class.isAssignableFrom(source);
    }
}
