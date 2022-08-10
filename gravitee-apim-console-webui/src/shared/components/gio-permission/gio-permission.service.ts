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
import { intersection } from 'lodash';

import UserService from '../../../services/user.service';
import { CurrentUserService } from '../../../ajs-upgraded-providers';

@Injectable({ providedIn: 'root' })
export class GioPermissionService {
  constructor(@Inject(CurrentUserService) private readonly currentUserService: UserService) {}

  hasAnyMatching(permissions: string[]): boolean {
    if (!permissions || !this.currentUserService.currentUser.userPermissions) {
      return false;
    }
    return (
      intersection(this.currentUserService.currentUser.userPermissions, permissions).length > 0 ||
      intersection(this.currentUserService.currentUser.userEnvironmentPermissions, permissions).length > 0 ||
      intersection(this.currentUserService.currentUser.userApiPermissions, permissions).length > 0 ||
      intersection(this.currentUserService.currentUser.userApplicationPermissions, permissions).length > 0
    );
  }
}
