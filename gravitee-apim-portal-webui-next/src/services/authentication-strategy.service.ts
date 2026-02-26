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

export interface AuthenticationStrategy {
  id: string;
  name: string;
  display_name?: string;
  description?: string;
  type: 'KEY_AUTH' | 'DCR' | 'SELF_MANAGED_OIDC';
  client_registration_provider_id?: string;
  scopes?: string[];
  auth_methods?: string[];
  credential_claims?: string;
  auto_approve?: boolean;
  hide_credentials?: boolean;
  created_at?: number;
  updated_at?: number;
}

@Injectable({
  providedIn: 'root',
})
export class AuthenticationStrategyService {
  constructor(
    private readonly http: HttpClient,
    private configService: ConfigService,
  ) {}

  list(): Observable<AuthenticationStrategy[]> {
    return this.http.get<AuthenticationStrategy[]>(
      `${this.configService.baseURL}/configuration/applications/authentication-strategies`,
    );
  }
}
