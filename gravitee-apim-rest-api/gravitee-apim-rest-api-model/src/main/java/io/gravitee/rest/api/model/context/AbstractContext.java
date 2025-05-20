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
package io.gravitee.rest.api.model.context;

import lombok.EqualsAndHashCode;
import lombok.Getter;

<<<<<<< HEAD:gravitee-apim-rest-api/gravitee-apim-rest-api-model/src/main/java/io/gravitee/rest/api/model/context/AbstractContext.java
@EqualsAndHashCode
=======
/**
 * @deprecated Use {@link io.gravitee.definition.model.sharedpolicygroup.SharedPolicyGroupPolicyConfiguration} instead.
 */
@Deprecated
@Setter
>>>>>>> 5ce54148ca (fix: After changing the name of shared policy group the updated name is not reflected in the API's in which its being used):gravitee-apim-gateway/gravitee-apim-gateway-handlers/gravitee-apim-gateway-handlers-shared-policy-group/src/main/java/io/gravitee/gateway/handlers/sharedpolicygroup/policy/SharedPolicyGroupPolicyConfiguration.java
@Getter
public abstract class AbstractContext implements OriginContext {

    private final Origin origin;

    public AbstractContext(Origin origin) {
        this.origin = origin;
    }
}
