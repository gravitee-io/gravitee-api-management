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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.util.BytesRef;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiDocumentTransformer implements DocumentTransformer<GenericApiEntity> {

    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_TYPE_VALUE = "api";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_NAME_LOWERCASE = "name_lowercase";
    public static final String FIELD_NAME_SORTED = "name_sorted";
    public static final String FIELD_NAME_SPLIT = "name_split";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_DESCRIPTION_LOWERCASE = "description_lowercase";
    public static final String FIELD_DESCRIPTION_SPLIT = "description_split";
    public static final String FIELD_OWNER = "ownerName";
    public static final String FIELD_OWNER_LOWERCASE = "ownerName_lowercase";
    public static final String FIELD_OWNER_MAIL = "ownerMail";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_LABELS_LOWERCASE = "labels_lowercase";
    public static final String FIELD_LABELS_SPLIT = "labels_split";
    public static final String FIELD_CATEGORIES = "categories";
    public static final String FIELD_CATEGORIES_SPLIT = "categories_split";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";
    public static final String FIELD_PATHS = "paths";
    public static final String FIELD_HOSTS = "hosts";
    public static final String FIELD_PATHS_SORTED = "paths_sorted";
    public static final String FIELD_PATHS_SPLIT = "paths_split";
    public static final String FIELD_HOSTS_SPLIT = "hosts_split";
    public static final String FIELD_TAGS = "tags";
    public static final String FIELD_TAGS_SPLIT = "tags_split";
    public static final String FIELD_METADATA = "metadata";
    public static final String FIELD_METADATA_SPLIT = "metadata_split";
    public static final String FIELD_DEFINITION_VERSION = "definition_version";
    public static final Pattern SPECIAL_CHARS = Pattern.compile("[|\\-+!(){}^\"~*?:&\\/]");
    public static final String FIELD_ORIGIN = "origin";
    public static final String FIELD_HAS_HEALTH_CHECK = "has_health_check";

    private ApiService apiService;

    public ApiDocumentTransformer(@Lazy ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public Document transform(GenericApiEntity api) {
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

        if (api.getReferenceId() != null) {
            doc.add(new StringField(FIELD_REFERENCE_TYPE, api.getReferenceType(), Field.Store.NO));
            doc.add(new StringField(FIELD_REFERENCE_ID, api.getReferenceId(), Field.Store.NO));
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
        if (api.getPrimaryOwner() != null) {
            doc.add(new StringField(FIELD_OWNER, api.getPrimaryOwner().getDisplayName(), Field.Store.NO));
            doc.add(new StringField(FIELD_OWNER_LOWERCASE, api.getPrimaryOwner().getDisplayName().toLowerCase(), Field.Store.NO));
            if (api.getPrimaryOwner().getEmail() != null) {
                doc.add(new TextField(FIELD_OWNER_MAIL, api.getPrimaryOwner().getEmail(), Field.Store.NO));
            }
        }

        if (api.getDefinitionVersion() != DefinitionVersion.V4 && api.getDefinitionVersion() != DefinitionVersion.FEDERATED) {
            ApiEntity apiEntity = (ApiEntity) api;
            if (apiEntity.getProxy() != null) {
                final int[] pathIndex = { 0 };
                apiEntity
                    .getProxy()
                    .getVirtualHosts()
                    .forEach(virtualHost -> {
                        appendPath(doc, pathIndex, virtualHost.getHost(), virtualHost.getPath());
                    });
            }
            doc.add(
                new StringField(
                    FIELD_HAS_HEALTH_CHECK,
                    apiService.hasHealthCheckEnabled(apiEntity, false) ? "true" : "false",
                    Field.Store.NO
                )
            );
        } else if (api instanceof io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity) {
            if (apiEntity.getListeners() != null) {
                final int[] pathIndex = { 0 };
                apiEntity
                    .getListeners()
                    .stream()
                    .filter(listener -> listener.getType() == ListenerType.HTTP)
                    .flatMap(listener -> {
                        HttpListener httpListener = (HttpListener) listener;
                        return httpListener.getPaths().stream();
                    })
                    .forEach(path -> appendPath(doc, pathIndex, path.getHost(), path.getPath()));
            }
            // TODO: add FIELD_HAS_HEALTH_CHECK for v4 api
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
        if (api.getCategories() != null) {
            for (String category : api.getCategories()) {
                doc.add(new StringField(FIELD_CATEGORIES, category, Field.Store.NO));
                doc.add(new TextField(FIELD_CATEGORIES_SPLIT, category, Field.Store.NO));
            }
        }

        // tags
        if (api.getTags() != null) {
            for (String tag : api.getTags()) {
                doc.add(new StringField(FIELD_TAGS, tag, Field.Store.NO));
                doc.add(new TextField(FIELD_TAGS_SPLIT, tag, Field.Store.NO));
            }
        }

        if (api.getCreatedAt() != null) {
            doc.add(new LongPoint(FIELD_CREATED_AT, api.getCreatedAt().getTime()));
        }
        if (api.getUpdatedAt() != null) {
            doc.add(new LongPoint(FIELD_UPDATED_AT, api.getUpdatedAt().getTime()));
        }

        // metadata
        if (api.getMetadata() != null) {
            api
                .getMetadata()
                .values()
                .forEach(metadataValue -> {
                    doc.add(new StringField(FIELD_METADATA, metadataValue.toString(), Field.Store.NO));
                    doc.add(new TextField(FIELD_METADATA_SPLIT, metadataValue.toString(), Field.Store.NO));
                });
        }

        if (api.getOriginContext() != null && api.getOriginContext().name() != null) {
            doc.add(new StringField(FIELD_ORIGIN, api.getOriginContext().name(), Field.Store.NO));
        }

        return doc;
    }

    private void appendPath(final Document doc, final int[] pathIndex, final String host, final String path) {
        doc.add(new StringField(FIELD_PATHS, path, Field.Store.NO));
        doc.add(new TextField(FIELD_PATHS_SPLIT, path, Field.Store.NO));
        if (host != null && !host.isEmpty()) {
            doc.add(new StringField(FIELD_HOSTS, host, Field.Store.NO));
            doc.add(new TextField(FIELD_HOSTS_SPLIT, host, Field.Store.NO));
        }
        if (pathIndex[0]++ == 0) {
            doc.add(new SortedDocValuesField(FIELD_PATHS_SORTED, new BytesRef(QueryParser.escape(path))));
        }
    }

    private BytesRef toSortedValue(String value) {
        return new BytesRef(SPECIAL_CHARS.matcher(value).replaceAll("").toLowerCase());
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return GenericApiEntity.class.isAssignableFrom(source);
    }
}
