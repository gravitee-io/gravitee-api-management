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
import { Component, computed, inject, input, InputSignal } from '@angular/core';
import { MatAnchor, MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { isEmpty } from 'lodash';

import { PortalNavigationItem } from '../../../entities/portal-navigation/portal-navigation-item';
import { User } from '../../../entities/user/user';
import { SignInService } from '../../../services/sign-in.service';
import { UserAvatarComponent } from '../../user-avatar/user-avatar.component';
import { NavBarButtonComponent } from '../nav-bar-button/nav-bar-button.component';

@Component({
  selector: 'app-desktop-nav-bar',
  templateUrl: './desktop-nav-bar.component.html',
  styleUrl: './desktop-nav-bar.component.scss',
  imports: [NavBarButtonComponent, UserAvatarComponent, MatAnchor, MatButton, MatIcon],
})
export class DesktopNavBarComponent {
  private readonly signInService = inject(SignInService);

  currentUser: InputSignal<User> = input({});
  topBarNavigationItems: InputSignal<PortalNavigationItem[]> = input<PortalNavigationItem[]>([]);
  analyticsEnabled: InputSignal<boolean> = input(false);
  protected isLoggedIn = computed(() => {
    return !isEmpty(this.currentUser());
  });

  protected signIn(): void {
    this.signInService.signIn();
  }
}
