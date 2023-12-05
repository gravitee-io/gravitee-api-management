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
package fixtures.definition;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import java.util.List;
import java.util.function.Supplier;

public class ApiDefinitionFixtures {

    private ApiDefinitionFixtures() {}

    private static final Supplier<Api.ApiBuilder> BASE_V4 = () ->
        Api.builder().name("an-api").apiVersion("1.0.0").type(ApiType.PROXY).analytics(Analytics.builder().enabled(false).build());

    public static Api anApiV4() {
        return aSyncApiV4();
    }

    public static Api aSyncApiV4() {
        var httpListener = HttpListener.builder().paths(List.of(new Path())).build();

        return BASE_V4.get().listeners(List.of(httpListener)).endpointGroups(List.of(EndpointGroup.builder().build())).build();
    }
}
