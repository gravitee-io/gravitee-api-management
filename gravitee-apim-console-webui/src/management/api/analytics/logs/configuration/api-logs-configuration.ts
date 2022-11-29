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
export const DEFAULT_LOGGING = { mode: 'CLIENT_PROXY', content: 'HEADERS_PAYLOADS', scope: 'REQUEST_RESPONSE' };

export const LOGGING_MODES = [
  {
    title: 'Client & proxy',
    id: 'CLIENT_PROXY',
    icon: 'gio:language',
    description: 'to log content for the client and the gateway',
  },
  {
    title: 'Client only',
    id: 'CLIENT',
    icon: 'gio:laptop',
    description: 'to log content between the client and the gateway',
  },
  {
    title: 'Proxy only',
    id: 'PROXY',
    icon: 'gio:hard-drive',
    description: 'to log content between the gateway and the backend',
  },
];

export const CONTENT_MODES = [
  {
    title: 'Headers & payloads',
    id: 'HEADERS_PAYLOADS',
    icon: 'gio:empty-page',
    description: 'to log headers and payloads',
  },
  {
    title: 'Headers only',
    id: 'HEADERS',
    icon: 'gio:file-minus',
    description: 'to log headers without payloads',
  },
  {
    title: 'Payloads only',
    id: 'PAYLOADS',
    icon: 'gio:page',
    description: 'to log payloads without headers',
  },
];

export const SCOPE_MODES = [
  {
    title: 'Request & Response',
    id: 'REQUEST_RESPONSE',
    icon: 'gio:data-transfer-both',
    description: 'to log request and response',
  },
  {
    title: 'Request only',
    id: 'REQUEST',
    icon: 'gio:data-transfer-up',
    description: 'to log request content only',
  },
  {
    title: 'Response only',
    id: 'RESPONSE',
    icon: 'gio:data-transfer-down',
    description: 'to log response content only',
  },
];
