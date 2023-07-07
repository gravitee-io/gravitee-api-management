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
package io.gravitee.gateway.security.core;

import io.gravitee.gateway.api.Request;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AuthenticationContext {
    String TOKEN_TYPE_AUTHORIZATION_BEARER = "AuthorizationBearer";
    String TOKEN_TYPE_API_KEY = "ApiKey";
    String TOKEN_TYPE_NONE = "None";

    String ATTR_INTERNAL_TOKEN_IDENTIFIED = "auth.tokenIdentifiedInRequest";
    String ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE = "auth.hasNextSecurityHandlerSupportingSameTokenType";

    Request request();

    /**
     * Stores an attribute in this context.
     *
     * @param name a String specifying the name of the attribute
     * @param value the Object to be stored
     */
    AuthenticationContext set(String name, Object value);

    /**
     * Removes an attribute from this context.
     * long as the request is being handled.
     *
     * @param name a String specifying the name of the attribute to remove
     */
    AuthenticationContext remove(String name);

    /**
     * Returns the value of the named attribute as an Object, or <code>null</code> if no attribute of the given
     * name exists.
     *
     * @param name a String specifying the name of the attribute
     * @return an Object containing the value of the attribute, or null if the attribute does not exist
     */
    Object get(String name);

    boolean contains(String apikeyContextAttribute);

    Map<String, Object> attributes();

    String getApi();

    void setSubscription(String subscription);

    void setApplication(String application);

    void setPlan(String plan);

    void setInternalAttribute(String key, Object value);

    <T> T getInternalAttribute(String key);
}
