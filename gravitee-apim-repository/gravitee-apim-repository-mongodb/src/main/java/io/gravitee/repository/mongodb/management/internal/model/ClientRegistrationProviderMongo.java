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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.List;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@EqualsAndHashCode(of = { "id" }, callSuper = false)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}client_registration_providers")
public class ClientRegistrationProviderMongo extends Auditable {

    /**
     * Client registration provider ID
     */
    @Id
    private String id;

    /**
     * Client registration OIDC discovery endpoint
     */
    private String environmentId;

    /**
     * Client registration provider name
     */
    private String name;

    /**
     * Client registration provider description
     */
    private String description;

    /**
     * Client registration OIDC discovery endpoint
     */
    private String discoveryEndpoint;

    private String initialAccessTokenType;

    /**
     * Client registration OIDC Client_ID
     */
    private String clientId;

    /**
     * Client registration OIDC Client_secret
     */
    private String clientSecret;

    /**
     * Client registration OIDC scopes
     */
    private List<String> scopes;

    private String initialAccessToken;

    private boolean renewClientSecretSupport;

    private String renewClientSecretEndpoint;

    private String renewClientSecretMethod;

    private String softwareId;
}
