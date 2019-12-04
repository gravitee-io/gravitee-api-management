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
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { User, UserService } from '@gravitee/ng-portal-webclient';

@Injectable({ providedIn: 'root' })
export class CurrentUserService {
  private readonly currentUserSource: BehaviorSubject<User>;

  constructor() {
    this.currentUserSource = new BehaviorSubject<User>(null);
  }

  changeUser(currentUser: Observable<User>) {
    currentUser.subscribe((user) => {
      if (this.currentUserSource.getValue() !== user) {
        this.currentUserSource.next(user);
      }
    });
  }

  revokeUser() {
    this.currentUserSource.next(null);
  }

  get(): BehaviorSubject<User> {
    return this.currentUserSource;
  }
}
