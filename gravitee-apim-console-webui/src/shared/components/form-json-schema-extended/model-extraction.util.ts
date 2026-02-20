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
import { EndpointGroupV4 } from '../../../entities/management-api-v2';

export function extractModelsFromConfig(config: unknown): string[] {
  if (!config) {
    return [];
  }

  let parsed = config;
  if (typeof config === 'string') {
    try {
      parsed = JSON.parse(config);
    } catch {
      return [];
    }
  }

  // LLM Proxy endpoints store models as { name, inputPrice?, outputPrice? }[]
  if (parsed && typeof parsed === 'object' && 'models' in parsed && Array.isArray((parsed as { models: unknown[] }).models)) {
    return (parsed as { models: unknown[] }).models
      .map((m: unknown) => {
        if (typeof m === 'string') return m;
        if (m && typeof m === 'object' && 'name' in m) return (m as { name: string }).name;
        return null;
      })
      .filter((name: string | null): name is string => name != null);
  }

  // Single model field
  if (parsed && typeof parsed === 'object' && 'model' in parsed && typeof (parsed as { model: unknown }).model === 'string') {
    return [(parsed as { model: string }).model];
  }

  return [];
}

export type ExtractedModel = {
  name: string;
  inputPrice?: number;
  outputPrice?: number;
  streaming?: boolean;
  thinking?: boolean;
  functionCalling?: boolean;
  contextWindowSize?: number;
  supportedEndpoints?: string[];
  inputModalities?: string[];
  outputModalities?: string[];
};

export function extractFullModelsFromConfig(config: unknown): ExtractedModel[] {
  if (!config) {
    return [];
  }

  let parsed = config;
  if (typeof config === 'string') {
    try {
      parsed = JSON.parse(config);
    } catch {
      return [];
    }
  }

  if (parsed && typeof parsed === 'object' && 'models' in parsed && Array.isArray((parsed as { models: unknown[] }).models)) {
    return (parsed as { models: unknown[] }).models
      .map((m: unknown) => {
        if (typeof m === 'string') return { name: m } as ExtractedModel;
        if (m && typeof m === 'object' && 'name' in m) return m as ExtractedModel;
        return null;
      })
      .filter((model: ExtractedModel | null): model is ExtractedModel => model != null);
  }

  if (parsed && typeof parsed === 'object' && 'model' in parsed && typeof (parsed as { model: unknown }).model === 'string') {
    return [{ name: (parsed as { model: string }).model }];
  }

  return [];
}

export function extractModelsFromEndpointGroups(groups: EndpointGroupV4[]): string[] {
  const allModels: string[] = [];
  for (const group of groups ?? []) {
    for (const endpoint of group.endpoints ?? []) {
      allModels.push(...extractModelsFromConfig(endpoint.configuration));
    }
    allModels.push(...extractModelsFromConfig(group.sharedConfiguration));
  }
  return [...new Set(allModels)];
}
