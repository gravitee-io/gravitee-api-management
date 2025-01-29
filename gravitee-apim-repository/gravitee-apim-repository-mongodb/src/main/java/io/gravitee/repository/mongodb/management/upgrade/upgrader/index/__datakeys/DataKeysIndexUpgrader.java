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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.index.__datakeys;

import com.mongodb.client.model.Filters;
import io.gravitee.repository.mongodb.encryption.EncryptionEnabledCondition;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import org.bson.BsonDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("DataKeysIndexUpgrader")
@Conditional(EncryptionEnabledCondition.class)
public class DataKeysIndexUpgrader extends IndexUpgrader {

    @Value("${management.mongodb.encryption.keyVault.collectionName:__dataKeys}")
    private String keyVaultCollectionName;

    @Override
    protected Index buildIndex() {
        Index dataKeysIndex = Index
            .builder()
            .collection(keyVaultCollectionName)
            .name("k1")
            .key("keyAltNames", ascending())
            .unique(true)
            .build();
        dataKeysIndex.options().partialFilterExpression(Filters.exists("keyAltNames"));

        return dataKeysIndex;
    }
}
