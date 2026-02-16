/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { HttpClient, HttpParams } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { PagedResult } from '../entities/pagedResult';
import { Application, ApplicationType } from '../entities/application/Application';
import { SubscribedApi } from '../entities/application/SubscribedApi';
import { SubscriptionPage } from '../entities/subscription/subscription';
import { ApplicationLog } from '../entities/application/ApplicationLog';
import { MembershipListItem } from '../entities/role/membershipListItem';
import { CreateApplication } from '../entities/application/CreateApplication';
import { ApplicationSubscriptionApiKey } from '../entities/subscription/ApplicationSubscriptionApiKey';

@Injectable({
  providedIn: 'root',
})
export class ApplicationService {
  private lastApplicationFetch$: BehaviorSubject<Application | null> = new BehaviorSubject<Application | null>(null);
  private refreshLastApplicationFetch$ = new Subject<void>();

  constructor(
    private readonly http: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  getMembers(applicationId: string): Observable<MembershipListItem[]> {
    return this.http.get<MembershipListItem[]>(`${this.constants.env.baseURL}/applications/${applicationId}/members`);
  }

  getAll(
    params: {
      environmentId?: string;
    } = {},
  ): Observable<any[]> {
    let baseURL = this.constants.env.baseURL;

    if (params.environmentId) {
      baseURL = `${this.constants.org.baseURL}/environments/${params.environmentId}`;
    }

    return this.http.get<any[]>(`${baseURL}/applications`, {
      params: {
        status: 'active',
        exclude: ['picture', 'owner'],
      },
    });
  }

  list(status?: string, query?: string, order?: string, page = 1, size = 10): Observable<PagedResult<Application>> {
    return this.http.get<PagedResult<Application>>(`${this.constants.env.baseURL}/applications/_paged`, {
      params: {
        page,
        size,
        ...(status ? { status } : {}),
        ...(query ? { query } : {}),
        ...(order ? { order } : {}),
      },
    });
  }

  findByIds(ids: string[], page = 1, size = 10): Observable<PagedResult<Application>> {
    let params = new HttpParams();
    params = params.append('page', page);
    params = params.append('size', size);

    if (ids?.length > 0) {
      params = params.appendAll({ ids });
    }
    return this.http.get<PagedResult<Application>>(`${this.constants.env.baseURL}/applications/_paged`, {
      params,
    });
  }

  restore(applicationId: string): Observable<Application> {
    return this.http.post<Application>(`${this.constants.env.baseURL}/applications/${applicationId}/_restore`, {});
  }

  getById(applicationId: string): Observable<Application> {
    return this.http.get<Application>(`${this.constants.env.baseURL}/applications/${applicationId}`).pipe(
      tap(application => {
        this.lastApplicationFetch$.next(application);
      }),
    );
  }

  getApplicationType(applicationId: string): Observable<ApplicationType> {
    return this.http.get<ApplicationType>(`${this.constants.env.baseURL}/applications/${applicationId}/configuration`);
  }

  update(application: Application): Observable<Application> {
    return this.http
      .put<Application>(`${this.constants.env.baseURL}/applications/${application.id}`, {
        name: application.name,
        description: application.description,
        domain: application.domain,
        groups: application.groups,
        settings: application.settings,
        ...(application.picture !== undefined ? { picture: application.picture } : {}),
        ...(application.background !== undefined ? { background: application.background } : {}),
        disable_membership_notifications: application.disable_membership_notifications,
        api_key_mode: application.api_key_mode,
      })
      .pipe(
        tap(application => {
          this.lastApplicationFetch$.next(application);
        }),
      );
  }

  getPermissions(applicationId: string): Observable<Record<string, ('C' | 'R' | 'U' | 'D')[]>> {
    return this.http.get<Record<string, ('C' | 'R' | 'U' | 'D')[]>>(
      `${this.constants.env.baseURL}/applications/${applicationId}/members/permissions`,
    );
  }

  getLastApplicationFetch(applicationId: string): Observable<Application> {
    return this.refreshLastApplicationFetch$.pipe(
      tap(() => {
        // Remove last fetch to force a new fetch
        this.lastApplicationFetch$.next(null);
      }),
      startWith({}),
      switchMap(() =>
        // If the last fetch is the same as the one we want, we return the last fetch
        // Otherwise, we fetch the application and return the last fetch
        this.lastApplicationFetch$.value && this.lastApplicationFetch$.value.id === applicationId
          ? this.lastApplicationFetch$
          : this.getById(applicationId).pipe(switchMap(() => this.lastApplicationFetch$)),
      ),
      filter(application => !!application),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }

  refreshLastApplicationFetch(): void {
    this.refreshLastApplicationFetch$.next();
  }

  getSubscribedAPIList(applicationId: string): Observable<SubscribedApi[]> {
    return this.http.get<SubscribedApi[]>(`${this.constants.env.baseURL}/applications/${applicationId}/subscribed`);
  }

  getSubscriptions(applicationId: string, expand?: string, statuses?: string[]): Observable<PagedResult<SubscriptionPage>> {
    return this.http.get<PagedResult<SubscriptionPage>>(`${this.constants.env.baseURL}/applications/${applicationId}/subscriptions`, {
      params: {
        ...(statuses && statuses.length > 0 ? { status: statuses.join(',') } : {}),
        ...(expand ? { expand: expand } : {}),
      },
    });
  }

  getSubscriptionsPage(
    applicationId: string,
    filters?: {
      status?: string[];
      apiKey?: string;
      apis?: string[];
      security_types?: string[];
    },
    page: number = 1,
    size: number = 20,
    expand: ('keys' | 'security')[] = [],
  ): Observable<PagedResult<SubscriptionPage>> {
    let params = new HttpParams().appendAll({ page, size });
    if (filters?.status?.length > 0) params = params.append('status', filters.status.join(','));
    if (filters?.apis?.length > 0) params = params.append('api', filters.apis.join(','));
    if (filters?.apiKey) params = params.append('api_key', filters.apiKey);
    if (filters?.security_types) params = params.append('security_types', filters.security_types.join(','));
    if (expand?.length > 0) params = params.append('expand', expand.join(','));

    return this.http.get<PagedResult<SubscriptionPage>>(`${this.constants.env.baseURL}/applications/${applicationId}/subscriptions`, {
      params,
    });
  }

  getLog(applicationId: string, logId: string, timestamp: string): Observable<ApplicationLog> {
    const timestampInUrl = timestamp ? '?timestamp=' + timestamp : '';
    return this.http.get<ApplicationLog>(`${this.constants.env.baseURL}/applications/${applicationId}/logs/${logId}${timestampInUrl}`);
  }

  delete(applicationId: string): Observable<Application> {
    return this.http.delete<Application>(`${this.constants.env.baseURL}/applications/${applicationId}/`);
  }

  create(application: CreateApplication): Observable<Application> {
    return this.http.post<Application>(`${this.constants.env.baseURL}/applications`, application);
  }

  getApiKeys(applicationId: string): Observable<ApplicationSubscriptionApiKey[]> {
    return this.http.get<ApplicationSubscriptionApiKey[]>(`${this.constants.env.baseURL}/applications/${applicationId}/apikeys`);
  }

  renewApiKey(applicationId: string): Observable<void> {
    return this.http.post<void>(`${this.constants.env.baseURL}/applications/${applicationId}/apikeys/_renew`, {});
  }

  revokeApiKey(applicationId: string, apiKeyId: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.env.baseURL}/applications/${applicationId}/apikeys/${apiKeyId}`);
  }
}
