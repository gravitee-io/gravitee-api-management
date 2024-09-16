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
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { SnackBarService } from './snack-bar.service';

import {
  CreateIntegrationPayload,
  DeletedFederatedAPIsResponse,
  FederatedAPIsResponse,
  AsyncJobStatus,
  Integration,
  IntegrationIngestionResponse,
  IntegrationPreview,
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

  public readonly bannerMessages = {
    techPreview: `This tech preview feature is new! We're gathering feedback on it to make it even better, so it may change as we make improvements.`,
    agentDisconnected: 'Check your agent status and ensure connectivity with the provider to start importing your APIs in Gravitee.',
  };

  constructor(
    private readonly httpClient: HttpClient,
    @Inject(Constants) private readonly constants: Constants,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ingest(integrationId: string, apiIdsToIngest: string[]): Observable<IntegrationIngestionResponse> {
    this.snackBarService.success('API ingestion is in progress. The process should only take a few minute to complete. Come back shortly!');

    return this.httpClient.post<IntegrationIngestionResponse>(`${this.url}/${integrationId}/_ingest`, { apiIds: apiIdsToIngest }).pipe(
      catchError((error) => {
        return of({
          status: AsyncJobStatus.ERROR,
          message: `Fail to ingest APIs: ${error.message}`,
        });
      }),
    );
  }

  public currentIntegration(): Observable<Integration> {
    return this.currentIntegration$.asObservable();
  }

  public resetCurrentIntegration(): void {
    this.currentIntegration$.next(null);
  }

  public getIntegrations(page: number, size: number): Observable<IntegrationResponse> {
    return this.httpClient.get<IntegrationResponse>(`${this.url}?page=${page}&perPage=${size}`);
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

  public previewIntegration(id: string): Observable<IntegrationPreview> {
    return this.httpClient.get<IntegrationPreview>(`${this.url}/${id}/_preview`);
  }

  public getFederatedAPIs(id: string, page: number = 1, size: number = 10): Observable<FederatedAPIsResponse> {
    return this.httpClient.get<FederatedAPIsResponse>(`${this.url}/${id}/apis?page=${page}&perPage=${size}`);
  }

  public deleteFederatedAPIs(id: string): Observable<DeletedFederatedAPIsResponse> {
    return this.httpClient.delete<DeletedFederatedAPIsResponse>(`${this.url}/${id}/apis`);
  }

  public getPermissions(id: string): Observable<Record<string, string>> {
    return this.httpClient.get<Record<string, string>>(`${this.url}/${id}/permissions`);
  }
}
