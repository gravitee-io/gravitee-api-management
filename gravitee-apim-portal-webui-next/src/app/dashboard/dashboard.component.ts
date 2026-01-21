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
import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButton } from '@angular/material/button';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';

import { Breadcrumb } from '../../components/breadcrumbs/breadcrumbs.component';
import { SidenavLayoutComponent } from '../../components/sidenav-layout/sidenav-layout.component';

interface MenuItem {
  path: string;
  title: string;
}

const MENU_ITEMS: MenuItem[] = [{ path: 'subscriptions', title: $localize`:@@subscriptionsTitle:Subscriptions` }];

@Component({
  selector: 'app-dashboard',
  imports: [SidenavLayoutComponent, MatButton, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  router = inject(Router);
  menuItems = signal<MenuItem[]>(MENU_ITEMS);

  breadcrumbs = toSignal<Breadcrumb[]>(
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      startWith(null),
      map(() => {
        const segments = this.router.url.split('/').filter(Boolean);
        const segment = segments[segments.length - 1];
        const label = MENU_ITEMS.find(mi => mi.path === segment)?.title ?? '';
        return [{ id: segment, label }];
      }),
    ),
  );
}
