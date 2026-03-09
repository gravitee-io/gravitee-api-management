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
import { Component, input, signal } from '@angular/core';

import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { Breadcrumb, BreadcrumbsComponent } from '../breadcrumbs/breadcrumbs.component';
import { SidenavToggleButtonComponent } from '../sidenav-toggle-button/sidenav-toggle-button.component';

@Component({
  selector: 'app-sidenav-layout',
  imports: [MobileClassDirective, SidenavToggleButtonComponent, BreadcrumbsComponent],
  standalone: true,
  templateUrl: './sidenav-layout.component.html',
  styleUrl: './sidenav-layout.component.scss',
})
export class SidenavLayoutComponent {
  breadcrumbs = input<Breadcrumb[]>([]);
  sidenavCollapsed = signal(false);

  onToggleSidenav() {
    this.sidenavCollapsed.update(val => !val);
  }

  onTriggerResponsiveBreakpoint(breakpoint: 'mobile' | null) {
    if ((breakpoint === null && this.sidenavCollapsed()) || (breakpoint !== null && !this.sidenavCollapsed())) {
      this.onToggleSidenav();
    }
  }
}
