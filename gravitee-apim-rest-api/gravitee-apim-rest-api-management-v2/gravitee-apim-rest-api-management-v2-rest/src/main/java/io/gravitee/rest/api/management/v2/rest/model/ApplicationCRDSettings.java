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
package io.gravitee.rest.api.management.v2.rest.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationCRDSettings {

    private SimpleApplicationSettings app;
    private OAuthClientSettings oauth;
    private TLSSettings tls;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TLSSettings {

        private String clientCertificate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimpleApplicationSettings {

        private String type;
        private String clientId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OAuthClientSettings {

        private List<String> grantTypes;
        private List<String> redirectUris;
        private String applicationType;
    }
}
