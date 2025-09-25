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
package io.gravitee.apim.core.newtai.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.newtai.model.ELGenQuery;
import io.gravitee.apim.core.newtai.model.ELGenReply;
import io.gravitee.apim.core.newtai.service_provider.NewtAIProvider;
import io.reactivex.rxjava3.core.Single;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GenerateELUseCase {

    private final NewtAIProvider newtAIProvider;

    public Single<Output> execute(Input input) {
        var inputWithContext = input.withProperties(Map.of("elContextVariables", CONTEXT));
        return newtAIProvider.generateEL(inputWithContext).map(Output::new);
    }

    public record Input(String message, Map<String, String> properties, AuditInfo auditInfo) implements ELGenQuery {
        public Input(String message, AuditInfo auditInfo) {
            this(message, new HashMap<>(), auditInfo);
        }

        public Input withProperties(Map<String, String> properties) {
            final var props = new HashMap<>(this.properties);
            props.putAll(properties);
            return new Input(message, props, auditInfo);
        }

        public Input withApiId(String apiId) {
            final var props = new HashMap<>(this.properties);
            props.put("apiId", apiId);
            return new Input(message, props, auditInfo);
        }

        public Map<String, String> properties() {
            return Collections.unmodifiableMap(properties);
        }
    }

    public record Output(String message, FeedbackId feedbackId) {
        public Output(ELGenReply reply) {
            this(reply.message(), new FeedbackId(reply.feedbackId()));
        }

        public record FeedbackId(String chatId, String userMessageId, String agentMessageId) {
            public FeedbackId(ELGenReply.FeedbackId feedbackId) {
                this(feedbackId.chatId(), feedbackId.userMessageId(), feedbackId.agentMessageId());
            }
        }
    }

    private static final String CONTEXT = """
        {
          "api": {
            "id": "c9f75e22-1a9a-4f46-b75e-221a9a7f465a",
            "name": "Echo",
            "properties": {
              "propB": "Beta",
              "propA": "Alpha"
            },
            "version": "1"
          },
          "context": {
            "attributes": {
              "application": "6989925b-d9fe-41f2-8992-5bd9fed1f2a7",
              "api.deployed-at": 1750402605200,
              "context-path": "/echo/",
              "user-id": "7f1008dd-300a-4bc7-9008-dd300a6bc7db",
              "plan": "bd3a24ad-1e6f-4f3c-ba24-ad1e6f6f3c4e",
              "clientIdentifier": "7f1008dd-300a-4bc7-9008-dd300a6bc7db",
              "organization": "DEFAULT",
              "environment": "DEFAULT",
              "CustomAttribute": "My custom Atribute Value",
              "api.name": "Echo",
              "api": "c9f75e22-1a9a-4f46-b75e-221a9a7f465a",
              "api-key": "bdf382f2-e323-4647-b382-f2e323464723"
            }
          },
          "dictionaries": {
            "color-dictionary": {
              "Red": "142",
              "Blue": "no blue",
              "Black": "{\\"id\\": \\"123456789\\"}"
            }
          },
          "endpoints": {
            "Default HTTP proxy": "Default HTTP proxy:",
            "Default HTTP proxy group": "Default HTTP proxy group:"
          },
          "node": {
            "id": "48f3e688-e83f-400d-b3e6-88e83f000de8",
            "shardingTags": [],
            "version": "4.8.0-alpha.3-SNAPSHOT"
          },
          "request": {
            "contextPath": "/echo/",
            "headers": {
              "Accept": [
                "*/*"
              ],
              "X-Gravitee-Request-Id": [
                "e2ac4ba5-e44e-40e1-ac4b-a5e44ec0e1df"
              ],
              "X-Gravitee-Transaction-Id": [
                "e2ac4ba5-e44e-40e1-ac4b-a5e44ec0e1df"
              ],
              "Postman-Token": [
                "a8c4139c-5df9-4a18-b010-c5a7c7cb508b"
              ],
              "User-Agent": [
                "PostmanRuntime/7.44.0"
              ],
              "Cache-Control": [
                "no-cache"
              ],
              "Test_Endpoints_header": [
                "aaaa"
              ]
            },
            "id": "e2ac4ba5-e44e-40e1-ac4b-a5e44ec0e1df",
            "localAddress": "0:0:0:0:0:0:0:1",
            "method": "GET",
            "params": {
              "queryParams": [
                "foo"
              ],
              "t": [
                "1",
                "2"
              ]
            },
            "path": "/echo",
            "pathInfo": "",
            "pathInfos": [
              ""
            ],
            "pathParams": {},
            "paths": [
              "",
              "echo"
            ],
            "remoteAddress": "0:0:0:0:0:0:0:1",
            "scheme": "http",
            "ssl": {
              "client": "[inaccessible]",
              "clientHost": "[inaccessible]",
              "clientPort": "[inaccessible]",
              "server": "[inaccessible]"
            },
            "timestamp": 1750429186674,
            "transactionId": "e2ac4ba5-e44e-40e1-ac4b-a5e44ec0e1df",
            "uri": "/echo?queryParams=foo&t=1&t=2",
            "version": "HTTP_1_1"
          },
          "response": {
            "headers": {
              "X-Gravitee-Transaction-Id": [
                "e2ac4ba5-e44e-40e1-ac4b-a5e44ec0e1df"
              ],
              "X-Gravitee-Client-Identifier": [
                "7f1008dd-300a-4bc7-9008-dd300a6bc7db"
              ],
              "Sozu-Id": [
                "01JY6VBEP9P1CPKJNVBFGRXXFE"
              ],
              "Content-Type": [
                "application/json"
              ],
              "Content-Length": [
                "431"
              ],
              "X-Gravitee-Request-Id": [
                "38d41bea-5308-4d18-941b-ea53088d18ae"
              ]
            },
            "status": 200
          },
          "subscription": {
            "applicationName": "Default application",
            "id": "7f1008dd-300a-4bc7-9008-dd300a6bc7db",
            "metadata": {
              "subscriptionMetadata": "Foo"
            },
            "type": "STANDARD"
          }
        }
        """;
}
