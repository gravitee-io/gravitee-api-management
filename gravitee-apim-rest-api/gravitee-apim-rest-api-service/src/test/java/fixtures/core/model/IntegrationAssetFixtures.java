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
package fixtures.core.model;

import io.gravitee.apim.core.integration.model.Asset;
import java.util.function.Supplier;

public class IntegrationAssetFixtures {

    private IntegrationAssetFixtures() {}

    private static final Supplier<Asset.AssetBuilder> BASE = () ->
        Asset
            .builder()
            .integrationId("integration-id")
            .id("asset-id")
            .name("An alien API")
            .description("An alien API description")
            .version("1.0.0");

    public static Asset anAssetForIntegration(String integrationId) {
        return BASE.get().integrationId(integrationId).build();
    }
}
