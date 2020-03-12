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
package io.gravitee.management.service.impl.configuration.application.registration.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.service.impl.configuration.application.registration.client.register.ClientRegistrationRequest;
import io.gravitee.management.service.impl.configuration.application.registration.client.register.ClientRegistrationResponse;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class DynamicClientRegistrationProviderClient {

    /**
     * Logger.
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ObjectMapper mapper = new ObjectMapper();

    protected CloseableHttpClient httpClient;

    protected String registrationEndpoint;

    protected ClientRegistrationResponse register(String initialAccessToken, ClientRegistrationRequest request) {
        HttpPost registerRequest = new HttpPost(registrationEndpoint);

        registerRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + initialAccessToken);
        registerRequest.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        try {
            registerRequest.setEntity(new StringEntity(
                    mapper.writeValueAsString(request),
                    ContentType.create(MediaType.APPLICATION_JSON, Charset.defaultCharset())));

            return httpClient.execute(registerRequest, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        return mapper.readValue(EntityUtils.toString(entity), ClientRegistrationResponse.class);
                    } else {
                        throw new DynamicClientRegistrationException("Client registration response does not contain any body");
                    }
                } else {
                    String responsePayload = EntityUtils.toString(response.getEntity());
                    if (responsePayload != null && !responsePayload.isEmpty()) {
                        try {
                            JsonNode node = mapper.readTree(responsePayload);
                            String error = node.path("error").asText();
                            String description = node.path("error_description").asText();
                            logger.error("Unexpected response from OIDC Registration endpoint: error[{}] description[{}]", error, description);
                            throw new DynamicClientRegistrationException("Unexpected response from OIDC Registration endpoint: error[" + error + "] description["+ description+"]");
                        } catch (JsonProcessingException ex) {
                            logger.error("Unexpected response from OIDC Registration endpoint: status[{}] message[{}]", status, responsePayload);
                            throw new DynamicClientRegistrationException("Unexpected response from OIDC Registration endpoint: status[" + status + "] message["+ responsePayload+"]");
                        }
                    } else {
                        logger.error("Unexpected response from OIDC Registration endpoint: status[{}]", status);
                        throw new DynamicClientRegistrationException("Unexpected response from OIDC Registration endpoint: status[" + status + "]");
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("Unexpected error while registering client: " + ex.getMessage(), ex);
            throw new DynamicClientRegistrationException("Unexpected error while registering client: " + ex.getMessage(), ex);
        }
    }

    public ClientRegistrationResponse update(String registrationAccessToken,
                                             String registrationClientUri,
                                             ClientRegistrationRequest request,
                                             String clientId) {
        HttpPut updateRequest = new HttpPut(registrationClientUri);

        updateRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + registrationAccessToken);
        updateRequest.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        try {
            JsonNode reqNode = mapper.readTree(mapper.writeValueAsString(request));

            // Set the client_id according to https://tools.ietf.org/html/rfc7592#page-7
            ((ObjectNode) reqNode).put("client_id", clientId);

            if (request.getScope() != null && !request.getScope().isEmpty()) {
                ((ObjectNode) reqNode).put("scope", String.join(ClientRegistrationRequest.SCOPE_DELIMITER, request.getScope()));
            } else {
                ((ObjectNode) reqNode).remove("scope");
            }

            updateRequest.setEntity(new StringEntity(
                    mapper.writeValueAsString(reqNode),
                    ContentType.create(MediaType.APPLICATION_JSON, Charset.defaultCharset())));

            return httpClient.execute(updateRequest, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        return mapper.readValue(EntityUtils.toString(entity), ClientRegistrationResponse.class);
                    } else {
                        throw new DynamicClientRegistrationException("Client registration response does not contain any body");
                    }
                } else {
                    String responsePayload = EntityUtils.toString(response.getEntity());
                    if (responsePayload != null && !responsePayload.isEmpty()) {
                        try {
                            JsonNode node = mapper.readTree(responsePayload);
                            String error = node.path("error").asText();
                            String description = node.path("error_description").asText();
                            logger.error("Unexpected response from OIDC Registration endpoint: error[{}] description[{}]", error, description);
                            throw new DynamicClientRegistrationException("Unexpected response from OIDC Registration endpoint: error[" + error + "] description["+ description+"]");
                        } catch (JsonProcessingException ex) {
                            logger.error("Unexpected response from OIDC Registration endpoint: status[{}] message[{}]", status, responsePayload);
                            throw new DynamicClientRegistrationException("Unexpected response from OIDC Registration endpoint: status[" + status + "] message["+ responsePayload+"]");
                        }
                    } else {
                        logger.error("Unexpected response from OIDC Registration endpoint: status[{}]", status);
                        throw new DynamicClientRegistrationException("Unexpected response from OIDC Registration endpoint: status[" + status + "]");
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("Unexpected error while registering client: " + ex.getMessage(), ex);
            throw new DynamicClientRegistrationException("Unexpected error while registering client: " + ex.getMessage(), ex);
        }
    }

    public ClientRegistrationResponse renewClientSecret(String method, String endpoint, String registrationAccessToken) {
        HttpRequestBase renewRequest = null;

        if (method.equalsIgnoreCase(HttpMethod.POST.name())) {
            renewRequest = new HttpPost(endpoint);
        } else if (method.equalsIgnoreCase(HttpMethod.PUT.name())) {
            renewRequest = new HttpPut(endpoint);
        } else if (method.equalsIgnoreCase(HttpMethod.PATCH.name())) {
            renewRequest = new HttpPatch(endpoint);
        }

        renewRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + registrationAccessToken);
        renewRequest.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        try {
            return httpClient.execute(renewRequest, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        return mapper.readValue(EntityUtils.toString(entity), ClientRegistrationResponse.class);
                    } else {
                        throw new DynamicClientRegistrationException("Renew client secret response does not contain any body");
                    }
                } else {
                    String responsePayload = EntityUtils.toString(response.getEntity());
                    if (responsePayload != null && !responsePayload.isEmpty()) {
                        try {
                            JsonNode node = mapper.readTree(responsePayload);
                            String error = node.path("error").asText();
                            String description = node.path("error_description").asText();
                            logger.error("Unexpected response from renew client secret endpoint: error[{}] description[{}]", error, description);
                            throw new DynamicClientRegistrationException("Unexpected response from renew client secret endpoint: error[" + error + "] description["+ description+"]");
                        } catch (JsonProcessingException ex) {
                            logger.error("Unexpected response from renew client secret endpoint: status[{}] message[{}]", status, responsePayload);
                            throw new DynamicClientRegistrationException("Unexpected response from new client secret endpoint: status[" + status + "] message["+ responsePayload+"]");
                        }
                    } else {
                        logger.error("Unexpected response from renew client secret endpoint: status[{}]", status);
                        throw new DynamicClientRegistrationException("Unexpected response from renew client secret endpoint: status[" + status + "]");
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("Unexpected error while renewing client secret: " + ex.getMessage(), ex);
            throw new DynamicClientRegistrationException("Unexpected error while renewing client secret: " + ex.getMessage(), ex);
        }
    }

    public ClientRegistrationResponse register(ClientRegistrationRequest request) {
        // 1_ Generate an access_token
        String accessToken = getInitialAccessToken();

        // 2_ Register the client
        return register(accessToken, request);
    }

    public abstract String getInitialAccessToken();
}
