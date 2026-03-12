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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo model for Basic Auth Credentials
 *
 * @author GraviteeSource Team
 */
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "#{@environment.getProperty('management.mongodb.prefix')}basic_auth_credentials")
public class BasicAuthCredentialsMongo {

    @Setter
    @Getter
    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Setter
    @Getter
    private String username;

    @Setter
    @Getter
    private String password;

    @Setter
    @Getter
    private List<String> subscriptions = new ArrayList<>();

    @Setter
    @Getter
    private String application;

    @Setter
    @Getter
    private String environmentId;

    @Setter
    @Getter
    private Date expireAt;

    @Setter
    @Getter
    private Date createdAt;

    @Setter
    @Getter
    private Date updatedAt;

    @Setter
    @Getter
    private boolean revoked;

    @Setter
    @Getter
    private Date revokedAt;
}
