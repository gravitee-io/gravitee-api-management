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
import { Component, Inject, Input, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';

import { Constants } from '../../entities/Constants';
import { CurrentUserService, UIRouterState } from '../../ajs-upgraded-providers';
import UserService from '../../services/user.service';
import { User } from '../../entities/user/user';
import { TaskService } from '../../services-ngx/task.service';

@Component({
  selector: 'gio-user-menu',
  template: require('./gio-user-menu.component.html'),
  styles: [require('./gio-user-menu.component.scss')],
})
export class GioUserMenuComponent implements OnInit {
  @Input()
  public hasAlert = false;
  @Input()
  public userTaskCount = 0;

  public currentUser: User;
  public userShortName: string;
  public userPicture: string;
  public supportEnabled: boolean;
  public newsletterProposed: boolean;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject('Constants') public readonly constants: Constants,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    public readonly taskService: TaskService,
  ) {}

  ngOnInit(): void {
    this.currentUser = this.currentUserService.currentUser;
    this.newsletterProposed =
      (this.currentUser && !this.currentUser.firstLogin) ||
      !!window.localStorage.getItem('newsletterProposed') ||
      !this.constants.org.settings.newsletter.enabled;
    this.userShortName = this.getUserShortName();
    this.userPicture = this.currentUserService.currentUserPicture();
    this.supportEnabled = this.constants.org.settings.management.support.enabled;
  }

  goToMyAccount(): void {
    this.ajsState.go('user', {
      ...this.ajsState.params,
      environmentId: this.ajsState.params.environmentId ?? this.constants.org.currentEnv.id,
    });
  }

  goToSupport(): void {
    this.ajsState.go('management.support.create');
  }

  goToTask(): void {
    this.ajsState.go('management.tasks');
  }

  signOut(): void {
    this.ajsState.go('logout');
  }

  private getUserShortName = (): string => {
    if (this.currentUser.firstname && this.currentUser.lastname) {
      const capitalizedFirstName = this.currentUser.firstname[0].toUpperCase() + this.currentUser.firstname.slice(1);
      const shotLastName = this.currentUser.lastname[0].toUpperCase();
      return `${capitalizedFirstName} ${shotLastName}.`;
    }
    return this.currentUser.displayName;
  };
}
