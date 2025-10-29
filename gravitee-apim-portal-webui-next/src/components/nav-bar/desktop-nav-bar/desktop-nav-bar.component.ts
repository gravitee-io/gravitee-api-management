/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Component, computed, input, InputSignal } from '@angular/core';
import { MatAnchor, MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { isEmpty } from 'lodash';

import { User } from '../../../entities/user/user';
import { PortalMenuLink } from '../../../services/portal-menu-links.service';
import { UserAvatarComponent } from '../../user-avatar/user-avatar.component';
import { NavBarButtonComponent } from '../nav-bar-button/nav-bar-button.component';

@Component({
  selector: 'app-desktop-nav-bar',
  templateUrl: './desktop-nav-bar.component.html',
  styleUrl: './desktop-nav-bar.component.scss',
  imports: [NavBarButtonComponent, UserAvatarComponent, MatAnchor, MatButton, RouterLink],
})
export class DesktopNavBarComponent {
  currentUser: InputSignal<User> = input({});
  forceLogin: InputSignal<boolean> = input(false);
  customLinks: InputSignal<PortalMenuLink[]> = input<PortalMenuLink[]>([]);
  protected isLoggedIn = computed(() => {
    return !isEmpty(this.currentUser());
  });
}
