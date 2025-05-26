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
package io.gravitee.repository.mongodb.management.upgrade.upgrader.groups;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.Index;
import io.gravitee.repository.mongodb.management.upgrade.upgrader.index.IndexUpgrader;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component("GroupsEnvironmentIdCollationIndexUpgrader")
public class GroupsEnvironmentIdCollationIndexUpgrader extends IndexUpgrader {

    @Override
    protected Index buildIndex() {
        Collation collation = Collation
            .builder()
            .locale(Locale.ENGLISH.getLanguage())
            .collationStrength(CollationStrength.SECONDARY)
            .build();
        return Index.builder().collection("groups").name("name_collation_1").key("name", ascending()).collation(collation).build();
    }
}
