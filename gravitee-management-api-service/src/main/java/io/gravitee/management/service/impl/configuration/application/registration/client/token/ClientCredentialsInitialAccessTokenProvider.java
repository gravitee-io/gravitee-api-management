/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl.configuration.application.registration.client.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.service.impl.configuration.application.registration.client.DynamicClientRegistrationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClientCredentialsInitialAccessTokenProvider implements InitialAccessTokenProvider {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ClientCredentialsInitialAccessTokenProvider.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private final String clientId;
    private final String clientSecret;
    private final List<String> scopes;

    public ClientCredentialsInitialAccessTokenProvider(final String clientId, final String clientSecret) {
        this(clientId, clientSecret, Collections.emptyList());
    }

    public ClientCredentialsInitialAccessTokenProvider(final String clientId, final String clientSecret, final List<String> scopes) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scopes = scopes;
    }

    @Override
    public String get(Map<String, String> attributes) {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        String tokenEndpoint = attributes.get("token_endpoint");

        HttpPost tokenRequest = new HttpPost(tokenEndpoint);

        List<NameValuePair> tokenRequestParams = new ArrayList<>();
        tokenRequestParams.add(new BasicNameValuePair("grant_type", "client_credentials"));

        if (scopes != null && !scopes.isEmpty()) {
            tokenRequestParams.add(new BasicNameValuePair("scope", String.join(" ", scopes)));
        }

        // In the future, we must being able to handle client authentication according to the
        // `token_endpoint_auth_methods_supported` property from discovery.
        tokenRequest.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ':' + clientSecret).getBytes()));
        tokenRequest.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        try {
            tokenRequest.setEntity(new UrlEncodedFormEntity(tokenRequestParams));

            return httpClient.execute(tokenRequest, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        TokenResponse token = mapper.readValue(EntityUtils.toString(entity),
                                TokenResponse.class);

                        return token.getAccessToken();
                    } else {
                        throw new DynamicClientRegistrationException("Token response does not contain any body");
                    }
                } else {
                    String responsePayload = EntityUtils.toString(response.getEntity());
                    if (responsePayload != null && !responsePayload.isEmpty()) {
                        try {
                            JsonNode node = mapper.readTree(responsePayload);
                            String error = node.path("error").asText();
                            String description = node.path("error_description").asText();
                            logger.error("Unexpected response from OIDC Token endpoint: error[{}] description[{}]", error, description);
                            throw new DynamicClientRegistrationException("Unexpected response from OIDC Token endpoint: error[" + error + "] description[" + description + "]");
                        } catch (JsonProcessingException ex) {
                            logger.error("Unexpected response from OIDC Token endpoint: status[{}] message[{}]", status, responsePayload);
                            throw new DynamicClientRegistrationException("Unexpected response from OIDC Token endpoint: status[" + status + "] message[" + responsePayload + "]");
                        }
                    } else {
                        logger.error("Unexpected response from OIDC Token endpoint: status[{}]", status);
                        throw new DynamicClientRegistrationException("Unexpected response from OIDC Token endpoint: status[" + status + "]");
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("Unexpected error while generating an access_token: " + ex.getMessage(), ex);
            throw new DynamicClientRegistrationException("Unexpected error while generating an access_token: " + ex.getMessage(), ex);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
