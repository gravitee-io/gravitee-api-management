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
package io.gravitee.rest.api.management.v2.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AbstractResourceLocationHeaderTest {

    private static final String COLLECTION = "http://localhost/management/v2/environments/env/apis/api/portal-navigation-items";

    @ParameterizedTest
    @ValueSource(strings = { "/_overview-page", "/_overview-page/" })
    void names_an_item_created_by_an_action_under_its_collection(String action) {
        var resource = requestedAt(COLLECTION + action);

        assertThat(resource.getCollectionLocationHeader("item-id")).isEqualTo(URI.create(COLLECTION + "/item-id"));
    }

    @Test
    void leaves_out_the_query_of_the_action() {
        var resource = requestedAt(COLLECTION + "/_overview-page?dryRun=true");

        assertThat(resource.getCollectionLocationHeader("item-id")).isEqualTo(URI.create(COLLECTION + "/item-id"));
    }

    private static AbstractResource requestedAt(String requestUri) {
        var resource = new AbstractResource() {};
        resource.uriInfo = mock(UriInfo.class);
        when(resource.uriInfo.getRequestUri()).thenReturn(URI.create(requestUri));
        return resource;
    }
}
