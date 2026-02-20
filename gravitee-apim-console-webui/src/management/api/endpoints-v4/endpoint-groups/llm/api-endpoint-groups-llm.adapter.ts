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
import { ApiV4 } from '../../../../../entities/management-api-v2';

export type SupportedEndpoint = 'CHAT_COMPLETIONS' | 'RESPONSES' | 'EMBEDDINGS';

export type InputModality = 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO' | 'DOCUMENT';

export type OutputModality = 'TEXT' | 'IMAGE' | 'AUDIO';

export type Provider = {
  name: string;
  providerConfiguration: ProviderConfiguration;
};

export type ProviderConfiguration = {
  provider: 'OPEN_AI_COMPATIBLE' | 'OPEN_AI' | 'GEMINI' | 'BEDROCK';
  models: Model[];
};

export type Model = {
  name: string;
  inputPrice?: number;
  outputPrice?: number;
  streaming?: boolean;
  thinking?: boolean;
  functionCalling?: boolean;
  contextWindowSize?: number;
  supportedEndpoints?: SupportedEndpoint[];
  inputModalities?: InputModality[];
  outputModalities?: OutputModality[];
};

export const toProviders = (api: ApiV4): Provider[] => {
  if (!api.endpointGroups) {
    return [];
  }

  return api.endpointGroups
    .filter((endpointGroup) => endpointGroup.endpoints && endpointGroup.endpoints.length > 0)
    .map((endpointGroup) => {
      const endpoint = endpointGroup.endpoints[0];

      return {
        name: endpointGroup.name,
        providerConfiguration: endpoint.configuration as ProviderConfiguration,
      };
    });
};
