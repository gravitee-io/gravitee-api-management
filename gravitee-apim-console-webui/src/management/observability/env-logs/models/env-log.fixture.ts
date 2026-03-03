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

import { EnvLog } from './env-log.model';

const DEFAULT_REQUEST_HEADERS = {
  'sec-fetch-site': ['none'],
  'x-request-id': ['c8a9ef5dfa53ca6986970c93f5b46172'],
  'sec-fetch-user': ['?1'],
  'sec-ch-ua-mobile': ['?0'],
  'upgrade-insecure-requests': ['1'],
  'x-real-ip': ['127.0.0.1'],
  'sec-fetch-mode': ['navigate'],
  'if-none-match': ['W/"5fbb-mMJWB0PhsRezCsVcgf4626fnIvY"'],
  'accept-language': ['en-GB,en-US;q=0.9,en;q=0.8'],
  cookie: ['signals-sdk-user-id=993bd8ef-e3ed-4a1f-9bad-dd15121c834f; _clck=3lgd6q%5E2%5Efyu%5E0%5E2066; _uetvid=62cac93083f71'],
  'x-forwarded-host': ['apim-master-gateway.team-apim.gravitee.dev'],
  'x-forwarded-proto': ['https'],
  'x-gravitee-transaction-id': ['522826f2-6a3a-4a90-a826-f26a3a5a9062'],
  host: ['apim-master-gateway.team-apim.gravitee.dev'],
  dnt: ['1'],
  priority: ['u=0, i'],
  'x-forward-port': ['443'],
  accept: [
    'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7',
  ],
  'sec-ch-ua': ['"Not_A Brand";v="99", "Chromium";v="142"'],
  'sec-ch-ua-platform': ['"macOS"'],
  'x-forward-for': ['127.0.0.1'],
  'x-forward-scheme': ['https'],
  'accept-encoding': ['gzip, deflate, br, zstd'],
  'x-gravitee-request-id': ['522826f2-6a3a-4a90-a826-f26a3a5a9062'],
  'sec-fetch-dest': ['document'],
  'user-agent': ['Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36'],
  'x-scheme': ['https'],
};

const ENDPOINT_REQUEST_HEADERS = {
  ...DEFAULT_REQUEST_HEADERS,
  'x-gravitee-proxy-latency': ['5'],
};

const DEFAULT_RESPONSE_HEADERS = {
  'transfer-encoding': ['chunked'],
  server: ['cloudflare'],
  'access-control-allow-origin': ['*'],
  'function-execution-id': ['copqmc/ut3fz'],
  vary: ['Accept-Encoding, cookie,need-authorization, x-fh-requested-host, accept-encoding'],
  'x-served-by': ['cache-rtm-ehrd2290022-RTM'],
  'x-cache-hits': ['1'],
  'report-to': [
    '{"group":"cf-nel","max_age":604800,"endpoints":[{"url":"https://a.nel.cloudflare.com/report/v4?s=sEYFf%2BMoxn92fxVLhrtDjbGORDaZjQXpoHl9VbgeLqWKc4cziUntXzClk%2BKa%2BvKcNsfYAtr9eWUO0u4i6BPxecM%2Fv11oJ86qRthQ%3D"}]}',
  ],
  'x-cloud-trace-context': ['a8653aa1f1f62b5eb9e6ccdbe5abc3fa'],
  'x-powered-by': ['Express'],
  'content-type': ['application/json; charset=utf-8'],
  'cf-ray': ['9a52ad94ef2d6634-AMS'],
  connection: ['keep-alive'],
  'x-gravitee-transaction-id': ['522826f2-6a3a-4a90-a826-f26a3a5a9062'],
  'cf-cache-status': ['MISS'],
  date: ['Thu, 27 Nov 2025 15:32:24 GMT'],
  'strict-transport-security': ['max-age=31556926'],
  'x-orig-accept-language': ['en-US,en; q=0.9'],
  'accept-ranges': ['bytes'],
  'x-gravitee-client-identifier': ['12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0'],
  'cache-control': ['public, max-age=86400, s-maxage=86400'],
  nel: ['{"report_to":"cf-nel","success_fraction":0.0,"max_age":604800}'],
  'x-timer': ['S1764257544.486285,VS0,VE2'],
  etag: ['S1764257544.486285,VS0,VE2'],
  'x-cache': ['HIT'],
  'alt-svc': ['h3=":443"; ma=86400'],
  'x-country-code': ['NL'],
  'x-gravitee-request-id': ['522826f2-6a3a-4a90-a826-f26a3a5a9062'],
};

const DEFAULT_REQUEST_BODY = `{
  "name": "charmander",
  "type": "fire"
}`;

const DEFAULT_RESPONSE_BODY = `{
  "count": 1328,
  "next": "https://pokeapi.co/api/v2/pokemon?offset=20&limit=20",
  "previous": null,
  "results": [
    {
      "name": "bulbasaur",
      "url": "https://pokeapi.co/api/v2/pokemon/1/"
    },
    {
      "name": "ivysaur",
      "url": "https://pokeapi.co/api/v2/pokemon/2/"
    },
    {
      "name": "venusaur",
      "url": "https://pokeapi.co/api/v2/pokemon/3/"
    }
  ]
}`;

/**
 * Used for generating custom log objects for tests
 */
export const fakeEnvLog = (attributes?: Partial<EnvLog>): EnvLog => {
  const defaultLog: EnvLog = {
    id: `${Math.floor(Math.random() * 100000)}`, // NOSONAR: safe for test data
    timestamp: 'Nov 27, 2025, 3:32:24 PM',
    api: 'Pokémon API',
    apiId: 'api-pokemon',
    type: 'HTTP Proxy',
    application: 'My mobile app',
    method: 'GET',
    path: '/poke',
    status: 200,
    responseTime: '44ms',
    gateway: 'API Gateway 1',
    // Details defaults
    host: 'apim-master-gateway.team-apim.gravitee.dev',
    requestId: 'cab539a8-810b-40b4-b539-a8810be0b489',
    transactionId: 'cab539a8-810b-40b4-b539-a8810be0b489',
    remoteAddress: '127.0.0.1',
    gatewayResponseTime: '132ms',
    endpointResponseTime: '131ms',
    gatewayLatency: '1ms',
    responseContentLength: '553',
    plan: { name: 'Free Plan' },
    endpoint: 'https://pokeapi.co/api/v2/pokemon',
    clientIdentifier: '12ca17b49af2289436f303e0166030a21e525d266e209267433801a8fd4071a0',
    requestEnded: true,
    entrypointRequest: {
      method: 'GET',
      uri: '/poke',
      headers: DEFAULT_REQUEST_HEADERS,
      body: DEFAULT_REQUEST_BODY,
    },
    endpointRequest: {
      method: 'GET',
      uri: 'https://pokeapi.co/api/v2/pokemon',
      headers: ENDPOINT_REQUEST_HEADERS,
      body: DEFAULT_REQUEST_BODY,
    },
    entrypointResponse: {
      status: 200,
      headers: DEFAULT_RESPONSE_HEADERS,
      body: DEFAULT_RESPONSE_BODY,
    },
    endpointResponse: {
      status: 200,
      headers: DEFAULT_RESPONSE_HEADERS,
      body: DEFAULT_RESPONSE_BODY,
    },
  };

  return {
    ...defaultLog,
    ...attributes,
  };
};

export const fakeEnvLogs = (): EnvLog[] => {
  return [
    fakeEnvLog({
      id: 'log-1',
      timestamp: '15/06/2025 12:00:00',
      api: 'Pokémon API',
      apiId: 'api-pokemon',
      path: '/api/v1/event-types',
      method: 'PATCH',
      status: 200,
      responseTime: '44ms',
      gateway: 'API Gateway 1',
      plan: { name: 'Keyless' },
      requestEnded: true,
    }),
    fakeEnvLog({
      id: 'log-2',
      timestamp: '16/06/2025 13:15:00',
      api: 'Pokémon API',
      apiId: 'api-pokemon',
      path: '/api/v1/event-types/{id}',
      method: 'DELETE',
      status: 400,
      responseTime: '0ms',
      gateway: 'API Gateway 2',
      application: undefined,
      plan: undefined,
      endpoint: undefined,
      requestEnded: false,
      errorKey: 'BACKEND_UNAVAILABLE',
    }),
    fakeEnvLog({
      id: 'log-3',
      timestamp: '17/06/2025 14:30:00',
      api: 'Pokémon API',
      apiId: 'api-pokemon',
      path: '/api/v1/event-types',
      method: 'POST',
      status: 201,
      responseTime: '1ms',
      gateway: 'API Gateway 3',
      plan: { name: 'Keyless' },
      requestEnded: true,
      warnings: [{ key: 'QUOTA_TOO_MANY_REQUESTS' }],
    }),
    fakeEnvLog({
      id: 'log-4',
      timestamp: '18/06/2025 15:45:00',
      api: 'Pokémon API',
      apiId: 'api-pokemon',
      path: '/api/v1/event-types/{id}',
      method: 'PUT',
      status: 301,
      responseTime: '8ms',
      gateway: 'API Gateway 4',
      plan: { name: 'Keyless' },
      requestEnded: true,
    }),
    fakeEnvLog({
      id: 'log-5',
      timestamp: '19/06/2025 17:00:00',
      api: 'Pokémon API',
      apiId: 'api-pokemon',
      path: '/api/v1/event-types/{id}',
      method: 'GET',
      status: 500,
      responseTime: '500ms',
      gateway: 'API Gateway 5',
      plan: { name: 'Keyless' },
      requestEnded: true,
    }),
  ];
};
