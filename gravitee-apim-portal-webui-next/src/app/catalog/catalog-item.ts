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
import type { LocalizeFn } from '@angular/localize/init';

import { ApiCardAccess } from '../../components/api-card/api-card.component';
import { ApiType } from '../../entities/api/api';
import { Plan } from '../../entities/plan/plan';

declare const $localize: LocalizeFn;

export type CatalogKind = 'agents' | 'apis';

const AGENT_API_TYPES: ApiType[] = ['A2A_PROXY', 'MCP_PROXY', 'LLM_PROXY'];

export function isAgent(apiType: ApiType | undefined, hasMcpServer: boolean): boolean {
  return (!!apiType && AGENT_API_TYPES.includes(apiType)) || hasMcpServer;
}

const PROTOCOL_LABELS: Partial<Record<ApiType, string>> = {
  A2A_PROXY: 'A2A',
  MCP_PROXY: 'MCP',
  LLM_PROXY: 'LLM',
  NATIVE: 'Kafka',
  MESSAGE: 'Async',
  PROXY: 'REST',
};

export function protocolLabel(apiType: ApiType | undefined, hasMcpServer: boolean): string {
  if (hasMcpServer && apiType !== 'A2A_PROXY') {
    return 'MCP';
  }
  return (apiType && PROTOCOL_LABELS[apiType]) ?? 'REST';
}

export function catalogAccess(plans: Plan[], subscribed: boolean): ApiCardAccess | undefined {
  if (subscribed) {
    return 'SUBSCRIBED';
  }
  if (plans.length === 0) {
    return undefined;
  }
  if (plans.some(plan => plan.security === 'KEY_LESS')) {
    return 'NO_KEY';
  }
  return plans.every(plan => plan.validation === 'MANUAL') ? 'APPROVAL' : 'CREDENTIALS';
}

export function accessLabel(access: ApiCardAccess): string {
  switch (access) {
    case 'SUBSCRIBED':
      return $localize`:@@catalogAccessSubscribed:Subscribed`;
    case 'NO_KEY':
      return $localize`:@@catalogAccessNoKey:No key needed`;
    case 'APPROVAL':
      return $localize`:@@catalogAccessApproval:Approval needed`;
    default:
      return $localize`:@@catalogAccessCredentials:Credentials needed`;
  }
}

const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
const SPANS: [days: number, unit: Intl.RelativeTimeFormatUnit][] = [
  [365, 'year'],
  [30, 'month'],
  [7, 'week'],
  [1, 'day'],
];

export function publishedLabel(publishedAt: Date | string | undefined, locale: string, now: Date = new Date()): string | undefined {
  if (!publishedAt) {
    return undefined;
  }
  const days = Math.floor((now.getTime() - new Date(publishedAt).getTime()) / MILLIS_PER_DAY);
  const span = SPANS.find(([size]) => days >= size);
  if (!span) {
    return $localize`:@@catalogPublishedToday:today`;
  }
  const [size, unit] = span;
  return new Intl.RelativeTimeFormat(locale, { numeric: 'always' }).format(-Math.floor(days / size), unit);
}
