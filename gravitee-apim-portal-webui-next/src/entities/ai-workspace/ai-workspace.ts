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
import { Links } from '../pagination/links';

export type AiWorkspaceBudgetPeriod = 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

export interface AiWorkspaceBudget {
  amount?: number;
  period?: AiWorkspaceBudgetPeriod;
}

export interface AiWorkspaceSummary {
  id: string;
  name: string;
  description?: string;
  budget?: AiWorkspaceBudget;
}

export type AiWorkspaceKeyStatus = 'ACTIVE' | 'PAUSED';

export interface AiWorkspaceKey {
  value: string;
  status: AiWorkspaceKeyStatus;
  createdAt?: string;
  revokedAt?: string;
  expiredAt?: string;
}

export interface AiWorkspace extends AiWorkspaceSummary {
  endpointUrl?: string;
  key?: AiWorkspaceKey;
}

export interface AiWorkspaceConsumption {
  tokens: number;
  requests: number;
  cost: number;
  from: string;
  to: string;
}

export interface AiWorkspaceModel {
  name: string;
  inputPrice?: string;
  outputPrice?: string;
}

export interface AiWorkspacesResponse {
  data?: AiWorkspaceSummary[];
  metadata?: {
    pagination?: {
      current_page?: number;
      size?: number;
      total?: number;
      total_pages?: number;
    };
  };
  links?: Links;
}

export interface AiWorkspaceModelsResponse {
  data?: AiWorkspaceModel[];
  metadata?: {
    pagination?: {
      current_page?: number;
      size?: number;
      total?: number;
      total_pages?: number;
    };
  };
  links?: Links;
}

export function formatAiWorkspaceBudget(budget?: AiWorkspaceBudget | null): string {
  if (budget?.amount == null) {
    return '—';
  }
  const amount = `$${budget.amount.toFixed(2)}`;
  if (!budget.period) {
    return amount;
  }
  return `${amount} / ${budget.period.toLowerCase()}`;
}

export function formatAiWorkspaceCost(cost?: number | null): string {
  if (cost == null) {
    return '$0.00';
  }
  return `$${cost.toFixed(2)}`;
}
