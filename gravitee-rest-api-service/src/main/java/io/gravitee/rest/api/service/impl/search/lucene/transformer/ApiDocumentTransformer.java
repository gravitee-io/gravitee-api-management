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

import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentTransformer;
import org.apache.lucene.document.*;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiDocumentTransformer implements DocumentTransformer<ApiEntity> {

    private final static String FIELD_ID = "id";
    private final static String FIELD_TYPE = "type";
    private final static String FIELD_TYPE_VALUE = "api";
    private final static String FIELD_NAME = "name";
    private final static String FIELD_NAME_LOWERCASE = "name_lowercase";
    private final static String FIELD_NAME_SPLIT = "name_split";
    private final static String FIELD_DESCRIPTION = "description";
    private final static String FIELD_OWNER = "ownerName";
    private final static String FIELD_OWNER_MAIL = "ownerMail";
    private final static String FIELD_LABELS = "labels";
    private final static String FIELD_CATEGORIES = "categories";
    private final static String FIELD_CREATED_AT = "createdAt";
    private final static String FIELD_UPDATED_AT = "updatedAt";
    private final static String FIELD_PATHS = "paths";
    private final static String FIELD_HOSTS = "hosts";
    private final static String FIELD_PATHS_SPLIT = "paths_split";
    private final static String FIELD_HOSTS_SPLIT = "hosts_split";
    private final static String FIELD_TAGS = "tags";

    @Override
    public Document transform(io.gravitee.rest.api.model.api.ApiEntity api) {
        Document doc = new Document();

        doc.add(new StringField(FIELD_ID, api.getId(), Field.Store.YES));
        doc.add(new StringField(FIELD_TYPE, FIELD_TYPE_VALUE, Field.Store.YES));
        if (api.getName() != null) {
            doc.add(new StringField(FIELD_NAME, api.getName(), Field.Store.NO));
            doc.add(new StringField(FIELD_NAME_LOWERCASE, api.getName().toLowerCase(), Field.Store.NO));
            doc.add(new TextField(FIELD_NAME_SPLIT, api.getName(), Field.Store.NO));
        }
        if (api.getDescription() != null) {
            doc.add(new TextField(FIELD_DESCRIPTION, api.getDescription(), Field.Store.NO));
        }
        if (api.getPrimaryOwner() != null) {
            doc.add(new TextField(FIELD_OWNER, api.getPrimaryOwner().getDisplayName(), Field.Store.NO));
            if (api.getPrimaryOwner().getEmail() != null) {
                doc.add(new TextField(FIELD_OWNER_MAIL, api.getPrimaryOwner().getEmail(), Field.Store.NO));
            }
        }

        api.getProxy().getVirtualHosts().forEach(new Consumer<VirtualHost>() {
            @Override
            public void accept(VirtualHost virtualHost) {
                doc.add(new StringField(FIELD_PATHS, virtualHost.getPath(), Field.Store.NO));
                doc.add(new TextField(FIELD_PATHS_SPLIT, virtualHost.getPath(), Field.Store.NO));

                if (virtualHost.getHost() != null && !virtualHost.getHost().isEmpty()) {
                    doc.add(new StringField(FIELD_HOSTS, virtualHost.getHost(), Field.Store.NO));
                    doc.add(new TextField(FIELD_HOSTS_SPLIT, virtualHost.getHost(), Field.Store.NO));
                }
            }
        });

        // labels
        if (api.getLabels() != null) {
            for (String label : api.getLabels()) {
                doc.add(new TextField(FIELD_LABELS, label, Field.Store.NO));
            }
        }

        // categories
        if (api.getCategories() != null) {
            for (String category : api.getCategories()) {
                doc.add(new TextField(FIELD_CATEGORIES, category, Field.Store.NO));
            }
        }

        // tags
        if (api.getTags() != null) {
            for (String tag : api.getTags()) {
                doc.add(new TextField(FIELD_TAGS, tag, Field.Store.NO));
            }
        }

        if (api.getCreatedAt() != null) {
            doc.add(new LongPoint(FIELD_CREATED_AT, api.getCreatedAt().getTime()));
        }
        if (api.getUpdatedAt() != null) {
            doc.add(new LongPoint(FIELD_UPDATED_AT, api.getUpdatedAt().getTime()));
        }

        return doc;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return ApiEntity.class.isAssignableFrom(source);
    }
}
