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
  private readonly agentCatalogService = inject(AgentCatalogService);

  title = input<string>();
  agentId = input<string>();
  orgId = input.required<string>();
  envId = input.required<string>();

  agent = rxResource<AgentCatalogItem | null, { agentId?: string; title?: string; orgId: string; envId: string }>({
    params: computed(() => ({ agentId: this.agentId(), title: this.title(), orgId: this.orgId(), envId: this.envId() })),
    stream: ({ params }) => {
      if (params.agentId) {
        return this.agentCatalogService.getAgent(params.agentId).pipe(catchError(() => of(null)));
      }
      if (!params.orgId || !params.envId) {
        return of(null);
      }
      if (params.title) {
        return this.agentCatalogService.findAgentByName(params.orgId, params.envId, params.title).pipe(catchError(() => of(null)));
      }
      return of(null);
    },
  });

  definition = computed(
    () => (!this.agent.error() ? this.agent.value()?.definition : undefined) ?? this.emptyDefinition(this.title() ?? ''),
  );
  provider = computed(() => this.definition()?.provider ?? null);
  capabilities = computed(() => this.definition()?.capabilities ?? null);
  skills = computed(() => this.definition()?.skills ?? []);

  metadata = computed(() => {
    const agentValue = !this.agent.error() ? this.agent.value() : undefined;
    return agentValue?.metadata ?? null;
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

  private emptyDefinition(name: string): AgentDefinition {
    return {
      name,
      url: '',
      version: '',
      capabilities: {},
      defaultInputModes: [],
      defaultOutputModes: [],
      skills: [],
    };
  }

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
