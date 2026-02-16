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
import { WebhookLog, WebhookLogsResponse } from './models/webhook-logs.models';

import { HttpMethod } from '../../../../entities/management-api-v2';

export const fakeWebhookLogsResponse = (modifier?: Partial<WebhookLogsResponse>): WebhookLogsResponse => {
  const base: WebhookLogsResponse = {
    pagination: { page: 1, perPage: 10, pageCount: 1, pageItemsCount: 3, totalCount: 3 },
    data: [
      {
        apiId: 'api-mock',
        requestId: 'req-1',
        timestamp: new Date(1763393209759).toISOString(),
        method: 'POST' as HttpMethod,
        status: 500,
        application: {
          id: '8ff0178d-9326-465c-b017-8d9326a65cb7',
          name: 'Acme Monitoring',
          type: 'SIMPLE',
          apiKeyMode: 'UNSPECIFIED',
        },
        plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' },
        requestEnded: true,
        gatewayResponseTime: 3012,
        uri: 'http://localhost:3000/status/500',
        endpoint: 'http://localhost:3000/status/500',
        callbackUrl: 'http://localhost:3000/status/500',
        duration: '3.0 s',
        additionalMetrics: {
          'string_webhook_req-method': 'POST',
          string_webhook_url: 'http://localhost:3000/status/500',
          'keyword_webhook_app-id': '8ff0178d-9326-465c-b017-8d9326a65cb7',
          'keyword_webhook_sub-id': '6d8b7266-e46d-407f-8b72-66e46d807f34',
          'int_webhook_retry-count': 2,
          'json_webhook_retry-timeline':
            '[{"attempt":1,"timestamp":1763393210788,"status":500,"duration":1028,"reason":"Webhook entrypoint response status is 500"},{"attempt":2,"timestamp":1763393211802,"status":500,"duration":1014,"reason":"Webhook entrypoint response status is 500"},{"attempt":3,"timestamp":1763393211810,"status":500,"duration":8,"reason":"Webhook entrypoint response status is 500"}]',
          'string_webhook_last-error': 'Webhook entrypoint response status is 500',
          'long_webhook_req-timestamp': 1763393209759,
          'json_webhook_req-headers': JSON.stringify({
            'content-length': '12',
            'X-Gravitee-Client-Identifier': '6d8b7266-e46d-407f-8b72-66e46d807f34',
            'X-Gravitee-Transaction-Id': '60381b05-411b-4dd5-b81b-05411bddd544',
            'X-Gravitee-Request-Id': '60381b05-411b-4dd5-b81b-05411bddd544',
          }),
          'string_webhook_req-body': 'mock message',
          'long_webhook_resp-time': 3012,
          'int_webhook_resp-status': 500,
          'int_webhook_resp-body-size': 0,
          'json_webhook_resp-headers': JSON.stringify({
            Server: 'gunicorn/19.9.0',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Credentials': 'true',
            Connection: 'keep-alive',
            'Content-Length': '0',
            Date: 'Mon, 17 Nov 2025 15:26:52 GMT',
            'Content-Type': 'text/html; charset=utf-8',
          }),
          'string_webhook_resp-body': '',
          bool_webhook_dlq: false,
        },
      },
      {
        apiId: '2a16110a-9d62-426b-9611-0a9d62926bf4',
        requestId: 'req-2',
        timestamp: new Date(1763390562274).toISOString(),
        method: 'POST' as HttpMethod,
        status: 200,
        application: {
          id: '8ff0178d-9326-465c-b017-8d9326a65cb7',
          name: 'Acme Webhook Client',
          type: 'SIMPLE',
          apiKeyMode: 'UNSPECIFIED',
        },
        plan: { id: 'plan-1', name: 'Webhook Plan', mode: 'PUSH' },
        requestEnded: true,
        gatewayResponseTime: 3022,
        uri: 'http://localhost:3000/callback',
        endpoint: 'http://localhost:3000/callback',
        callbackUrl: 'http://localhost:3000/callback',
        duration: '3.0 s',
        additionalMetrics: {
          'string_webhook_req-method': 'POST',
          string_webhook_url: 'http://localhost:3000/callback',
          'keyword_webhook_app-id': '8ff0178d-9326-465c-b017-8d9326a65cb7',
          'keyword_webhook_sub-id': 'bf590dde-00d6-43f3-990d-de00d6b3f3f7',
          'int_webhook_retry-count': 0,
          'json_webhook_retry-timeline': '[]',
          'long_webhook_req-timestamp': 1763390562274,
          'json_webhook_req-headers': JSON.stringify({
            'content-length': '12',
            'X-Gravitee-Client-Identifier': 'bf590dde-00d6-43f3-990d-de00d6b3f3f7',
            'X-Gravitee-Transaction-Id': '17e20a10-9eeb-44e9-a20a-109eeb94e9af',
            'X-Gravitee-Request-Id': '17e20a10-9eeb-44e9-a20a-109eeb94e9af',
          }),
          'string_webhook_req-body': 'mock message',
          'long_webhook_resp-time': 3022,
          'int_webhook_resp-status': 200,
          'int_webhook_resp-body-size': 307,
          'json_webhook_resp-headers': JSON.stringify({
            'Access-Control-Allow-Origin': '*',
            Connection: 'close',
            Date: 'Mon Nov 17 2025 15:42:43 GMT+0100 (Central European Standard Time)',
            'Content-Type': 'text/plain',
          }),
          'string_webhook_resp-body':
            'POST /callback HTTP/1.1 X-Gravitee-Client-Identifier: bf590dde-00d6-43f3-990d-de00d6b3f3f7 X-Gravitee-Transaction-Id: 17e20a10-9eeb-44e9-a20a-109eeb94e9af X-Gravitee-Request-Id: 17e20a10-9eeb-44e9-a20a-109eeb94e9af content-length: 12 host: localhost:3000 accept-encoding: deflate, gzip mock message',
          bool_webhook_dlq: false,
        },
      },
      {
        apiId: 'api-mock',
        requestId: 'req-3',
        timestamp: new Date(1763394228728).toISOString(),
        method: 'POST' as HttpMethod,
        status: 0,
        application: {
          id: '8ff0178d-9326-465c-b017-8d9326a65cb7',
          name: 'Acme Loyalty',
          type: 'SIMPLE',
          apiKeyMode: 'UNSPECIFIED',
        },
        plan: { id: 'plan-incident', name: 'Incident Alerts', mode: 'PUSH' },
        requestEnded: false,
        gatewayResponseTime: 1043,
        uri: 'http://localhost:2500/callback',
        endpoint: 'http://localhost:2500/callback',
        callbackUrl: 'http://localhost:2500/callback',
        duration: '1.0 s',
        message: 'Connection refused: localhost/127.0.0.1:2500',
        errorKey: 'CONNECTION_REFUSED',
        errorComponentName: 'LoyaltyCallback',
        errorComponentType: 'ENDPOINT',
        warnings: [
          {
            componentType: 'POLICY',
            componentName: 'RetryPolicy',
            key: 'retry-scheduled',
            message: 'Next retry scheduled in 60 seconds.',
          },
        ],
        additionalMetrics: {
          'string_webhook_req-method': 'POST',
          string_webhook_url: 'http://localhost:2500/callback',
          'keyword_webhook_app-id': '8ff0178d-9326-465c-b017-8d9326a65cb7',
          'keyword_webhook_sub-id': '95492b8c-5cf9-409b-892b-8c5cf9d09bc8',
          'int_webhook_retry-count': 0,
          'json_webhook_retry-timeline': '[]',
          'string_webhook_last-error': 'Connection refused: localhost/127.0.0.1:2500',
          'long_webhook_req-timestamp': 1763394228728,
          'json_webhook_req-headers': undefined,
          'string_webhook_req-body': undefined,
          'long_webhook_resp-time': 1043,
          'int_webhook_resp-status': 0,
          'int_webhook_resp-body-size': 0,
          'json_webhook_resp-headers': undefined,
          'string_webhook_resp-body': undefined,
          bool_webhook_dlq: false,
        },
      },
    ],
  };

  if (modifier) {
    return {
      ...base,
      ...modifier,
      data: modifier.data ?? base.data,
      pagination: modifier.pagination ?? base.pagination,
    };
  }

  return base;
};

export const fakeWebhookLog = (modifier?: Partial<WebhookLog>): WebhookLog => {
  const baseResponse = fakeWebhookLogsResponse();
  const base: WebhookLog = baseResponse.data[0];

  if (modifier) {
    return {
      ...base,
      ...modifier,
    };
  }

  return base;
};

export const getWebhookLogByRequestId = (requestId: string, logs?: WebhookLog[]): WebhookLog | undefined => {
  const defaultLogs = fakeWebhookLogsResponse().data;
  const logsToSearch = logs ?? defaultLogs;
  return logsToSearch.find(log => log.requestId === requestId);
};
