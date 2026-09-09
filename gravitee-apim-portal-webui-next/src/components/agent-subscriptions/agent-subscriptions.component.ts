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
import { Component, computed, inject, input, output } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { RouterLink } from '@angular/router';

import { isActiveApiKey } from '../../entities/subscription';
import { CapitalizeFirstPipe } from '../../pipe/capitalize-first.pipe';
import {
  AgentSubscriptionAccessContext,
  AgentSubscriptionService,
  AgentSubscriptionSummary,
} from '../../services/agent-subscription.service';
import { ConfigService } from '../../services/config.service';
import { AgentAccessCardComponent } from '../agent-access-card/agent-access-card.component';

@Component({
  selector: 'app-agent-subscriptions',
  imports: [MatButtonModule, MatExpansionModule, RouterLink, AgentAccessCardComponent, CapitalizeFirstPipe],
  templateUrl: './agent-subscriptions.component.html',
  styleUrl: './agent-subscriptions.component.scss',
})
export class AgentSubscriptionsComponent {
  private readonly agentSubscriptionService = inject(AgentSubscriptionService);
  private readonly configService = inject(ConfigService);

  agentName = input.required<string>();
  entrypointUrls = input<string[]>([]);
  subscriptions = input.required<AgentSubscriptionSummary[]>();

  newSubscription = output<void>();

  protected readonly apiKeyHeader = this.configService.configuration.portal?.apikeyHeader ?? '';

  private readonly contexts = rxResource<Map<string, AgentSubscriptionAccessContext>, AgentSubscriptionSummary[]>({
    params: () => this.subscriptions(),
    stream: ({ params }) => this.agentSubscriptionService.loadAccessContexts(params),
  });

  protected readonly showApplicationName = computed(() => this.subscriptions().length > 1);
  protected readonly entrypointUrl = computed(() => this.entrypointUrls()[0] ?? '');
  protected readonly rows = computed(() => {
    const contexts = this.contexts.value() ?? new Map();
    return this.subscriptions().map(summary => {
      const usableKey = (summary.subscription.keys ?? []).find(key => !!key.key && isActiveApiKey(key));
      return {
        summary,
        context: contexts.get(summary.subscription.id) ?? {},
        apiKey: usableKey?.key ?? '',
      };
    });
  });
}
