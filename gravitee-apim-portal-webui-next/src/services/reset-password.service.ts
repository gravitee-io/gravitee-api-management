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

@Injectable({
  providedIn: 'root',
})
export class ResetPasswordService {
  constructor(
    private readonly http: HttpClient,
    private readonly configuration: ConfigService,
  ) {}

  resetPassword(username: string, reset_page_url: string): Observable<void> {
    return this.http.post<void>(`${this.configuration.baseURL}/users/_reset_password`, { username, reset_page_url });
  }

  confirmResetPassword(firstname: string, lastname: string, password: string, token: string | null): Observable<void> {
    return this.http.post<void>(`${this.configuration.baseURL}/users/_change_password`, {
      firstname,
      lastname,
      password,
      token,
    });
  }
}
