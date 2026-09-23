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

export type AimBudgetPeriod = 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

export interface AimAiWorkspace {
  id: string;
  name: string;
  description?: string | null;
  version: string;
  /** Gateway path for the workspace LLM proxy, when provisioned. */
  contextPath?: string | null;
  assetIds?: string[];
}

export interface AimBudget {
  id: string;
  name: string;
  status?: string | null;
  costBudget?: number | null;
  period?: AimBudgetPeriod | null;
}

export interface AimInlineLlmModel {
  name: string;
  inputPrice?: number | null;
  outputPrice?: number | null;
  aliases?: string[];
}

export interface AimCatalogLlmModel {
  catalogId: string;
  name?: string;
  aliases?: string[];
}

export interface AimInlineLlmProvider {
  kind: 'inline';
  name: string;
  target?: string;
  models: AimInlineLlmModel[];
}

export interface AimCatalogLlmProvider {
  kind: 'catalog';
  catalogSourceId: string;
  models: AimCatalogLlmModel[];
}

export type AimLlmProvider = AimInlineLlmProvider | AimCatalogLlmProvider;

export interface AimCatalogModelPricing {
  inputPer1M?: number | null;
  outputPer1M?: number | null;
  currency?: string | null;
}

export interface AimCatalogModelDefinition {
  name?: string | null;
  pricing?: AimCatalogModelPricing | null;
  pricingOverride?: AimCatalogModelPricing | null;
}

export interface AimCatalogModel {
  id: string;
  definition?: AimCatalogModelDefinition | null;
}

export interface AimCatalogModelPage {
  data?: AimCatalogModel[];
}

/** Resolved catalog display name + pricing for a workspace model row. */
export interface AimCatalogModelInfo {
  name: string;
  inputPer1M?: number | null;
  outputPer1M?: number | null;
}

export type AimCatalogModelInfoMap = Map<string, AimCatalogModelInfo | null>;

export interface AimWorkspaceUsageEntry {
  applicationId: string;
  requests: number;
  tokens: number;
  /** USD dollars (analytics cost metric), not micro-dollars. */
  cost: number;
}

export interface AimWorkspaceUsageResponse {
  usage: AimWorkspaceUsageEntry[];
}

export interface AimWorkspaceModelRow {
  id: string;
  modelName: string;
  inputPrice: string;
  outputPrice: string;
}

export interface AimWorkspaceBudgetConsumed {
  tokensConsumed: string;
  requests: string;
  budgetConsumed: string;
}

const MICRO_DOLLARS_PER_USD = 1_000_000;
const USD_FORMATTER = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 });
const USAGE_COST_FORMATTER = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});
const COUNT_FORMATTER = new Intl.NumberFormat('en-US');

const BUDGET_PERIOD_LABELS: Record<AimBudgetPeriod, string> = {
  HOUR: 'hour',
  DAY: 'day',
  WEEK: 'week',
  MONTH: 'month',
};

const PRICE_UNAVAILABLE = '—';

export function formatAimBudget(budget: AimBudget | null | undefined): string | null {
  if (typeof budget?.costBudget !== 'number' || !budget.period) {
    return null;
  }
  const amount = USD_FORMATTER.format(budget.costBudget / MICRO_DOLLARS_PER_USD);
  return `${amount} / ${BUDGET_PERIOD_LABELS[budget.period]}`;
}

/** Per-1M-token price for the Models table, e.g. `$1.25 / 1M`. */
export function formatAimModelPrice(price: number | null | undefined): string {
  return price === null || price === undefined ? PRICE_UNAVAILABLE : `$${price.toFixed(2)} / 1M`;
}

export function effectiveAimCatalogPricing(
  definition: AimCatalogModelDefinition | null | undefined,
): AimCatalogModelPricing | undefined {
  if (!definition) {
    return undefined;
  }
  const override = definition.pricingOverride;
  if (!override) {
    return definition.pricing ?? undefined;
  }
  return {
    inputPer1M: override.inputPer1M,
    outputPer1M: override.outputPer1M,
    currency: override.currency ?? definition.pricing?.currency ?? 'USD',
  };
}

export function buildAimCatalogModelInfoMap(models: readonly AimCatalogModel[]): AimCatalogModelInfoMap {
  const infos: AimCatalogModelInfoMap = new Map();
  for (const model of models) {
    if (!model.id) {
      continue;
    }
    const name = model.definition?.name?.trim();
    if (!name) {
      infos.set(model.id, null);
      continue;
    }
    const pricing = effectiveAimCatalogPricing(model.definition);
    infos.set(model.id, {
      name,
      inputPer1M: pricing?.inputPer1M,
      outputPer1M: pricing?.outputPer1M,
    });
  }
  return infos;
}

export function formatAimWorkspaceBudgetConsumed(
  usage: AimWorkspaceUsageEntry | null | undefined,
): AimWorkspaceBudgetConsumed {
  const tokens = usage?.tokens ?? 0;
  const requests = usage?.requests ?? 0;
  const cost = usage?.cost ?? 0;
  return {
    tokensConsumed: COUNT_FORMATTER.format(tokens),
    requests: COUNT_FORMATTER.format(requests),
    budgetConsumed: USAGE_COST_FORMATTER.format(cost),
  };
}

/** Flatten AIM providers into one display row per model. */
export function flattenAimWorkspaceModels(
  providers: readonly AimLlmProvider[],
  catalogInfos: AimCatalogModelInfoMap = new Map(),
): AimWorkspaceModelRow[] {
  const rows: AimWorkspaceModelRow[] = [];
  for (const provider of providers) {
    if (provider.kind === 'inline') {
      for (const model of provider.models ?? []) {
        rows.push({
          id: `inline:${provider.name}:${model.name}`,
          modelName: model.name,
          inputPrice: formatAimModelPrice(model.inputPrice),
          outputPrice: formatAimModelPrice(model.outputPrice),
        });
      }
    } else {
      for (const model of provider.models ?? []) {
        const info = catalogInfos.get(model.catalogId);
        rows.push({
          id: `catalog:${provider.catalogSourceId}:${model.catalogId}`,
          modelName: model.name ?? info?.name ?? model.catalogId,
          inputPrice: formatAimModelPrice(info?.inputPer1M),
          outputPrice: formatAimModelPrice(info?.outputPer1M),
        });
      }
    }
  }
  return rows;
}
