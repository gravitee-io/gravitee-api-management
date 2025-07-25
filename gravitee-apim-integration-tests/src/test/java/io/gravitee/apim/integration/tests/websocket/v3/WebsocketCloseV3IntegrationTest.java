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
package io.gravitee.apim.integration.tests.websocket.v3;

import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.integration.tests.websocket.v4Emulation.WebsocketCloseV4EmulationIntegrationTest;
import io.gravitee.definition.model.ExecutionMode;

@GatewayTest(v2ExecutionMode = ExecutionMode.V3)
public class WebsocketCloseV3IntegrationTest extends WebsocketCloseV4EmulationIntegrationTest {}
