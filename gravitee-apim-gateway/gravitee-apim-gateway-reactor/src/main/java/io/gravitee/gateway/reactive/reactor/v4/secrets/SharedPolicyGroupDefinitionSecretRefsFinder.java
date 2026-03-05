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
package io.gravitee.gateway.reactive.reactor.v4.secrets;

import io.gravitee.definition.model.v4.sharedpolicygroup.SharedPolicyGroup;
import io.gravitee.secrets.api.discovery.Definition;
import io.gravitee.secrets.api.discovery.DefinitionDescriptor;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.discovery.DefinitionSecretRefsListener;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
public class SharedPolicyGroupDefinitionSecretRefsFinder extends AbstractV4APISecretRefFinder<SharedPolicyGroup> {

    public static final String SHARED_POLICY_GROUP_DEFINITION_KIND = "shared-policy-group";

    @Override
    public boolean canHandle(Object definition) {
        return canHandle(definition, SharedPolicyGroup.class);
    }

    @Override
    public DefinitionDescriptor toDefinitionDescriptor(SharedPolicyGroup definition, DefinitionMetadata metadata) {
        return new DefinitionDescriptor(
            new Definition(SHARED_POLICY_GROUP_DEFINITION_KIND, definition.getId()),
            Optional.ofNullable(metadata.revision())
        );
    }

    @Override
    public void findSecretRefs(SharedPolicyGroup definition, DefinitionSecretRefsListener listener) {
        safeList(definition.getPolicies()).forEach(step -> processStep(listener, step));
    }
}
