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
import { MatTabLink, MatTabNav, MatTabNavPanel } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { Application } from '../../../entities/application/application';
import { BreadcrumbService } from '../../../services/breadcrumb.service';
import { applicationListBreadcrumb } from '../applications/application-breadcrumbs';

@Component({
  selector: 'app-application',
  imports: [RouterOutlet, MatTabLink, MatTabNav, MatTabNavPanel, RouterLinkActive, RouterLink],
  templateUrl: './application.component.html',
  styleUrl: './application.component.scss',
})
export default class ApplicationComponent {
  private readonly breadcrumbService = inject(BreadcrumbService);
  readonly application = input.required<Application>();

  constructor() {
    effect(() => {
      const application = this.application();
      this.breadcrumbService.set([applicationListBreadcrumb(true), { id: `application-${application.id}`, label: application.name }]);
    });
  }
}
