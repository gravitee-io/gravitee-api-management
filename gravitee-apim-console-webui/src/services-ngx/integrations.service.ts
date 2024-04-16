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

import { Inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import {
  CreateIntegrationPayload,
  Integration,
  IntegrationResponse,
  UpdateIntegrationPayload,
} from '../management/integrations/integrations.model';
import { Constants } from '../entities/Constants';

@Injectable({
  providedIn: 'root',
})
export class IntegrationsService {
  private url: string = `${this.constants.env.v2BaseURL}/integrations`;
  private currentIntegration$: BehaviorSubject<Integration> = new BehaviorSubject<Integration>(null);

  constructor(
    private readonly httpClient: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  public currentIntegration(): Observable<Integration> {
    return this.currentIntegration$.asObservable();
  }

  public resetCurrentIntegration(): void {
    this.currentIntegration$.next(null);
  }

  public getIntegrations(page, size): Observable<IntegrationResponse> {
    return this.httpClient.get<IntegrationResponse>(`${this.url}/?page=${page}&perPage=${size}`);
  }

  public getIntegration(id: string): Observable<Integration> {
    return this.httpClient.get<Integration>(`${this.url}/${id}`).pipe(tap((integration) => this.currentIntegration$.next(integration)));
  }

  public createIntegration(payload: CreateIntegrationPayload): Observable<Integration> {
    return this.httpClient.post<Integration>(this.url, payload);
  }

  public updateIntegration(payload: UpdateIntegrationPayload, id: string): Observable<Integration> {
    return this.httpClient.put<Integration>(`${this.url}/${id}`, payload);
  }

  public deleteIntegration(id: string): Observable<Integration> {
    return this.httpClient.delete<Integration>(`${this.url}/${id}`);
  }

  public ingestIntegration(id: string) {
    return this.httpClient.post(`${this.url}/${id}/_ingest`, null);
  }
}
