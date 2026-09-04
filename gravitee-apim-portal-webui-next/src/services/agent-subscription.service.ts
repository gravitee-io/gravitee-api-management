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

import { SubscriptionService } from './subscription.service';
import { isActiveApiKey, Subscription } from '../entities/subscription';

export interface AgentSubscriptionAccess {
  apiKey: string;
  applicationName: string;
}

const MAX_CANDIDATES = 10;

@Injectable({
  providedIn: 'root',
})
export class AgentSubscriptionService {
  private readonly subscriptionService = inject(SubscriptionService);

  findForAgent(apiId: string): Observable<AgentSubscriptionAccess | null> {
    return this.subscriptionService.list({ apiIds: [apiId], statuses: ['ACCEPTED'], size: MAX_CANDIDATES }).pipe(
      map(response => response.data ?? []),
      // Only get() returns the keys, so each candidate needs its own call; they go out together
      // rather than one after another, and the first usable one in listing order wins so the
      // application named in the panel stays the same between visits.
      switchMap(candidates =>
        candidates.length
          ? forkJoin(candidates.map(candidate => this.subscriptionService.get(candidate.id).pipe(catchError(() => of(null)))))
          : of([]),
      ),
      map(subscriptions => subscriptions.map(subscription => (subscription ? this.accessFrom(subscription) : null)).find(Boolean) ?? null),
      catchError(() => of(null)),
    );
  }

  private accessFrom(subscription: Subscription): AgentSubscriptionAccess | null {
    const usableKey = (subscription.keys ?? []).find(key => !!key.key && isActiveApiKey(key));
    if (!usableKey?.key) {
      return null;
    }
    return {
      apiKey: usableKey.key,
      applicationName: usableKey.application?.name ?? '',
    };
  }
}
