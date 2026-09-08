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
import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { catchError, of } from 'rxjs';

import { DocumentationSkeletonComponent } from '../../../../components/documentation-skeleton/documentation-skeleton.component';
import { AgentCatalogItem, AgentCapabilities, AgentDefinition } from '../../../../entities/agent/agent-catalog-info';
import { AgentCatalogService } from '../../../../services/agent-catalog.service';

@Component({
  selector: 'app-agent-detail',
  imports: [DatePipe, MatButtonModule, MatChipsModule, MatIconModule, MatTooltipModule, DocumentationSkeletonComponent],
  templateUrl: './agent-detail.component.html',
  styleUrl: './agent-detail.component.scss',
})
export class AgentDetailComponent {
  // TODO: Remove FALLBACK_METADATA once the Gamma catalog agent is linked to the navigation item.
  // Temporary dummy data used until the real agent definition is available.
  private static readonly FALLBACK_METADATA: Record<string, string> = {
    protocol: 'A2A (Agent-to-Agent)',
    runtime: 'Gravitee AI Gateway',
    'max-tokens': '8 192',
    'rate-limit': '60 req / min',
    region: 'eu-west-1',
  };

  private readonly agentCatalogService = inject(AgentCatalogService);

  title = input<string>();
  orgId = input.required<string>();
  envId = input.required<string>();

  agent = rxResource<AgentCatalogItem | null, { title?: string; orgId: string; envId: string }>({
    params: computed(() => ({ title: this.title(), orgId: this.orgId(), envId: this.envId() })),
    stream: ({ params }) => {
      if (!params.orgId || !params.envId) {
        return of(null);
      }
      if (params.title) {
        return this.agentCatalogService.findAgentByName(params.orgId, params.envId, params.title).pipe(catchError(() => of(null)));
      }
      return of(null);
    },
  });

  // TODO: Remove fallbackDefinition once the Gamma catalog agent is linked to the navigation item.
  // Temporary dummy data used until the real agent definition is available.
  private readonly fallbackDefinition = computed<AgentDefinition>(() => ({
    name: this.title() ?? '',
    description:
      'An AI-powered agent that understands your API ecosystem. It can answer questions about API specifications, ' +
      'assist with subscription configuration, generate code snippets for popular languages, and provide ' +
      'real-time guidance on rate limits, quotas, and best practices — all through the A2A protocol.',
    url: 'https://agents.gravitee.io/a2a',
    version: '2.1.0',
    documentationUrl: 'https://documentation.gravitee.io/apim/agents',
    provider: { organization: 'Gravitee.io', url: 'https://gravitee.io' },
    capabilities: { streaming: true, pushNotifications: true, stateTransitionHistory: true },
    defaultInputModes: ['text', 'file'],
    defaultOutputModes: ['text', 'file'],
    skills: [
      {
        id: 'skill-api-explorer',
        name: 'API Explorer',
        description:
          'Navigates and explains OpenAPI / AsyncAPI specifications. Summarizes endpoints, schemas, and authentication requirements.',
        tags: ['openapi', 'asyncapi', 'discovery'],
        examples: ['List all POST endpoints in the Payments API', 'What authentication does the Orders API require?'],
      },
      {
        id: 'skill-subscription-helper',
        name: 'Subscription Helper',
        description: 'Guides you through API plan selection, subscription creation, and key management.',
        tags: ['subscription', 'plans', 'keys'],
        examples: ['Which plan fits 10 000 requests per day?', 'How do I rotate my API key?'],
      },
      {
        id: 'skill-code-gen',
        name: 'Code Generator',
        description: 'Produces ready-to-run code snippets for calling APIs in multiple languages and frameworks.',
        tags: ['codegen', 'sdk', 'integration'],
        examples: ['Generate a Python requests call for POST /orders', 'Show me a cURL for the health-check endpoint'],
        inputModes: ['text'],
        outputModes: ['text', 'file'],
      },
      {
        id: 'skill-troubleshoot',
        name: 'Troubleshooter',
        description: 'Diagnoses common API errors (4xx / 5xx), rate-limit issues, and policy misconfigurations.',
        tags: ['debug', 'errors', 'rate-limit'],
        examples: ['Why am I getting a 429 on /payments?', 'Explain this CORS error'],
      },
    ],
  }));

  definition = computed(() => (!this.agent.error() ? this.agent.value()?.definition : undefined) ?? this.fallbackDefinition());
  provider = computed(() => this.definition()?.provider ?? null);
  capabilities = computed(() => this.definition()?.capabilities ?? null);
  skills = computed(() => this.definition()?.skills ?? []);

  metadata = computed(() => {
    const agentValue = !this.agent.error() ? this.agent.value() : undefined;
    if (agentValue?.definition) {
      return agentValue.metadata ?? null;
    }
    return AgentDetailComponent.FALLBACK_METADATA;
  });

  hasAnyCapability = computed(() => {
    const caps = this.capabilities();
    return caps != null && (caps.streaming || caps.pushNotifications || caps.stateTransitionHistory);
  });

  enabledCapabilities = computed(() => {
    const caps = this.capabilities();
    if (!caps) return [];
    const result: { key: keyof AgentCapabilities; label: string }[] = [];
    if (caps.streaming) result.push({ key: 'streaming', label: $localize`:@@agentCapStreaming:Streaming` });
    if (caps.pushNotifications) result.push({ key: 'pushNotifications', label: $localize`:@@agentCapPush:Push Notifications` });
    if (caps.stateTransitionHistory)
      result.push({ key: 'stateTransitionHistory', label: $localize`:@@agentCapStateHistory:State History` });
    return result;
  });

  hasInputModes = computed(() => (this.definition()?.defaultInputModes?.length ?? 0) > 0);
  hasOutputModes = computed(() => (this.definition()?.defaultOutputModes?.length ?? 0) > 0);
  hasIoModes = computed(() => this.hasInputModes() || this.hasOutputModes());

  metadataEntries = computed(() => {
    const meta = this.metadata();
    if (!meta) return [];
    return Object.entries(meta);
  });

  updatedDate = computed(() => {
    const raw = !this.agent.error() ? this.agent.value()?.updateDate : undefined;
    if (!raw) return null;
    const d = new Date(raw);
    return isNaN(d.getTime()) ? null : d;
  });

  safeDocumentationUrl = computed(() => this.sanitizeUrl(this.definition()?.documentationUrl));
  safeProviderUrl = computed(() => this.sanitizeUrl(this.provider()?.url));

  private sanitizeUrl(url: string | undefined | null): string | null {
    if (!url) return null;
    try {
      const parsed = new URL(url);
      return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? url : null;
    } catch {
      return null;
    }
  }
}
