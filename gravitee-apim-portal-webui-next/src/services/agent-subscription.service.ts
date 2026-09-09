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
import { inject, Injectable } from '@angular/core';
import { catchError, forkJoin, map, Observable, of, switchMap } from 'rxjs';

import { ApplicationService } from './application.service';
import { SubscriptionService } from './subscription.service';
import { Application } from '../entities/application/application';
import { PlanSecurityEnum } from '../entities/plan/plan';
import { isActiveApiKey, Subscription, SubscriptionMetadata } from '../entities/subscription';

export interface AgentSubscriptionSummary {
  subscription: Subscription;
  planName: string;
  planSecurity: PlanSecurityEnum;
  applicationName: string;
}

export interface AgentChatCredentials {
  apiKey: string;
  applicationName: string;
}

export interface AgentSubscriptionAccess {
  subscriptions: AgentSubscriptionSummary[];
  chatCredentials: AgentChatCredentials | null;
}

export interface AgentSubscriptionAccessContext {
  clientId?: string;
  clientSecret?: string;
}

const MAX_CANDIDATES = 10;
const EMPTY_ACCESS: AgentSubscriptionAccess = { subscriptions: [], chatCredentials: null };

@Injectable({
  providedIn: 'root',
})
export class AgentSubscriptionService {
  private readonly subscriptionService = inject(SubscriptionService);
  private readonly applicationService = inject(ApplicationService);

  findForAgent(apiId: string): Observable<AgentSubscriptionAccess> {
    return this.subscriptionService.list({ apiIds: [apiId], statuses: ['ACCEPTED', 'PENDING', 'PAUSED'], size: MAX_CANDIDATES }).pipe(
      switchMap(response => {
        const listed = response.data ?? [];
        const metadata = response.metadata ?? {};
        const accepted = listed.filter(candidate => candidate.status === 'ACCEPTED');
        // Only get() returns the keys, so each accepted candidate needs its own call; they go
        // out together rather than one after another. Pending and paused rows stay in the
        // listing so the panel can show them, but they never carry keys.
        const details$ = accepted.length
          ? forkJoin(accepted.map(candidate => this.subscriptionService.get(candidate.id).pipe(catchError(() => of(null)))))
          : of([] as (Subscription | null)[]);

        return details$.pipe(map(details => this.accessFrom(listed, metadata, accepted, details)));
      }),
      catchError(() => of(EMPTY_ACCESS)),
    );
  }

  loadAccessContexts(summaries: AgentSubscriptionSummary[]): Observable<Map<string, AgentSubscriptionAccessContext>> {
    if (!summaries.length) {
      return of(new Map());
    }

    return forkJoin(summaries.map(summary => this.contextFor(summary))).pipe(
      map(contexts => new Map(summaries.map((summary, index) => [summary.subscription.id, contexts[index]]))),
    );
  }

  private accessFrom(
    listed: Subscription[],
    metadata: SubscriptionMetadata,
    accepted: Subscription[],
    details: (Subscription | null)[],
  ): AgentSubscriptionAccess {
    const detailsById = new Map(accepted.map((candidate, index) => [candidate.id, details[index]]));
    const subscriptions = listed.map(listedSub => {
      const detailed = detailsById.get(listedSub.id);
      return {
        subscription: detailed ?? listedSub,
        planName: metadata[listedSub.plan]?.name ?? '',
        planSecurity: metadata[listedSub.plan]?.securityType ?? 'KEY_LESS',
        applicationName: metadata[listedSub.application]?.name ?? '',
      };
    });

    return {
      subscriptions,
      chatCredentials: this.chatCredentialsFrom(subscriptions),
    };
  }

  private chatCredentialsFrom(summaries: AgentSubscriptionSummary[]): AgentChatCredentials | null {
    for (const summary of summaries) {
      if (summary.subscription.status !== 'ACCEPTED') {
        continue;
      }
      const usableKey = (summary.subscription.keys ?? []).find(key => !!key.key && isActiveApiKey(key));
      if (usableKey?.key) {
        return {
          apiKey: usableKey.key,
          applicationName: usableKey.application?.name ?? '',
        };
      }
    }
    return null;
  }

  private contextFor(summary: AgentSubscriptionSummary): Observable<AgentSubscriptionAccessContext> {
    if (!this.needsClientCredentials(summary.planSecurity)) {
      return of({});
    }

    return this.applicationService.get(summary.subscription.application).pipe(
      map(application => this.clientCredentialsFrom(application)),
      catchError(() => of({})),
    );
  }

  private needsClientCredentials(planSecurity: PlanSecurityEnum): boolean {
    return planSecurity === 'OAUTH2' || planSecurity === 'JWT';
  }

  private clientCredentialsFrom(application: Application | undefined): Pick<AgentSubscriptionAccessContext, 'clientId' | 'clientSecret'> {
    if (!application) {
      return {};
    }
    if (application.settings.oauth) {
      return {
        clientId: application.settings.oauth.client_id,
        clientSecret: application.settings.oauth.client_secret,
      };
    }
    if (application.settings.app) {
      return { clientId: application.settings.app.client_id };
    }
    return {};
  }
}
