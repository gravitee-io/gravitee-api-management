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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { GioLicenseService, GioMenuSearchService, LicenseOptions, MenuSearchItem, SelectorItem } from '@gravitee/ui-particles-angular';
import { distinctUntilChanged, map, takeUntil } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../entities/Constants';
import { ApimFeature, UTMTags } from '../../shared/components/gio-license/gio-license-data';
import { Environment } from '../../entities/environment/environment';
import { cleanRouterLink } from '../../util/router-link.util';
import { EnvironmentSettingsService } from '../../services-ngx/environment-settings.service';

interface MenuItem {
  icon: string;
  routerLink?: string;
  displayName: string;
  permissions?: string[];
  licenseOptions?: LicenseOptions;
  iconRight$?: Observable<any>;
  subMenuPermissions?: string[];
  category: string;
}

export const SIDE_NAV_GROUP_ID = 'side-nav-items';

@Component({
  selector: 'gio-side-nav',
  templateUrl: './gio-side-nav.component.html',
  styleUrls: ['./gio-side-nav.component.scss'],
})
export class GioSideNavComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();

  public mainMenuItems: MenuItem[] = [];
  public footerMenuItems: MenuItem[] = [];

  public environments: SelectorItem[] = [];

  public currentEnv: Environment;
  public licenseExpirationDate$: Observable<Date>;
  public licenseExpirationNotificationEnabled = true;

  constructor(
    private readonly permissionService: GioPermissionService,
    @Inject(Constants) private readonly constants: Constants,
    private readonly gioLicenseService: GioLicenseService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly gioMenuSearchService: GioMenuSearchService,
    private readonly environmentSettingsService: EnvironmentSettingsService,
  ) {}

  ngOnInit(): void {
    this.activatedRoute.params
      .pipe(
        map((p) => p.envHrid),
        distinctUntilChanged(),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({
        next: (_) => {
          this.environments = this.constants.org.environments.map((env) => ({ value: env.id, displayValue: env.name }));
          this.currentEnv = this.constants.org.currentEnv;

          this.mainMenuItems = this.buildMainMenuItems();
          this.footerMenuItems = this.buildFooterMenuItems();
          this.gioMenuSearchService.removeMenuSearchItems([SIDE_NAV_GROUP_ID]);
          this.gioMenuSearchService.addMenuSearchItems(this.getSideNaveMenuSearchItems());
        },
      });

    if (this.constants.org.settings?.licenseExpirationNotification?.enabled !== undefined) {
      this.licenseExpirationNotificationEnabled = this.constants.org.settings.licenseExpirationNotification.enabled;
    }

    this.licenseExpirationDate$ = this.gioLicenseService.getExpiresAt$().pipe(distinctUntilChanged(), takeUntil(this.unsubscribe$));

    this.environmentSettingsService.get().subscribe((_) => {
      this.mainMenuItems = this.buildMainMenuItems();
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }

  changeCurrentEnv(envId: string): void {
    this.router.navigate(['/', envId]);
  }

  navigate(selectedItem: MenuSearchItem): void {
    this.router.navigate([selectedItem.routerLink]);
  }

  private buildMainMenuItems(): MenuItem[] {
    const auditLicenseOptions: LicenseOptions = {
      feature: ApimFeature.APIM_AUDIT_TRAIL,
      context: UTMTags.CONTEXT_ENVIRONMENT,
    };

    const auditIconRight$ = this.getMenuItemIconRight$(auditLicenseOptions);

    const alertEngineLicenseOptions: LicenseOptions = {
      feature: ApimFeature.ALERT_ENGINE,
      context: UTMTags.CONTEXT_ENVIRONMENT,
    };
    const alertEngineIconRight$ = this.getMenuItemIconRight$(alertEngineLicenseOptions);

    const mainMenuItems: MenuItem[] = [
      { icon: 'gio:home', routerLink: './home', displayName: 'Dashboard', category: 'home' },
      {
        icon: 'gio:cloud-settings',
        routerLink: './apis',
        displayName: 'APIs',
        category: 'Apis',
      },
      {
        icon: 'gio:box',
        routerLink: './integrations',
        displayName: 'Integrations',
        permissions: ['environment-integration-r'],
        category: 'Integrations',
      },
      {
        icon: 'gio:multi-window',
        routerLink: './applications',
        displayName: 'Applications',
        permissions: ['environment-application-r'],
        category: 'Applications',
      },
    ];

    if (!this.constants?.org?.settings?.cloud?.enabled) {
      mainMenuItems.push({
        icon: 'gio:cloud-server',
        displayName: 'Gateways',
        routerLink: './gateways',
        permissions: ['environment-instance-r'],
        category: 'Gateways',
      });
    }

    mainMenuItems.push({
      icon: 'gio:shield-check',
      routerLink: './api-score',
      displayName: 'API Score',
      permissions: ['environment-integration-r'],
      category: 'API Score',
    });

    mainMenuItems.push(
      {
        icon: 'gio:verified',
        displayName: 'Audit',
        routerLink: './audit',
        permissions: ['environment-audit-r'],
        licenseOptions: auditLicenseOptions,
        iconRight$: auditIconRight$,
        category: 'Audit',
      },
      {
        icon: 'gio:bar-chart-2',
        displayName: 'Analytics',
        routerLink: './analytics/dashboard',
        permissions: ['environment-platform-r'],
        category: 'Analytics',
      },
    );

    if (!this.constants.isOEM && this.constants.org.settings.alert && this.constants.org.settings.alert.enabled) {
      mainMenuItems.push({
        icon: 'gio:alarm',
        displayName: 'Alerts',
        routerLink: './alerts',
        permissions: ['environment-alert-r'],
        licenseOptions: alertEngineLicenseOptions,
        iconRight$: alertEngineIconRight$,
        category: 'Alerts',
      });
    }

    mainMenuItems.push({
      icon: 'gio:settings',
      routerLink: './settings',
      displayName: 'Settings',
      category: 'Environment',
      // prettier-ignore
      permissions: [
        // Portal
        'environment-dashboard-r',                    // Analytics
        'environment-api_header-r',                   // API Portal Information
        'environment-quality_rule-r',                 // API Quality
        'organization-identity_provider-r',           // Authentication
        'environment-identity_provider_activation-r', // Authentication
        'environment-category-r',                     // Categories
        'environment-client_registration_provider-r', // Client Registration
        'environment-documentation-c',                // Documentation
        'environment-documentation-u',                // Documentation
        'environment-documentation-d',                // Documentation
        'environment-metadata-r',                     // Metadata
        'environment-settings-r',                     // Settings
        'environment-theme-r',                        // Theme
        'environment-top_apis-r',                     // Top APIs
        // Gateway
        'organization-settings-r',                    // API Logging + FIXME should be moved to organization settings screen
        'environment-dictionary-r',                   // Dictionaries
        'environment-tag-c',                          // Sharding Tags
        'environment-tag-u',                          // Sharding Tags
        'environment-tag-d',                          // Sharding Tags
        'environment-tenant-c',                       // Tenants
        'environment-tenant-u',                       // Tenants
        'environment-tenant-d',                       // Tenants
        // User Management
        'organization-custom_user_fields-r',          // User Fields + FIXME should be moved to organization settings screen
        'environment-group-r',                        // Groups
        // Notifications
        'environment-notification-r',                 // Notifications
      ],
    });

    return this.filterMenuByPermission(mainMenuItems);
  }

  private getMenuItemIconRight$(licenseOptions: LicenseOptions) {
    return this.gioLicenseService.isMissingFeature$(licenseOptions.feature).pipe(map((notAllowed) => (notAllowed ? 'gio:lock' : null)));
  }

  private buildFooterMenuItems(): MenuItem[] {
    return this.filterMenuByPermission([
      {
        icon: 'gio:building',
        routerLink: '/_organization',
        displayName: 'Organization',
        permissions: ['organization-settings-r'],
        category: 'Organization',
      },
    ]);
  }

  private filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    return menuItems.filter((item) => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
  }

  private getSideNaveMenuSearchItems(): MenuSearchItem[] {
    return this.mainMenuItems
      .map((item) => {
        return {
          name: item.displayName,
          routerLink: `/${this.currentEnv.hrids}/${cleanRouterLink(item.routerLink)}`,
          category: item.category,
          groupIds: [SIDE_NAV_GROUP_ID],
        };
      })
      .concat(
        this.footerMenuItems.map((item) => ({
          name: item.displayName,
          routerLink: `/${cleanRouterLink(item.routerLink)}`,
          category: item.category,
          groupIds: [SIDE_NAV_GROUP_ID],
        })),
      );
  }
}
