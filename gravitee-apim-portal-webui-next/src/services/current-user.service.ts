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
import { effect, Injectable, signal, WritableSignal } from '@angular/core';
import { isEmpty } from 'lodash';
import { catchError, Observable, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { ConfigService } from './config.service';
import { User } from '../entities/user/user';

@Injectable({
  providedIn: 'root',
})
export class CurrentUserService {
  public user: WritableSignal<User> = signal({});
  public isUserAuthenticated: WritableSignal<boolean> = signal(false);

  constructor(
    private http: HttpClient,
    private configuration: ConfigService,
  ) {
    effect(() => {
      this.isUserAuthenticated.set(!isEmpty(this.user()));
    });
  }

  public isAuthenticated(): boolean {
    return !isEmpty(this.user());
  }

  public loadUser(): Observable<unknown> {
    return this.http.get<User>(`${this.configuration.baseURL}/user`).pipe(
      tap(resp => {
        this.user.set(resp);
      }),
      catchError(_ => {
        this.user.set({});
        return of({});
      }),
    );
  }

  public clear(): void {
    this.user.set({});
  }
}
