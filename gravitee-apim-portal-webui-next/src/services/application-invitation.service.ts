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
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import {
  ApplicationInvitation,
  ApplicationInvitationResendInput,
  ApplicationInvitationsCreateInput,
  ApplicationInvitationsResponse,
  ApplicationInvitationsSearchFilters,
  ApplicationInvitationUpdateInput,
} from '../entities/application/application-invitation';

@Injectable({
  providedIn: 'root',
})
export class ApplicationInvitationService {
  private readonly http = inject(HttpClient);
  private readonly configService = inject(ConfigService);

  searchApplicationInvitations(
    applicationId: string,
    page = 1,
    size = 10,
    filters: ApplicationInvitationsSearchFilters = {},
  ): Observable<ApplicationInvitationsResponse> {
    return this.http.post<ApplicationInvitationsResponse>(
      `${this.configService.baseURL}/applications/${applicationId}/invitations/_search`,
      { filters },
      {
        params: { page, size },
      },
    );
  }

  createApplicationInvitations(
    applicationId: string,
    input: ApplicationInvitationsCreateInput,
  ): Observable<ApplicationInvitationsResponse> {
    return this.http.post<ApplicationInvitationsResponse>(`${this.configService.baseURL}/applications/${applicationId}/invitations`, input);
  }

  deleteApplicationInvitation(applicationId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.configService.baseURL}/applications/${applicationId}/invitations/${invitationId}`);
  }

  updateApplicationInvitation(
    applicationId: string,
    invitationId: string,
    input: ApplicationInvitationUpdateInput,
  ): Observable<ApplicationInvitation> {
    return this.http.put<ApplicationInvitation>(
      `${this.configService.baseURL}/applications/${applicationId}/invitations/${invitationId}`,
      input,
    );
  }

  resendApplicationInvitation(applicationId: string, invitationId: string, input: ApplicationInvitationResendInput): Observable<void> {
    return this.http.post<void>(`${this.configService.baseURL}/applications/${applicationId}/invitations/${invitationId}/_resend`, input);
  }
}
