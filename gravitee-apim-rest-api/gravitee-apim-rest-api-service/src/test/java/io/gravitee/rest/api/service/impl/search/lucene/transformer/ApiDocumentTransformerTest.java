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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexableField;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiDocumentTransformerTest {

    ApiDocumentTransformer cut = new ApiDocumentTransformer();

    @Test
    public void shouldTransform() {
        ApiEntity toTransform = getApiEntity();

        Document transformed = cut.transform(toTransform);

        assertDocumentMatchesInputApiEntity(toTransform, transformed);
    }

    @Test
    public void shouldTransformWithoutError_OnMissingReferenceId() {
        ApiEntity api = new ApiEntity();
        api.setId("api-uuid");
        Document doc = cut.transform(api);
        assertThat(doc.get("id")).isEqualTo(api.getId());
    }

    @NotNull
    private ApiEntity getApiEntity() {
        ApiEntity toTransform = new ApiEntity();
        toTransform.setId("apiId");
        toTransform.setName("name");
        toTransform.setDescription("description");
        toTransform.setReferenceId("xxxxxx");
        toTransform.setReferenceType("env1");
        UserEntity userEntity = new UserEntity();
        userEntity.setId("userId");
        userEntity.setEmail("userMail");
        userEntity.setFirstname("userFirstname");
        userEntity.setLastname("userLastname");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        toTransform.setPrimaryOwner(primaryOwnerEntity);
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Arrays.asList(new VirtualHost("host", "path"), new VirtualHost("host2", "path2")));
        toTransform.setProxy(proxy);
        toTransform.setLabels(Arrays.asList("label1", "label2", "label2"));
        toTransform.setCategories(new HashSet<>(Arrays.asList("cat1", "cat2")));
        toTransform.setTags(new HashSet<>(Arrays.asList("tag1", "tag2")));
        Date date = new Date();
        toTransform.setCreatedAt(date);
        toTransform.setUpdatedAt(date);
        HashMap<String, Object> metadatas = new HashMap<>();
        metadatas.put("metadata1", "value1");
        metadatas.put("metadata2", "value2");
        metadatas.put("metadata3", "value3");
        toTransform.setMetadata(metadatas);
        return toTransform;
    }

    private void assertDocumentMatchesInputApiEntity(ApiEntity toTransform, Document transformed) {
        assertThat(toTransform.getId()).isEqualTo(transformed.get("id"));
        assertThat(toTransform.getName()).isEqualTo(transformed.get("name"));
        assertThat(toTransform.getDescription()).isEqualTo(transformed.get("description"));
        assertThat(toTransform.getPrimaryOwner().getDisplayName()).isEqualTo(transformed.get("ownerName"));
        assertThat(toTransform.getPrimaryOwner().getEmail()).isEqualTo(transformed.get("ownerMail"));
        IndexableField[] paths = transformed.getFields("paths");
        IndexableField[] hosts = transformed.getFields("hosts");
        assertThat(toTransform.getProxy().getVirtualHosts()).hasSize(paths.length);
        assertThat(toTransform.getProxy().getVirtualHosts()).hasSameSizeAs(hosts);
        assertThat(toTransform.getLabels()).hasSameSizeAs(transformed.getFields("labels"));
        assertThat(toTransform.getCategories()).hasSameSizeAs(transformed.getFields("categories"));
        assertThat(toTransform.getTags()).hasSameSizeAs(transformed.getFields("tags"));
        assertThat(toTransform.getCreatedAt().getTime()).isEqualTo(((LongPoint) transformed.getField("createdAt")).numericValue());
        assertThat(toTransform.getUpdatedAt().getTime()).isEqualTo(((LongPoint) transformed.getField("updatedAt")).numericValue());
        assertThat(toTransform.getMetadata().values()).hasSameSizeAs(transformed.getFields("metadata"));
    }
}
