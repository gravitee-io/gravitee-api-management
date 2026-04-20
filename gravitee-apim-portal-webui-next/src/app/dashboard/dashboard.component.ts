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
import { Component, computed, DestroyRef, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { SidenavLayoutComponent } from '../../components/sidenav-layout/sidenav-layout.component';
import { BreadcrumbService } from '../../services/breadcrumb.service';
import { ConfigService } from '../../services/config.service';

interface MenuItem {
  path: string;
  title: string;
}

const MENU_ITEMS: MenuItem[] = [
  { path: 'analytics', title: $localize`:@@analyticsTitle:Analytics` },
  { path: 'applications', title: $localize`:@@applicationsTitle:Applications` },
  { path: 'subscriptions', title: $localize`:@@subscriptionsTitle:Subscriptions` },
];

@Component({
  selector: 'app-dashboard',
  imports: [SidenavLayoutComponent, MatButton, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly configService = inject(ConfigService);
  readonly breadcrumbService = inject(BreadcrumbService);

  readonly menuItems = computed(() => {
    const analyticsEnabled = this.configService.configuration.portalNext?.analytics?.enabled ?? false;
    return analyticsEnabled ? MENU_ITEMS : MENU_ITEMS.filter(item => item.path !== 'analytics');
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.breadcrumbService.clear());
  }
}
