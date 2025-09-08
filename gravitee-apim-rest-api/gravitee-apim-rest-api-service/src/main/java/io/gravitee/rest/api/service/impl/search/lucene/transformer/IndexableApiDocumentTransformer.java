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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.*;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.search.model.IndexableApi;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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

        if (api.getLifecycleState() != null) {
            doc.add(new StringField(FIELD_STATUS, api.getLifecycleState().name(), Field.Store.NO));
            doc.add(new SortedDocValuesField(FIELD_STATUS_SORTED, toSortedValue(api.getLifecycleState().name())));
        }

        String portalStatus = api.getApiLifecycleState() == Api.ApiLifecycleState.PUBLISHED
            ? Api.ApiLifecycleState.PUBLISHED.name()
            : Api.ApiLifecycleState.UNPUBLISHED.name();
        doc.add(new StringField(FIELD_PORTAL_STATUS, portalStatus, Field.Store.NO));
        doc.add(new SortedDocValuesField(FIELD_PORTAL_STATUS_SORTED, toSortedValue(portalStatus)));

        doc.add(new StringField(FIELD_VISIBILITY, api.getVisibility().name(), Field.Store.NO));
        doc.add(new SortedDocValuesField(FIELD_VISIBILITY_SORTED, toSortedValue(api.getVisibility().name())));

        if (api.getDefinitionVersion() != null) {
            doc.add(new StringField(FIELD_DEFINITION_VERSION, api.getDefinitionVersion().getLabel(), Field.Store.NO));
            String apiType = generateApiType(api);
            doc.add(new StringField(FIELD_API_TYPE, apiType, Field.Store.NO));
            doc.add(new SortedDocValuesField(FIELD_API_TYPE_SORTED, toSortedValue(apiType)));
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
            doc.add(new SortedDocValuesField(FIELD_OWNER_SORTED, toSortedValue(primaryOwner.displayName())));
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
        if (categories != null && !categories.isEmpty()) {
            for (String category : categories) {
                doc.add(new StringField(FIELD_CATEGORIES, category, Field.Store.NO));
                doc.add(new TextField(FIELD_CATEGORIES_SPLIT, category, Field.Store.NO));
            }
            String categoriesAsc = categories.stream().sorted().collect(Collectors.joining(","));
            String categoriesDesc = categories.stream().sorted(Comparator.reverseOrder()).collect(Collectors.joining(","));
            doc.add(new SortedDocValuesField(FIELD_CATEGORIES_ASC_SORTED, toSortedValue(categoriesAsc)));
            doc.add(new SortedDocValuesField(FIELD_CATEGORIES_DESC_SORTED, toSortedValue(categoriesDesc)));
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
        } else if (api.getDefinitionVersion() == DefinitionVersion.V2) {
            transformV2Api(doc, indexableApi);
        }

        return doc;
    }

    String generateApiType(Api api) {
        String apiType;
        if (api.getDefinitionVersion() == DefinitionVersion.V4) {
            String type = api.getType() == ApiType.NATIVE ? "KAFKA" : api.getType() == ApiType.PROXY ? "HTTP_PROXY" : "MESSAGE";
            apiType = api.getDefinitionVersion().name() + "_" + type;
        } else {
            apiType = api.getDefinitionVersion().name();
        }
        return apiType;
    }

    private boolean accept(IndexableApi indexableApi) {
        Api api = indexableApi.getApi();

        if (api.getDefinitionVersion() != null) {
            return switch (api.getDefinitionVersion()) {
                case V4, FEDERATED, FEDERATED_AGENT, V2 -> true;
                case V1 -> false;
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
        if (apiDefinitionV4.getTags() != null && !apiDefinitionV4.getTags().isEmpty()) {
            for (String tag : apiDefinitionV4.getTags()) {
                doc.add(new StringField(FIELD_TAGS, tag, Field.Store.NO));
                doc.add(new TextField(FIELD_TAGS_SPLIT, tag, Field.Store.NO));
            }
            String tagsAsc = apiDefinitionV4.getTags().stream().sorted().collect(Collectors.joining(","));
            String tagsDesc = apiDefinitionV4.getTags().stream().sorted(Comparator.reverseOrder()).collect(Collectors.joining(","));
            doc.add(new SortedDocValuesField(FIELD_TAGS_ASC_SORTED, toSortedValue(tagsAsc)));
            doc.add(new SortedDocValuesField(FIELD_TAGS_DESC_SORTED, toSortedValue(tagsDesc)));
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

    private void transformV2Api(Document doc, IndexableApi indexableApi) {
        var api = indexableApi.getApi();

        // Handle v2 API paths from proxy configuration
        if (api.getApiDefinition() != null && api.getApiDefinition().getProxy() != null) {
            final int[] pathIndex = { 0 };
            api
                .getApiDefinition()
                .getProxy()
                .getVirtualHosts()
                .forEach(virtualHost -> appendPath(doc, pathIndex, virtualHost.getHost(), virtualHost.getPath()));
        }

        // Handle v2 API tags
        if (api.getApiDefinition() != null && api.getApiDefinition().getTags() != null && !api.getApiDefinition().getTags().isEmpty()) {
            for (String tag : api.getApiDefinition().getTags()) {
                doc.add(new StringField(FIELD_TAGS, tag, Field.Store.NO));
                doc.add(new TextField(FIELD_TAGS_SPLIT, tag, Field.Store.NO));
            }
            String tagsAsc = api.getApiDefinition().getTags().stream().sorted().collect(Collectors.joining(","));
            String tagsDesc = api.getApiDefinition().getTags().stream().sorted(Comparator.reverseOrder()).collect(Collectors.joining(","));
            doc.add(new SortedDocValuesField(FIELD_TAGS_ASC_SORTED, toSortedValue(tagsAsc)));
            doc.add(new SortedDocValuesField(FIELD_TAGS_DESC_SORTED, toSortedValue(tagsDesc)));
        }

        // Handle v2 API health check
        doc.add(new StringField(FIELD_HAS_HEALTH_CHECK, Boolean.toString(hasHealthCheckEnabledV2(api)), Field.Store.NO));
    }

    private boolean hasHealthCheckEnabledV2(Api api) {
        if (api.getApiDefinition() == null) {
            return false;
        }

        var apiDefinition = api.getApiDefinition();

        // Check for global health check services at API level
        if (apiDefinition.getServices() != null) {
            boolean hasGlobalHealthCheck = apiDefinition
                .getServices()
                .getAll()
                .stream()
                .anyMatch(service -> service.isEnabled() && service instanceof HealthCheckService);
            if (hasGlobalHealthCheck) {
                return true;
            }
        }

        // Check for endpoint-level health checks
        if (apiDefinition.getProxy() != null && apiDefinition.getProxy().getGroups() != null) {
            return apiDefinition
                .getProxy()
                .getGroups()
                .stream()
                .anyMatch(group ->
                    group.getEndpoints() != null &&
                    group
                        .getEndpoints()
                        .stream()
                        .anyMatch(endpoint -> endpoint.getHealthCheck() != null && endpoint.getHealthCheck().isEnabled())
                );
        }

        return false;
    }

    private void appendPath(final Document doc, final int[] pathIndex, final String host, final String path) {
        doc.add(new StringField(FIELD_PATHS, path, Field.Store.NO));
        doc.add(new TextField(FIELD_PATHS_SPLIT, path, Field.Store.NO));
        if (host != null && !host.isEmpty()) {
            doc.add(new StringField(FIELD_HOSTS, host, Field.Store.NO));
            doc.add(new TextField(FIELD_HOSTS_SPLIT, host, Field.Store.NO));
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
        return IndexableApi.class.isAssignableFrom(source);
    }
}
