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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Constants } from '../entities/Constants';
import { NewToken, Token } from '../entities/user/userTokens';

@Injectable({
  providedIn: 'root',
})
export class UsersTokenService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  getTokens(userId: string): Observable<Token[]> {
    return this.http.get<Token[]>(`${this.constants.org.baseURL}/users/${userId}/tokens`);
  }

  createToken(userId: string, token: NewToken): Observable<Token> {
    return this.http.post<Token>(`${this.constants.org.baseURL}/users/${userId}/tokens`, token);
  }

  revokeToken(userId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.constants.org.baseURL}/users/${userId}/tokens/${id}`);
  }
}
