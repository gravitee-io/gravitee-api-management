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
import { ApiV4 } from '../api';

export type ApiProtocolType = 'HTTP_PROXY' | 'HTTP_MESSAGE' | 'NATIVE_KAFKA' | 'MCP_PROXY' | 'LLM_PROXY' | 'A2A_PROXY';

export const getApiProtocolTypeFromApi = (api: ApiV4): ApiProtocolType => {
  switch (api.type) {
    case 'PROXY':
      return 'HTTP_PROXY';
    case 'MESSAGE':
      return 'HTTP_MESSAGE';
    case 'NATIVE':
      return 'NATIVE_KAFKA';
    case 'MCP_PROXY':
      return 'MCP_PROXY';
    case 'LLM_PROXY':
      return 'LLM_PROXY';
    case 'A2A_PROXY':
      return 'A2A_PROXY';
  }
};
