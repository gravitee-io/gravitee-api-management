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
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConfigService } from './config.service';
import {
  ClientCertificate,
  ClientCertificatesResponse,
  CreateClientCertificateInput,
  UpdateClientCertificateInput,
} from '../entities/application/client-certificate';

@Injectable({
  providedIn: 'root',
})
export class ApplicationCertificateService {
  constructor(
    private readonly http: HttpClient,
    private readonly configService: ConfigService,
  ) {}

  list(applicationId: string, page = 1, size = 10): Observable<ClientCertificatesResponse> {
    return this.http.get<ClientCertificatesResponse>(
      `${this.configService.baseURL}/applications/${applicationId}/certificates?page=${page}&size=${size}`,
    );
  }

  create(applicationId: string, body: CreateClientCertificateInput): Observable<ClientCertificate> {
    return this.http.post<ClientCertificate>(`${this.configService.baseURL}/applications/${applicationId}/certificates`, body);
  }

  update(applicationId: string, certId: string, body: UpdateClientCertificateInput): Observable<ClientCertificate> {
    return this.http.put<ClientCertificate>(`${this.configService.baseURL}/applications/${applicationId}/certificates/${certId}`, body);
  }

  delete(applicationId: string, certId: string): Observable<void> {
    return this.http.delete<void>(`${this.configService.baseURL}/applications/${applicationId}/certificates/${certId}`);
  }
}
