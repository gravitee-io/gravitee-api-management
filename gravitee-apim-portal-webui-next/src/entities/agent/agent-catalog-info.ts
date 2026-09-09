/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

export interface AgentCatalogItem {
  id: string;
  kind: 'agent';
  entityId?: string;
  slug?: string;
  sourceId: string;
  sourceKind: string;
  environmentId: string;
  organizationId: string;
  creationDate: string;
  updateDate: string;
  metadata?: Record<string, string>;
  definition: AgentDefinition;
}

export interface AgentDefinition {
  name: string;
  description?: string;
  url: string;
  provider?: AgentProvider;
  version: string;
  documentationUrl?: string;
  capabilities: AgentCapabilities;
  defaultInputModes: string[];
  defaultOutputModes: string[];
  skills: AgentSkill[];
}

export interface AgentProvider {
  organization: string;
  url?: string;
}

export interface AgentCapabilities {
  streaming?: boolean;
  pushNotifications?: boolean;
  stateTransitionHistory?: boolean;
}

export interface AgentSkill {
  id: string;
  name: string;
  description?: string;
  tags?: string[];
  examples?: string[];
  inputModes?: string[];
  outputModes?: string[];
}

export interface AgentCatalogPage {
  data: AgentCatalogItem[];
  pagination: {
    page: number;
    perPage: number;
    pageCount: number;
    totalCount: number;
  };
}
