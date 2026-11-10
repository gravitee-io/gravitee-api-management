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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.credential.model.CredentialConsumer;
import io.gravitee.apim.core.credential.model.NewCredentialCommand;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.credential.model.UpdateCredentialCommand;
import io.gravitee.rest.api.management.v2.rest.model.CreateCredential;
import io.gravitee.rest.api.management.v2.rest.model.Credential;
import io.gravitee.rest.api.management.v2.rest.model.CredentialSecretReference;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCredential;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface CredentialMapper {
    CredentialMapper INSTANCE = Mappers.getMapper(CredentialMapper.class);

    @Mapping(target = "managed", expression = "java(credential.isManaged())")
    Credential map(io.gravitee.apim.core.credential.model.Credential credential);

    List<Credential> map(List<io.gravitee.apim.core.credential.model.Credential> credentials);

    List<io.gravitee.rest.api.management.v2.rest.model.CredentialConsumer> mapConsumers(List<CredentialConsumer> consumers);

    default CredentialSecretReference map(SecretReference reference) {
        return new CredentialSecretReference().reference(reference.reference()).managed(reference.isManaged());
    }

    // The generated request models start their maps empty, so an empty map means the client left the field out.
    @Mapping(target = "references", expression = "java(given(createCredential.getReferences()))")
    @Mapping(target = "values", expression = "java(given(createCredential.getValues()))")
    NewCredentialCommand map(CreateCredential createCredential);

    @Mapping(target = "references", expression = "java(given(updateCredential.getReferences()))")
    UpdateCredentialCommand map(UpdateCredential updateCredential);

    default <V> Map<String, V> given(Map<String, V> map) {
        return map == null || map.isEmpty() ? null : new LinkedHashMap<>(map);
    }
}
