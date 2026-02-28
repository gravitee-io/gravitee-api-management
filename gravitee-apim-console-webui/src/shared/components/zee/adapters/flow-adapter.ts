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

import { ZeeResourceAdapter } from '../zee.model';

export const FLOW_ADAPTER: ZeeResourceAdapter = {
  previewLabel: 'Generated Flow',
  transform: (generated: any): any => {
    // Map rehydrated Gravitee API Definition Flow shape
    // to the REST API save payload format
    return {
      name: generated.name,
      enabled: generated.enabled ?? true,
      selectors: generated.selectors?.map(mapSelector) ?? [],
      request: generated.request?.map(mapStep) ?? [],
      response: generated.response?.map(mapStep) ?? [],
      subscribe: generated.subscribe?.map(mapStep) ?? [],
      publish: generated.publish?.map(mapStep) ?? [],
      tags: generated.tags ?? [],
    };
  },
};

function mapStep(step: any) {
  let configuration = {};
  if (step.configuration) {
    try {
      configuration = typeof step.configuration === 'string' ? JSON.parse(step.configuration) : step.configuration;
    } catch (e) {
      console.warn('Zee FLOW_ADAPTER: Failed to parse step configuration json', step.configuration, e);
    }
  }

  return {
    name: step.name,
    policy: step.policy,
    enabled: step.enabled ?? true,
    description: step.description,
    condition: step.condition,
    configuration,
    messageCondition: step.messageCondition,
  };
}

function mapSelector(sel: any) {
  // Map based on discriminator type
  switch (sel.type) {
    case 'http':
    case 'HTTP':
      return { type: 'HTTP', path: sel.path, pathOperator: sel.pathOperator, methods: sel.methods };
    case 'channel':
    case 'CHANNEL':
      return { type: 'CHANNEL', channel: sel.channel, channelOperator: sel.channelOperator, operations: sel.operations, entrypoints: sel.entrypoints };
    case 'condition':
    case 'CONDITION':
      return { type: 'CONDITION', condition: sel.condition };
    default:
      return sel;
  }
}
