/*
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
import http from 'k6/http';
import { k6Options } from '@env/environment';
import ws from 'k6/ws';

export class LoadTestEndpointClient {
  static startWebsocketSubscription(contextPath: string, subscription: string) {
    console.log(`Init WebSocket Subscription ${subscription}`);
    const backendUrl = k6Options.apim.websocket.websocketServiceBaseUrl.endsWith('/')
      ? k6Options.apim.websocket.websocketServiceBaseUrl + subscription
      : k6Options.apim.websocket.websocketServiceBaseUrl + '/' + subscription;
    const wsUrl = k6Options.apim.gatewayBaseUrl + contextPath; // contextPath already contains the leading '/'
    const response = http.request(
      'POST',
      backendUrl,
      JSON.stringify({
        action: 'start',
        websocketUrl: wsUrl,
      }),
      {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { name: OUT_OF_SCENARIO },
      },
    );

    if (response.status != 200) {
      throw new Error(`[POST] [${backendUrl}] Start action returned HTTP ${response.status}`);
    }
  }

  static stopWebsocketSubscription(subscription: string) {
    console.log(`Stop WebSocket Subscription ${subscription}`);
    const wsUrl = k6Options.apim.websocket.websocketServiceBaseUrl.endsWith('/')
      ? k6Options.apim.websocket.websocketServiceBaseUrl + subscription
      : k6Options.apim.websocket.websocketServiceBaseUrl + '/' + subscription;
    const response = http.request(
      'POST',
      wsUrl,
      JSON.stringify({
        action: 'stop',
      }),
      {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { name: OUT_OF_SCENARIO },
      },
    );

    if (response.status != 200) {
      throw new Error(`[POST] [${wsUrl}] Stop action returned HTTP ${response.status}`);
    }
  }
}

export const OUT_OF_SCENARIO = 'out-of-scenario';
