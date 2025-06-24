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

import static io.gravitee.apim.core.utils.CollectionUtils.stream;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;

import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import jakarta.annotation.Nullable;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
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

    private final ApiService apiService;
    private final Collator collator = Collator.getInstance(Locale.ENGLISH);

    public ApiDocumentTransformer(@Lazy ApiService apiService) {
        this.apiService = apiService;
    }

    @Override
    public Document transform(GenericApiEntity api) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, api.getId(), YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, YES));

        // If no definition version or name, the api is being deleted. No need for more info in doc.
        if (api.getDefinitionVersion() == null && api.getName() == null) {
            return doc;
        }

        if (api.getDefinitionVersion() != null) {
            doc.add(new StringField(FIELD_DEFINITION_VERSION, api.getDefinitionVersion().getLabel(), NO));
        }

        if (api.getReferenceId() != null) {
            doc.add(new StringField(FIELD_REFERENCE_TYPE, api.getReferenceType(), NO));
            doc.add(new StringField(FIELD_REFERENCE_ID, api.getReferenceId(), NO));
        }

        if (api.getName() != null) {
            doc.add(new StringField(FIELD_NAME, api.getName(), NO));
            doc.add(new SortedDocValuesField(FIELD_NAME_SORTED, toSortedValue(api.getName())));
            doc.add(new StringField(FIELD_NAME_LOWERCASE, api.getName().toLowerCase(), NO));
            doc.add(new TextField(FIELD_NAME_SPLIT, api.getName(), NO));
        }
        if (api.getDescription() != null) {
            doc.add(new StringField(FIELD_DESCRIPTION, api.getDescription(), NO));
            doc.add(new StringField(FIELD_DESCRIPTION_LOWERCASE, api.getDescription().toLowerCase(), NO));
            doc.add(new TextField(FIELD_DESCRIPTION_SPLIT, api.getDescription(), NO));
        }
        if (api.getPrimaryOwner() != null) {
            doc.add(new StringField(FIELD_OWNER, api.getPrimaryOwner().getDisplayName(), NO));
            doc.add(new StringField(FIELD_OWNER_LOWERCASE, api.getPrimaryOwner().getDisplayName().toLowerCase(), NO));
            if (api.getPrimaryOwner().getEmail() != null) {
                doc.add(new TextField(FIELD_OWNER_MAIL, api.getPrimaryOwner().getEmail(), NO));
            }
        }

        if (api instanceof ApiEntity apiEntity && apiEntity.getProxy() != null) {
            final int[] pathIndex = { 0 };
            apiEntity
                .getProxy()
                .getVirtualHosts()
                .forEach(virtualHost -> appendPath(doc, pathIndex, virtualHost.getHost(), virtualHost.getPath()));
        } else if (api instanceof io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity && apiEntity.getListeners() != null) {
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
        // FIELD_HAS_HEALTH_CHECK
        doc.add(new StringField(FIELD_HAS_HEALTH_CHECK, Boolean.toString(hasHealthCheckEnabled(api)), NO));

        // FIELD_LABELS*
        for (String label : safeIterate(api.getLabels())) {
            doc.add(new StringField(FIELD_LABELS, label, YES));
            doc.add(new StringField(FIELD_LABELS_LOWERCASE, label.toLowerCase(), NO));
            doc.add(new TextField(FIELD_LABELS_SPLIT, label, NO));
        }

        // FIELD_CATEGORIES*
        for (String category : safeIterate(api.getCategories())) {
            doc.add(new StringField(FIELD_CATEGORIES, category, NO));
            doc.add(new TextField(FIELD_CATEGORIES_SPLIT, category, NO));
        }

        // FIELD_TAGS*
        for (String tag : safeIterate(api.getTags())) {
            doc.add(new StringField(FIELD_TAGS, tag, NO));
            doc.add(new TextField(FIELD_TAGS_SPLIT, tag, NO));
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
                    doc.add(new StringField(FIELD_METADATA, metadataValue.toString(), NO));
                    doc.add(new TextField(FIELD_METADATA_SPLIT, metadataValue.toString(), NO));
                });
        }

        if (api.getOriginContext() != null && api.getOriginContext().name() != null) {
            doc.add(new StringField(FIELD_ORIGIN, api.getOriginContext().name(), NO));
        }

        return doc;
    }

    private boolean hasHealthCheckEnabled(GenericApiEntity api) {
        return switch (api) {
            case FederatedApiEntity fed -> false;
            case NativeApiEntity nat -> false;
            case io.gravitee.rest.api.model.v4.api.ApiEntity v4 -> stream(v4.getEndpointGroups())
                .anyMatch(eg ->
                    hasHealthCheckEnabled(eg.getServices(), EndpointGroupServices::getHealthCheck) ||
                    stream(eg.getEndpoints()).anyMatch(e -> hasHealthCheckEnabled(e.getServices(), EndpointServices::getHealthCheck))
                );
            case ApiEntity v2 -> apiService.hasHealthCheckEnabled(v2, false);
            default -> false;
        };
    }

    private <T> boolean hasHealthCheckEnabled(T obj, Function<T, Service> endpointServices) {
        return obj != null && endpointServices.apply(obj) != null && endpointServices.apply(obj).isEnabled();
    }

    private void appendPath(final Document doc, final int[] pathIndex, final String host, final String path) {
        doc.add(new StringField(FIELD_PATHS, path, NO));
        doc.add(new TextField(FIELD_PATHS_SPLIT, path, NO));
        if (host != null && !host.isEmpty()) {
            doc.add(new StringField(FIELD_HOSTS, host, NO));
            doc.add(new TextField(FIELD_HOSTS_SPLIT, host, NO));
        }
        if (pathIndex[0]++ == 0) {
            doc.add(new SortedDocValuesField(FIELD_PATHS_SORTED, toSortedValue(path)));
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
        return GenericApiEntity.class.isAssignableFrom(source);
    }

    private <T> Collection<T> safeIterate(@Nullable Collection<T> l) {
        return l != null ? l : List.of();
    }
}
