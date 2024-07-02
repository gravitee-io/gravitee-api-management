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
import { Component } from '@angular/core';
import { GioBreadcrumbModule, GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { NgForOf, NgIf } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { GroupItem } from '../../settings/settings-navigation/settings-navigation.service';

@Component({
  selector: 'developer-portal-navigation',
  standalone: true,
  imports: [GioBreadcrumbModule, GioSubmenuModule, NgForOf, NgIf, RouterLinkActive, RouterOutlet, RouterLink],
  templateUrl: './developer-portal-navigation.component.html',
  styleUrl: './developer-portal-navigation.component.scss',
})
export class DeveloperPortalNavigationComponent {
  items: GroupItem[] = [
    {
      title: 'Developer Portal',
      items: [
        {
          displayName: 'Configuration',
          routerLink: './configuration',
        },
        {
          displayName: 'Customization',
          routerLink: './customization',
        },
        {
          displayName: 'User Management',
          routerLink: './user-management',
        },
        {
          displayName: 'Support',
          routerLink: './user-management',
        },
      ],
    },
  ];
}
