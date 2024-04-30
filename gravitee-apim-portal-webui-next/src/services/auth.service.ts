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

interface Token {
  token_type: string;
  token: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(
    private http: HttpClient,
    private configuration: ConfigService,
  ) {}

  login(username: string, password: string) {
    return this.http.post<Token>(
      `${this.configuration.baseURL}/auth/login`,
      {},
      {
        headers: {
          Authorization: `Basic ${btoa(username + ':' + password)}`,
        },
      },
    );
  }

  logout(): Observable<unknown> {
    return this.http.post<Token>(`${this.configuration.baseURL}/auth/logout`, {});
  }
}
