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
import { toProviders } from './api-endpoint-groups-llm.adapter';

import { ApiV4 } from '../../../../../entities/management-api-v2';

const aiServerApi = (workspace: string): ApiV4 =>
  ({
    endpointGroups: [
      {
        name: 'gamma',
        type: 'ai-server',
        endpoints: [
          {
            name: 'endpoint-1',
            configuration: { workspace },
          },
        ],
      },
    ],
  }) as unknown as ApiV4;

describe('api-endpoint-groups-llm.adapter', () => {
  describe('toProviders (ai-server / pipelines extraction)', () => {
    it('extracts only pipelines and tags them by kind', () => {
      const workspace = `
pipelines:
  local:
    - id: local-pipeline-1
      name: Local One
    - id: local-pipeline-2
  remote:
    - id: remote-pipeline-1

connections:
  - id: connection-should-be-ignored
`;
      const [provider] = toProviders(aiServerApi(workspace));
      const models = provider.endpoints[0].providerConfiguration.models;

      expect(models).toEqual([
        { name: 'local-pipeline-1', pipelineKind: 'LOCAL' },
        { name: 'local-pipeline-2', pipelineKind: 'LOCAL' },
        { name: 'remote-pipeline-1', pipelineKind: 'REMOTE' },
      ]);
    });

    it('ignores `- id:` entries outside the pipelines section', () => {
      const workspace = `
datasources:
  - id: ds-1
  - id: ds-2
pipelines:
  local:
    - id: p-local
other:
  - id: ignored
`;
      const [provider] = toProviders(aiServerApi(workspace));
      const models = provider.endpoints[0].providerConfiguration.models;

      expect(models).toEqual([{ name: 'p-local', pipelineKind: 'LOCAL' }]);
    });

    it('ignores pipelines without a local/remote kind', () => {
      const workspace = `
pipelines:
  - id: no-kind
`;
      const [provider] = toProviders(aiServerApi(workspace));
      const models = provider.endpoints[0].providerConfiguration.models;

      expect(models).toEqual([]);
    });

    it('returns empty models when workspace is empty or missing', () => {
      expect(toProviders(aiServerApi(''))[0].endpoints[0].providerConfiguration.models).toEqual([]);
      expect(
        toProviders({
          endpointGroups: [{ name: 'gamma', type: 'ai-server', endpoints: [{ name: 'e', configuration: {} }] }],
        } as unknown as ApiV4)[0].endpoints[0].providerConfiguration.models,
      ).toEqual([]);
    });

    it('stops collecting when a new top-level key follows pipelines', () => {
      const workspace = `pipelines:
  remote:
    - id: only-remote
other-top-level:
  remote:
    - id: must-not-be-collected
`;
      const [provider] = toProviders(aiServerApi(workspace));
      const models = provider.endpoints[0].providerConfiguration.models;

      expect(models).toEqual([{ name: 'only-remote', pipelineKind: 'REMOTE' }]);
    });
  });

  describe('toProviders (non-ai-server)', () => {
    it('passes the endpoint configuration through unchanged as ProviderConfiguration', () => {
      const api = {
        endpointGroups: [
          {
            name: 'openai-group',
            type: 'llm-proxy',
            endpoints: [
              {
                name: 'endpoint-1',
                configuration: {
                  provider: 'OPEN_AI',
                  models: [{ name: 'gpt-4', inputPrice: 1, outputPrice: 2 }],
                },
              },
            ],
          },
        ],
      } as unknown as ApiV4;

      const [provider] = toProviders(api);
      const config = provider.endpoints[0].providerConfiguration;
      expect(config.provider).toBe('OPEN_AI');
      expect(config.models).toEqual([{ name: 'gpt-4', inputPrice: 1, outputPrice: 2 }]);
    });
  });
});
