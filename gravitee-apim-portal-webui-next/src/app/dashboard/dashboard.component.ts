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
import { filter, map } from 'rxjs';

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
  private readonly router = inject(Router);
  menuItems = signal<MenuItem[]>(MENU_ITEMS);

  breadcrumbs = toSignal<Breadcrumb[]>(
    // TODO generalize and extract this logic to a shared service or smth similar
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      map(() => {
        const segments = this.router.url.split('/').filter(Boolean);
        const relevantSegments = segments.filter(s => s !== 'dashboard');
        const breadcrumbs: Breadcrumb[] = [];
        relevantSegments.forEach((segment, index) => {
          const isLast = index === relevantSegments.length - 1;
          const menuItem = MENU_ITEMS.find(mi => mi.path === segment);
          let label = '';
          let url;
          if (menuItem) {
            label = menuItem.title;
            if (!isLast) {
              url = `/dashboard/${menuItem.path}`;
            }
          } else if (index > 0 && relevantSegments[index - 1] === 'subscriptions') {
            const subscriptionId = segment;
            label = $localize`:@@subscriptionTitle:Subscription ` + subscriptionId;
          } else {
            label = segment; // Fallback
          }
          breadcrumbs.push({ id: segment, label, url });
        });
        return breadcrumbs;
      }),
    ),
  );
}
