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
  groupIndex: number;
  endpoints: ProviderEndpoint[];
};

export type ProviderEndpoint = {
  name: string;
  endpointIndex: number;
  providerConfiguration: ProviderConfiguration;
};

export type ProviderConfiguration = {
  provider?: 'OPEN_AI_COMPATIBLE' | 'OPEN_AI';
  models: Model[];
  aliasOnly?: boolean;
  aliasRequiresPrefix?: boolean;
};

export type PipelineKind = 'LOCAL' | 'REMOTE';

export type Model = {
  outputPrice?: number;
  inputPrice?: number;
  name: string;
  aliases?: string[];
  pipelineKind?: PipelineKind;
};

export type Pipeline = {
  id: string;
  kind: PipelineKind;
};

/**
 * Extracts pipelines from a workspace YAML string.
 * Only considers `- id: <pipelineId>` entries located under a top-level
 * `pipelines:` section, within either a `local:` or `remote:` subsection.
 * Entries outside the `pipelines:` section (e.g. other top-level lists that
 * happen to contain `- id:` lines) are ignored.
 */
const extractPipelines = (workspace: string): Pipeline[] => {
  if (!workspace) return [];

  const lines = workspace.split(/\r?\n/);
  const pipelines: Pipeline[] = [];

  const indentOf = (line: string): number => line.match(/^\s*/)?.[0].length ?? 0;
  const isBlankOrComment = (line: string): boolean => /^\s*(#.*)?$/.test(line);

  let inPipelines = false;
  let pipelinesIndent = -1;
  let currentKind: PipelineKind | null = null;
  let kindIndent = -1;

  const pipelinesHeader = /^(\s*)pipelines\s*:\s*$/;
  const localHeader = /^(\s*)local\s*:\s*$/;
  const remoteHeader = /^(\s*)remote\s*:\s*$/;
  const idEntry = /^\s*-\s*id\s*:\s*(.+?)\s*$/;

  for (const line of lines) {
    if (isBlankOrComment(line)) continue;
    const indent = indentOf(line);

    if (inPipelines && indent <= pipelinesIndent) {
      // Left the pipelines section.
      inPipelines = false;
      currentKind = null;
    }

    const pipelinesMatch = pipelinesHeader.exec(line);
    if (pipelinesMatch) {
      inPipelines = true;
      pipelinesIndent = pipelinesMatch[1].length;
      currentKind = null;
      kindIndent = -1;
      continue;
    }

    if (!inPipelines) continue;

    if (currentKind !== null && indent <= kindIndent) {
      currentKind = null;
    }

    const localMatch = localHeader.exec(line);
    if (localMatch && localMatch[1].length > pipelinesIndent) {
      currentKind = 'LOCAL';
      kindIndent = localMatch[1].length;
      continue;
    }

    const remoteMatch = remoteHeader.exec(line);
    if (remoteMatch && remoteMatch[1].length > pipelinesIndent) {
      currentKind = 'REMOTE';
      kindIndent = remoteMatch[1].length;
      continue;
    }

    if (currentKind === null) continue;

    const id = idEntry.exec(line);
    if (id) {
      pipelines.push({ id: id[1].trim(), kind: currentKind });
    }
  }

  return pipelines;
};

/**
 * Converts a Gamma AI endpoint configuration (workspace YAML) into a
 * ProviderConfiguration with pipeline IDs as model names, tagged with
 * their pipeline kind (LOCAL or REMOTE) and N/A costs.
 */
const toGammaAiProviderConfiguration = (configuration: Record<string, unknown>): ProviderConfiguration => {
  const workspace = (configuration?.workspace as string) || '';
  const pipelines = extractPipelines(workspace);
  return {
    provider: undefined,
    models: pipelines.map(p => ({ name: p.id, pipelineKind: p.kind })),
  };
};

const toProviderConfiguration = (groupType: string, configuration: unknown): ProviderConfiguration => {
  if (groupType === 'ai-server') {
    return toGammaAiProviderConfiguration((configuration ?? {}) as Record<string, unknown>);
  }
  return configuration as ProviderConfiguration;
};

export const toProviders = (api: ApiV4): Provider[] => {
  if (!api.endpointGroups) {
    return [];
  }

  return api.endpointGroups
    .filter(endpointGroup => endpointGroup.endpoints && endpointGroup.endpoints.length > 0)
    .map((endpointGroup, groupIndex) => ({
      name: endpointGroup.name,
      type: endpointGroup.type,
      groupIndex,
      endpoints: endpointGroup.endpoints.map((endpoint, endpointIndex) => ({
        name: endpoint.name,
        endpointIndex,
        providerConfiguration: toProviderConfiguration(endpointGroup.type, endpoint.configuration),
      })),
    }));
};
