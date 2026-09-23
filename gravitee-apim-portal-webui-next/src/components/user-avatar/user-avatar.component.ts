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
import { Component, DestroyRef, effect, inject, input, InputSignal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButton } from '@angular/material/button';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { RouterModule } from '@angular/router';
import { isEmpty } from 'lodash';
import { EMPTY, switchMap } from 'rxjs';

import { User } from '../../entities/user/user';
import { UserNotificationInboxService } from '../../services/user-notification-inbox.service';

@Component({
  selector: 'app-user-avatar',
  imports: [MatBadgeModule, MatButton, MatMenuTrigger, MatMenu, MatMenuItem, RouterModule],
  templateUrl: 'user-avatar.component.html',
  styleUrl: './user-avatar.component.scss',
})
export class UserAvatarComponent {
  private readonly userNotificationInboxService = inject(UserNotificationInboxService);
  private readonly destroyRef = inject(DestroyRef);

  user: InputSignal<User> = input({});
  analyticsEnabled: InputSignal<boolean> = input(false);
  initials: string = '';
  readonly unreadNotificationCount = this.userNotificationInboxService.unreadCount;

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

    toObservable(this.user)
      .pipe(
        switchMap(user => {
          if (isEmpty(user)) {
            this.userNotificationInboxService.clear();
            return EMPTY;
          }
          return this.userNotificationInboxService.fetchCount();
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  onMenuOpened(): void {
    if (!isEmpty(this.user())) {
      this.userNotificationInboxService.refresh();
    }
  }
}
