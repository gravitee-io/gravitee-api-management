/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { NgTemplateOutlet } from '@angular/common';
import { Component, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

import { NavBarButtonComponent } from '../../components/nav-bar/nav-bar-button/nav-bar-button.component';
import { NavigationItemContentViewerComponent } from '../../components/navigation-item-content-viewer/navigation-item-content-viewer.component';
import { MobileClassDirective } from '../../directives/mobile-class.directive';
import { Breadcrumb, BreadcrumbsComponent } from '../documentation/components/documentation-folder/breadcrumb/breadcrumbs.component';
import { SidenavToggleButtonComponent } from '../documentation/components/documentation-folder/sidenav-toggle-button/sidenav-toggle-button.component';
import { TreeComponent } from '../documentation/components/documentation-folder/tree/tree.component';

@Component({
  selector: 'app-management',
  imports: [
    BreadcrumbsComponent,
    MobileClassDirective,
    NavigationItemContentViewerComponent,
    SidenavToggleButtonComponent,
    TreeComponent,
    MatButton,
    NavBarButtonComponent,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    NgTemplateOutlet,
  ],
  templateUrl: './management.component.html',
  styleUrl: './management.component.scss',
})
export class ManagementComponent {
  routes = signal<{ path: string; label: string }[]>([{ path: 'subscriptions', label: 'Subscriptions' }]);
  sidenavCollapsed = signal(false);
  breadcrumbs = toSignal<Breadcrumb[]>(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      startWith(null),
      map(() => this.getBreadcrumbsFromRoute()),
    ),
  );

  constructor(private readonly router: Router) {}

  onToggleSidenav() {
    this.sidenavCollapsed.set(!this.sidenavCollapsed());
  }

  onTriggerResponsiveBreakpoint(breakpoint: 'mobile' | null) {
    if ((breakpoint === null && this.sidenavCollapsed()) || (breakpoint !== null && !this.sidenavCollapsed())) {
      this.onToggleSidenav();
    }
  }

  getBreadcrumbsFromRoute(): Breadcrumb[] {
    const segments = this.router.url.split('/').filter(Boolean);
    const segment = segments[segments.length - 1];
    const label = this.routes().find(route => route.path === segment)?.label ?? '';
    return [{ id: segment, label }];
  }
}
