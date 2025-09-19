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
package io.gravitee.apim.infra.federation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.gravitee.definition.model.federation.FederatedAgent;
import io.vertx.rxjava3.core.Vertx;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class A2aAgentFetcherImplTest {

    WireMockServer wireMockServer;
    A2aAgentFetcherImpl a2aAgentFetcher;
    Vertx vertx;
    String wellKnownUrl;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        wellKnownUrl = "http://localhost:" + wireMockServer.port() + "/.well-known/agent.json";

        vertx = Vertx.vertx();
        a2aAgentFetcher = new A2aAgentFetcherImpl(vertx);
        a2aAgentFetcher.init();
    }

    @AfterEach
    void tearDown() {
        if (vertx != null) {
            vertx.close();
        }
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void should_successfully_fetch_agent_card() {
        // Given
        String agentCardJson = """
            {
                "name": "Test Agent",
                "description": "A test A2A agent",
                "version": "1.0.0",
                "url": "https://example.com/.well-known/agent.json",
                "documentationUrl": "https://example.com/docs",
                "provider": {
                    "organization": "test-org",
                    "url": "https://provider.example.com"
                },
                "capabilities": {
                    "streaming": true,
                    "pushNotifications": false,
                    "stateTransitionHistory": true
                },
                "skills": [
                    {
                        "id": "skill-1",
                        "name": "Test Skill",
                        "description": "Skill Description",
                        "tags": ["test", "example"],
                        "examples": ["example command"],
                        "inputModes": ["text"],
                        "outputModes": ["text", "json"]
                    }
                ],
                "defaultInputModes": ["text"],
                "defaultOutputModes": ["text"],
                "securitySchemes": {
                    "apiKey": {
                        "type": "apiKey",
                        "name": "X-API-Key",
                        "in": "header"
                    }
                },
                "security": [
                    {
                        "apiKey": []
                    }
                ]
            }
            """;

        wireMockServer.stubFor(
            get(urlEqualTo("/.well-known/agent.json")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(agentCardJson)
            )
        );

        // When
        var result = a2aAgentFetcher.fetchAgentCard(wellKnownUrl).test().awaitDone(5, TimeUnit.SECONDS).values();

        // Then
        assertThat(result).hasSize(1);

        FederatedAgent agent = result.getFirst();
        assertThat(agent.getName()).isEqualTo("Test Agent");
        assertThat(agent.getDescription()).isEqualTo("A test A2A agent");
        assertThat(agent.getVersion()).isEqualTo("1.0.0");
        assertThat(agent.getUrl()).isEqualTo("https://example.com/.well-known/agent.json");
        assertThat(agent.getDocumentationUrl()).isEqualTo("https://example.com/docs");

        assertThat(agent.getProvider()).isEqualTo(new FederatedAgent.Provider("test-org", "https://provider.example.com"));

        assertThat(agent.getCapabilities())
            .containsEntry("streaming", true)
            .containsEntry("pushNotifications", false)
            .containsEntry("stateTransitionHistory", true);

        assertThat(agent.getSkills()).containsOnly(
            new FederatedAgent.Skill(
                "skill-1",
                "Test Skill",
                "Skill Description",
                List.of("test", "example"),
                List.of("example command"),
                List.of("text"),
                List.of("text", "json")
            )
        );

        assertThat(agent.getDefaultInputModes()).containsExactly("text");
        assertThat(agent.getDefaultOutputModes()).containsExactly("text");

        assertThat(agent.getSecuritySchemes()).isNotNull();
        assertThat(agent.getSecurity()).isNotNull();

        // Verify the request was made
        wireMockServer.verify(getRequestedFor(urlEqualTo("/.well-known/agent.json")));
    }

    @Test
    void should_successfully_fetch_minimal_agent_card() {
        // Given
        String minimalAgentCardJson = """
            {
                "name": "Minimal Agent",
                "description": "A minimal A2A agent",
                "version": "1.0.0",
                "url": "https://minimal.example.com/.well-known/agent.json",
                "capabilities": {},
                "skills": [],
                "defaultInputModes": [],
                "defaultOutputModes": []
            }
            """;

        wireMockServer.stubFor(
            get(urlEqualTo("/.well-known/agent.json")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(minimalAgentCardJson)
            )
        );

        // When
        var result = a2aAgentFetcher.fetchAgentCard(wellKnownUrl).test().awaitDone(5, TimeUnit.SECONDS).values();

        // Then
        assertThat(result).hasSize(1);

        FederatedAgent agent = result.getFirst();
        assertThat(agent.getName()).isEqualTo("Minimal Agent");
        assertThat(agent.getDescription()).isEqualTo("A minimal A2A agent");
        assertThat(agent.getVersion()).isEqualTo("1.0.0");
        assertThat(agent.getUrl()).isEqualTo("https://minimal.example.com/.well-known/agent.json");
        assertThat(agent.getProvider()).isNull();
        assertThat(agent.getDocumentationUrl()).isNull();
        assertThat(agent.getCapabilities()).isEmpty();
        assertThat(agent.getSkills()).isEmpty();
        assertThat(agent.getDefaultInputModes()).isEmpty();
        assertThat(agent.getDefaultOutputModes()).isEmpty();
    }

    @Test
    void should_handle_https_url() {
        // Given
        String agentCardJson = """
            {
                "name": "HTTPS Agent",
                "description": "An agent over HTTPS",
                "version": "1.0.0",
                "url": "https://secure.example.com/.well-known/agent.json",
                "capabilities": {},
                "skills": [],
                "defaultInputModes": [],
                "defaultOutputModes": []
            }
            """;

        wireMockServer.stubFor(
            get(urlEqualTo("/.well-known/agent.json")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(agentCardJson)
            )
        );

        // When
        var result = a2aAgentFetcher.fetchAgentCard(wellKnownUrl).test().awaitDone(5, TimeUnit.SECONDS).values();

        // Then
        assertThat(result).hasSize(1).map(FederatedAgent::getName).first().isEqualTo("HTTPS Agent");
    }

    @Test
    void should_fail_when_agent_card_not_found() {
        // Given
        wireMockServer.stubFor(get(urlEqualTo("/.well-known/agent.json")).willReturn(aResponse().withStatus(404)));

        // When
        var testObserver = a2aAgentFetcher.fetchAgentCard(wellKnownUrl).test().awaitDone(5, TimeUnit.SECONDS);

        // Then
        testObserver.assertError(Throwable.class);
    }

    @Test
    void should_fail_when_server_returns_internal_error() {
        // Given
        wireMockServer.stubFor(
            get(urlEqualTo("/.well-known/agent.json")).willReturn(aResponse().withStatus(500).withBody("Internal Server Error"))
        );

        // When
        var testObserver = a2aAgentFetcher.fetchAgentCard(wellKnownUrl).test().awaitDone(5, TimeUnit.SECONDS);

        // Then
        testObserver.assertError(Throwable.class);
    }

    @Test
    void should_fail_when_response_is_not_valid_json() {
        // Given
        wireMockServer.stubFor(
            get(urlEqualTo("/.well-known/agent.json")).willReturn(
                aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("invalid json content")
            )
        );

        // When
        var testObserver = a2aAgentFetcher.fetchAgentCard(wellKnownUrl).test().awaitDone(5, TimeUnit.SECONDS);

        // Then
        testObserver.assertError(Throwable.class);
    }

    @Test
    void should_fail_with_invalid_url() {
        // Given
        String invalidUrl = "not-a-valid-url";

        // When
        var testObserver = a2aAgentFetcher.fetchAgentCard(invalidUrl).test().awaitDone(5, TimeUnit.SECONDS);

        // Then
        testObserver.assertError(Throwable.class);
    }
}
