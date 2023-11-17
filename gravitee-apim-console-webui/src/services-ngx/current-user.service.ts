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
import { shareReplay } from 'rxjs/operators';

import { Constants } from '../entities/Constants';
import { User } from '../entities/user/user';
import { hashStringToCode } from '../services/string.service';

@Injectable({
  providedIn: 'root',
})
export class CurrentUserService {
  private currentUser: Observable<User> | null;

  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  getTags(): Observable<string[]> {
    return this.http.get<string[]>(`${this.constants.org.baseURL}/user/tags`);
  }

  current(): Observable<User> {
    if (!this.currentUser) {
      return this.http.get<User>(`${this.constants.org.baseURL}/user`).pipe(shareReplay(1));
    }
    return this.currentUser;
  }

  getUserPictureUrl(user: User): string {
    if (user && user.id) {
      return `${this.constants.org.baseURL}/user/avatar?${hashStringToCode(user.id)}`;
    }
  }

  clearCurrent() {
    this.currentUser = null;
  }
}
