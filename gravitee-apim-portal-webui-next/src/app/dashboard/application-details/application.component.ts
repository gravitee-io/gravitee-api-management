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
import { Component, effect, inject, input } from '@angular/core';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BreadcrumbService } from 'xng-breadcrumb';

import { Application } from '../../../entities/application/application';
import { CurrentUserService } from '../../../services/current-user.service';

@Component({
  selector: 'app-application',
  imports: [
    RouterOutlet,
    MatCard,
    MatCardContent,
    MatTabLink,
    MatTabNav,
    MatTabNavPanel,
    RouterLinkActive,
    RouterLink,
  ],
  templateUrl: './application.component.html',
  styleUrl: './application.component.scss',
})
export default class ApplicationComponent {
  application = input.required<Application>();
  isAuthenticated = inject(CurrentUserService).isUserAuthenticated;

  constructor(private breadcrumbService: BreadcrumbService) {
    effect(() => {
      this.breadcrumbService.set('@appName', this.application().name);
    });
  }
}
