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
import { catchError, concatMap, defaultIfEmpty, EMPTY, filter, from, map, Observable, of, switchMap, take } from 'rxjs';

import { SubscriptionService } from './subscription.service';
import { isActiveApiKey, Subscription } from '../entities/subscription';

export interface AgentSubscriptionAccess {
  subscriptionId: string;
  apiKey: string;
  applicationName: string;
}

const MAX_CANDIDATES = 10;

@Injectable({
  providedIn: 'root',
})
export class AgentSubscriptionService {
  private readonly subscriptionService = inject(SubscriptionService);

  forAgent(apiId: string): Observable<AgentSubscriptionAccess | null> {
    return this.subscriptionService.list({ apiIds: [apiId], statuses: ['ACCEPTED'], size: MAX_CANDIDATES }).pipe(
      map(response => response.data ?? []),
      switchMap(candidates =>
        from(candidates).pipe(
          concatMap(candidate => this.subscriptionService.get(candidate.id).pipe(catchError(() => EMPTY))),
          map(subscription => this.accessFrom(subscription)),
          filter((access): access is AgentSubscriptionAccess => access !== null),
          take(1),
          defaultIfEmpty(null),
        ),
      ),
      catchError(() => of(null)),
    );
  }

  private accessFrom(subscription: Subscription): AgentSubscriptionAccess | null {
    const usableKey = (subscription.keys ?? []).find(key => !!key.key && isActiveApiKey(key));
    if (!usableKey?.key) {
      return null;
    }
    return {
      subscriptionId: subscription.id,
      apiKey: usableKey.key,
      applicationName: usableKey.application?.name ?? '',
    };
  }
}
