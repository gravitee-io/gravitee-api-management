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
import { Component, computed, inject, input } from '@angular/core';
import { rxResource, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { GioBannerModule } from '@gravitee/ui-particles-angular';
import { map, of } from 'rxjs';

import { EnvironmentSettingsService } from '../../../services-ngx/environment-settings.service';
import { GioPermissionService } from '../gio-permission/gio-permission.service';
import { PortalNavigationItemService } from '../../../services-ngx/portal-navigation-item.service';
import { PortalNavigationApi } from '../../../entities/management-api-v2';

@Component({
  selector: 'classic-portal-only-banner',
  standalone: true,
  imports: [GioBannerModule, RouterLink, MatButtonModule],
  templateUrl: './classic-portal-only-banner.component.html',
  styleUrl: './classic-portal-only-banner.component.scss',
})
export class ClassicPortalOnlyBannerComponent {
  private readonly environmentSettingsService = inject(EnvironmentSettingsService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly portalNavigationItemService = inject(PortalNavigationItemService);

  title = input('Classic Developer Portal Only');
  body = input('');
  actionLabel = input('Next Gen Portal Settings');

  protected readonly canShowSettingsAction: boolean = this.permissionService.hasAnyMatching([
    'environment-settings-r',
    'environment-settings-u',
  ]);
  protected readonly settingsRouterLink: string[] | null = (() => {
    const envHrid = this.activatedRoute.snapshot.params['envHrid'];
    return envHrid ? ['/', envHrid, '_portal', 'navigation'] : null;
  })();
  private readonly apiId: string | null = this.activatedRoute.snapshot.params['apiId'] ?? null;

  protected readonly isPortalNextEnabled = toSignal(this.environmentSettingsService.isPortalNextEnabled(), { initialValue: false });

  protected readonly navItemResource = rxResource({
    params: () => (this.isPortalNextEnabled() && this.apiId ? this.apiId : null),
    stream: ({ params: apiId }) =>
      apiId
        ? this.portalNavigationItemService.getNavigationItems('TOP_NAVBAR').pipe(
            map(({ items }) => {
              const matchingItem = items.find((i): i is PortalNavigationApi => i.type === 'API' && i.apiId === apiId);
              return matchingItem?.id ?? null;
            }),
          )
        : of(null),
  });

  protected readonly settingsRouterLinkQueryParams = computed(() => {
    const navId = this.navItemResource.value();
    return navId ? { navId } : null;
  });
}
