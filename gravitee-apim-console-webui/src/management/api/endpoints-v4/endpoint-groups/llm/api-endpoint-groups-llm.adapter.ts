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

export type Provider = {
  name: string;
  type: string;
  providerConfiguration: ProviderConfiguration;
};

export type ProviderConfiguration = {
  provider: 'OPEN_AI_COMPATIBLE' | 'OPEN_AI';
  models: Model[];
};

export type Model = {
  outputPrice?: number;
  inputPrice?: number;
  name: string;
};

/**
 * Extracts pipeline IDs from a workspace YAML string.
 * Looks for lines matching "- id: <pipelineId>" under the pipelines section.
 */
const extractPipelineIds = (workspace: string): string[] => {
  if (!workspace) return [];
  const ids: string[] = [];
  const regex = /^\s*-\s*id:\s*(.+)$/gm;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(workspace)) !== null) {
    ids.push(match[1].trim());
  }
  return ids;
};

/**
 * Converts a Gamma AI endpoint configuration (workspace YAML) into a
 * ProviderConfiguration with pipeline IDs as model names and N/A costs.
 */
const toGammaAiProviderConfiguration = (configuration: Record<string, unknown>): ProviderConfiguration => {
  const workspace = (configuration?.workspace as string) || '';
  const pipelineIds = extractPipelineIds(workspace);
  return {
    provider: undefined,
    models: pipelineIds.map((id) => ({ name: id })),
  };
};

export const toProviders = (api: ApiV4): Provider[] => {
  if (!api.endpointGroups) {
    return [];
  }

  return api.endpointGroups
    .filter((endpointGroup) => endpointGroup.endpoints && endpointGroup.endpoints.length > 0)
    .map((endpointGroup) => {
      const endpoint = endpointGroup.endpoints[0];

      // Gamma AI endpoints use a workspace YAML instead of provider/models
      if (endpointGroup.type === 'ai-server') {
        return {
          name: endpointGroup.name,
          type: endpointGroup.type,
          providerConfiguration: toGammaAiProviderConfiguration(endpoint.configuration as Record<string, unknown>),
        };
      }

      return {
        name: endpointGroup.name,
        type: endpointGroup.type,
        providerConfiguration: endpoint.configuration as ProviderConfiguration,
      };
    });
};
