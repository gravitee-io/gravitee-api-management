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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.impl.ApiServiceImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApiDocumentTransformerTest {

    @InjectMocks
    ApiDocumentTransformer cut = new ApiDocumentTransformer(new ApiServiceImpl());

    @Test
    void shouldTransform() {
        ApiEntity toTransform = getApiEntity();

        Document transformed = cut.transform(toTransform);

        assertDocumentMatchesInputApiEntity(toTransform, transformed);
    }

    @Test
    void shouldTransformWithoutError_OnMissingReferenceId() {
        ApiEntity api = new ApiEntity();
        api.setId("api-uuid");
        Document doc = cut.transform(api);
        assertThat(doc.get("id")).isEqualTo(api.getId());
    }

    @Test
    void shouldTransformWithoutError_V4ApiOnDeleteMode() {
        var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        api.setId("api-uuid");
        api.setDefinitionVersion(null);
        api.setName(null);

        Document doc = cut.transform(api);
        assertThat(doc.get("id")).isEqualTo(api.getId());
    }

    @Nested
    class HasHealthCheck {

        @Test
        void v4_as_endpoint_group() {
            var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
            api.setId("api-uuid");
            api.setDefinitionVersion(DefinitionVersion.V4);
            api.setName("name");
            api.setEndpointGroups(List.of(new EndpointGroup(new EndpointGroupServices(null, new Service(true, true, "type", "conf")))));

            Document doc = cut.transform(api);
            assertThat(doc.get("has_health_check")).isEqualTo("true");
        }

        @Test
        void v4_as_endpoint_() {
            var api = new io.gravitee.rest.api.model.v4.api.ApiEntity();
            api.setId("api-uuid");
            api.setDefinitionVersion(DefinitionVersion.V4);
            api.setName("name");
            var endpointGroup = new EndpointGroup(new EndpointGroupServices(null, null));
            endpointGroup.setEndpoints(List.of(new Endpoint(new EndpointServices(new Service(true, true, "type", "conf")))));
            api.setEndpointGroups(List.of(endpointGroup));

            Document doc = cut.transform(api);
            assertThat(doc.get("has_health_check")).isEqualTo("true");
        }
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
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setEnabled(true);
        Services services = new Services();
        services.setHealthCheckService(healthCheckService);
        toTransform.setServices(services);
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
        DefinitionContext context = new DefinitionContext();
        toTransform.setDefinitionContext(context);
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
        assertThat(toTransform.getDefinitionContext().getOrigin()).isEqualTo(transformed.get("origin"));
        assertThat("true").isEqualTo(transformed.get("has_health_check"));
    }

    @Test
    public void shouldSortListCorrectlyWithCollatorAndBytesRef() throws Exception {
        List<String> names = List.of("nano", "Zorro", "äther", "vem", "foo/bar", "Épée", "épona", "öko", "bns-One");
        List<String> expectedSorted = List.of("äther", "bns-One", "Épée", "épona", "foo/bar", "nano", "öko", "vem", "Zorro");
        Method toSortedValueMethod = ApiDocumentTransformer.class.getDeclaredMethod("toSortedValue", String.class);
        toSortedValueMethod.setAccessible(true);

        Field collatorField = ApiDocumentTransformer.class.getDeclaredField("collator");
        collatorField.setAccessible(true);
        Collator collator = (Collator) collatorField.get(cut);
        List<String> sortedByCollator = new ArrayList<>(names);
        sortedByCollator.sort(collator);
        Map<String, BytesRef> bytesRefMap = new HashMap<>();
        for (String name : names) {
            bytesRefMap.put(name, (BytesRef) toSortedValueMethod.invoke(cut, name));
        }
        List<String> sortedByBytesRef = new ArrayList<>(names);
        sortedByBytesRef.sort(Comparator.comparing(bytesRefMap::get));
        assertThat(sortedByCollator).isEqualTo(expectedSorted);
        assertThat(sortedByBytesRef).isEqualTo(expectedSorted);
    }
}
