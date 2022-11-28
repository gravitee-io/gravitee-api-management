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
    icon: 'public',
    description: 'to log content for the client and the gateway',
  },
  {
    title: 'Client only',
    id: 'CLIENT',
    icon: 'admin_panel_settings',
    description: 'to log content between the client and the gateway',
  },
  {
    title: 'Proxy only',
    id: 'PROXY',
    icon: 'local_police',
    description: 'to log content between the gateway and the backend',
  },
];

export const CONTENT_MODES = [
  {
    title: 'Headers & payloads',
    id: 'HEADERS_PAYLOADS',
    icon: 'file_copy',
    description: 'to log headers and payloads',
  },
  {
    title: 'Headers only',
    id: 'HEADERS',
    icon: 'text_snippet',
    description: 'to log headers without payloads',
  },
  {
    title: 'Payloads only',
    id: 'PAYLOADS',
    icon: 'list_alt',
    description: 'to log payloads without headers',
  },
];

export const SCOPE_MODES = [
  {
    title: 'Request & Response',
    id: 'REQUEST_RESPONSE',
    icon: 'sync_alt',
    description: 'to log request and response',
  },
  {
    title: 'Request only',
    id: 'REQUEST',
    icon: 'arrow_forward',
    description: 'to log request content only',
  },
  {
    title: 'Response only',
    id: 'RESPONSE',
    icon: 'arrow_back',
    description: 'to log response content only',
  },
];
