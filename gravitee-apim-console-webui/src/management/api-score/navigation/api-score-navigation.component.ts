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
import { GioSubmenuModule } from '@gravitee/ui-particles-angular';
import { MatIconModule } from '@angular/material/icon';
import { RouterLinkActive, RouterModule } from '@angular/router';

export interface NavigationItem {
  routerLink: string;
  displayName: string;
  permissions?: string[];
  icon?: string;
  routerLinkActiveOptions?: { exact: boolean };
  disabled?: boolean;
  testId?: string;
}

@Component({
  selector: 'app-api-score-navigation',
  standalone: true,
  imports: [RouterModule, GioSubmenuModule, MatIconModule, RouterLinkActive],
  templateUrl: './api-score-navigation.component.html',
  styleUrl: './api-score-navigation.component.scss',
})
export class ApiScoreNavigationComponent {
  public navigationItems: NavigationItem[] = [
    {
      routerLink: '.',
      displayName: 'Overview',
      icon: 'stat-up',
      routerLinkActiveOptions: { exact: true },
      testId: 'overview-nav-item',
    },
    {
      routerLink: 'rulesets',
      displayName: 'Rulesets',
      icon: 'shield-check',
      routerLinkActiveOptions: { exact: true },
      testId: 'rulesets-nav-item',
    },
  ];
}
