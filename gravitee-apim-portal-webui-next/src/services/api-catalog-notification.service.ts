/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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

import { ApplicationNotificationService } from './application-notification.service';
import { SubscriptionService } from './subscription.service';
import {
  isApiCatalogNotificationSubscribed,
  PortalNotificationHook,
  withAllApiNotificationHooks,
  withoutApiDistinctiveNotificationHooks,
} from '../entities/notification/portal-notification-hook';

export type ApiCatalogNotificationToggleResult =
  | { status: 'subscribed'; applicationIds: string[] }
  | { status: 'unsubscribed'; applicationIds: string[] }
  | { status: 'no-subscription' };

@Injectable({
  providedIn: 'root',
})
export class ApiCatalogNotificationService {
  private readonly applicationNotificationService = inject(ApplicationNotificationService);
  private readonly subscriptionService = inject(SubscriptionService);

  /** API ids that already have full API notification coverage on a subscribed application. */
  getSubscribedApiIds(apiIds: string[]): Observable<Set<string>> {
    if (!apiIds.length) {
      return of(new Set<string>());
    }

    return this.subscriptionService
      .list({
        apiIds,
        size: -1,
        statuses: ['ACCEPTED', 'PAUSED', 'PENDING'],
      })
      .pipe(
        catchError(() => of({ data: [], links: {}, metadata: {} })),
        switchMap(response => {
          const applicationsByApi = new Map<string, Set<string>>();
          for (const subscription of response.data ?? []) {
            const apiId = subscription.api ?? subscription.reference_id;
            if (!apiId || !subscription.application) {
              continue;
            }
            const apps = applicationsByApi.get(apiId) ?? new Set<string>();
            apps.add(subscription.application);
            applicationsByApi.set(apiId, apps);
          }

          const applicationIds = [...new Set([...applicationsByApi.values()].flatMap(apps => [...apps]))];
          if (!applicationIds.length) {
            return of(new Set<string>());
          }

          return forkJoin(
            applicationIds.map(applicationId =>
              this.applicationNotificationService.getNotifications(applicationId).pipe(
                catchError(() => of([] as string[])),
                map(hooks => ({ applicationId, hooks })),
              ),
            ),
          ).pipe(
            map(results => {
              const subscribedApps = new Set(
                results.filter(result => isApiCatalogNotificationSubscribed(result.hooks)).map(result => result.applicationId),
              );
              const subscribedApis = new Set<string>();
              for (const [apiId, apps] of applicationsByApi) {
                if ([...apps].some(appId => subscribedApps.has(appId))) {
                  subscribedApis.add(apiId);
                }
              }
              return subscribedApis;
            }),
          );
        }),
      );
  }

  toggleApiNotifications(apiId: string): Observable<ApiCatalogNotificationToggleResult> {
    return this.subscriptionService
      .list({
        apiIds: [apiId],
        size: -1,
        statuses: ['ACCEPTED', 'PAUSED', 'PENDING'],
      })
      .pipe(
        catchError(() => of({ data: [], links: {}, metadata: {} })),
        switchMap(response => {
          const applicationIds = [
            ...new Set(
              (response.data ?? [])
                .map(subscription => subscription.application)
                .filter((applicationId): applicationId is string => !!applicationId),
            ),
          ];

          if (!applicationIds.length) {
            return of({ status: 'no-subscription' } as const);
          }

          return forkJoin({
            catalogue: this.applicationNotificationService.getHooks().pipe(catchError(() => of([] as PortalNotificationHook[]))),
            current: forkJoin(
              applicationIds.map(applicationId =>
                this.applicationNotificationService.getNotifications(applicationId).pipe(
                  catchError(() => of([] as string[])),
                  map(hooks => ({ applicationId, hooks })),
                ),
              ),
            ),
          }).pipe(
            switchMap(({ catalogue, current }) => {
              const currentlySubscribed = current.every(entry => isApiCatalogNotificationSubscribed(entry.hooks));
              const updates = current.map(entry => {
                const nextHooks = currentlySubscribed
                  ? withoutApiDistinctiveNotificationHooks(entry.hooks)
                  : withAllApiNotificationHooks(entry.hooks, catalogue);
                return this.applicationNotificationService.updateNotifications(entry.applicationId, nextHooks).pipe(
                  map(() => entry.applicationId),
                  catchError(() => of(null)),
                );
              });

              return forkJoin(updates).pipe(
                map(results => {
                  const updatedIds = results.filter((id): id is string => !!id);
                  return {
                    status: currentlySubscribed ? ('unsubscribed' as const) : ('subscribed' as const),
                    applicationIds: updatedIds,
                  };
                }),
              );
            }),
          );
        }),
      );
  }
}
