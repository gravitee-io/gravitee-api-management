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
import { Component, effect, input, InputSignal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { RouterModule } from '@angular/router';

import { User } from '../../entities/user/user';

@Component({
  selector: 'app-user-avatar',
  imports: [MatButton, MatMenuTrigger, MatMenu, MatMenuItem, RouterModule],
  templateUrl: 'user-avatar.component.html',
  styleUrl: './user-avatar.component.scss',
})
export class UserAvatarComponent {
  user: InputSignal<User> = input({});
  initials: string = '';

  constructor() {
    effect(() => {
      if (!!this.user().first_name || !!this.user().last_name) {
        const firstName = this.user().first_name ?? '';
        const lastName = this.user().last_name ?? '';
        this.initials = `${firstName.length ? firstName[0] : ''}${lastName.length ? lastName[0] : ''}`;
      } else {
        this.initials = this.user().display_name?.[0] ?? '';
      }
    });
  }
}
