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
import { Component, OnInit } from '@angular/core';

import { CurrentUserService } from '../../../services/current-user.service';
import { User, UserService } from '@gravitee/ng-portal-webclient';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-user-account',
  templateUrl: './user-account.component.html',
  styleUrls: ['./user-account.component.css']
})

export class UserAccountComponent implements OnInit {

  public currentUser: User;
  public avatar: string;

  constructor(
    private currentUserService: CurrentUserService,
    private userService: UserService,
    private notificationService: NotificationService,
  ) {
  }

  ngOnInit() {
    this.currentUserService.get().subscribe((user) => {
      this.currentUser = user;
      this.avatar = this.currentUser._links ? this.currentUser._links.avatar : null;
    });
  }

  onFileLoad(picture) {
    this.avatar = picture;
  }

  get displayName() {
    if (this.currentUser) {
      return this.currentUser.first_name ? `${this.currentUser.first_name} ${this.currentUser.last_name}` : this.currentUser.display_name;
    }
    return '';
  }

  get email() {
    if (this.currentUser) {
      return this.currentUser.email;
    }
    return '';
  }

  update() {
    if (this.avatar) {
      this.userService.updateCurrentUser({ UserInput: { id: this.currentUser.id, avatar: this.avatar } })
        .toPromise()
        .then((user) => {
          this.currentUserService.set(user);
          this.notificationService.success(i18n('user.account.success'));
          // @ts-ignore
          document.querySelector('gv-identity-picture').picture = this.avatar;
          delete this.avatar;
        });
    }
  }
}
