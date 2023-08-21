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
import { Component, Inject, OnInit } from '@angular/core';
import { StateService } from '@uirouter/core';
import { LicenseOptions, GioLicenseService, SelectorItem } from '@gravitee/ui-particles-angular';
import { IRootScopeService } from 'angular';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { AjsRootScope, CurrentUserService, PortalSettingsService, UIRouterState } from '../../ajs-upgraded-providers';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../entities/Constants';
import { EnvironmentService } from '../../services-ngx/environment.service';
import UserService from '../../services/user.service';
import PortalConfigService from '../../services/portalConfig.service';
import { ApimFeature, UTMTags } from '../../shared/components/gio-license/gio-license-data';

interface MenuItem {
  icon: string;
  targetRoute: string;
  baseRoute: string;
  displayName: string;
  permissions?: string[];
  licenseOptions?: LicenseOptions;
  iconRight$?: Observable<any>;
  subMenuPermissions?: string[];
}

@Component({
  selector: 'gio-side-nav',
  template: require('./gio-side-nav.component.html'),
  styles: [require('./gio-side-nav.component.scss')],
})
export class GioSideNavComponent implements OnInit {
  public mainMenuItems: MenuItem[] = [];
  public footerMenuItems: MenuItem[] = [];

  public environments: SelectorItem[] = [];

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(AjsRootScope) private readonly ajsRootScope: IRootScopeService,
    private readonly permissionService: GioPermissionService,
    private readonly environmentService: EnvironmentService,
    @Inject(PortalSettingsService) private readonly portalConfigService: PortalConfigService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly gioLicenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    this.mainMenuItems = this.buildMainMenuItems();
    this.footerMenuItems = this.buildFooterMenuItems();
    this.environments = this.constants.org.environments.map((env) => ({ value: env.id, displayValue: env.name }));

    // FIXME: to remove after migration. This allow to get the current environment when user "Go back to APIM" from organisation settings
    this.updateCurrentEnv();
  }

  private buildMainMenuItems(): MenuItem[] {
    const auditLicenseOptions: LicenseOptions = {
      feature: ApimFeature.APIM_AUDIT_TRAIL,
      context: UTMTags.CONTEXT_ENVIRONMENT,
    };
    const auditIconRight$ = this.gioLicenseService
      .isMissingFeature$(auditLicenseOptions)
      .pipe(map((notAllowed) => (notAllowed ? 'gio:lock' : null)));
    const mainMenuItems: MenuItem[] = [
      { icon: 'gio:home', targetRoute: 'management.dashboard.home', baseRoute: 'management.dashboard', displayName: 'Dashboard' },
      { icon: 'gio:upload-cloud', targetRoute: 'management.apis.ng-list', baseRoute: 'management.apis', displayName: 'APIs' },
      {
        icon: 'gio:multi-window',
        targetRoute: 'management.applications.list',
        baseRoute: 'management.applications',
        displayName: 'Applications',
        permissions: ['environment-application-r'],
      },
      {
        icon: 'gio:cloud-server',
        targetRoute: 'management.instances.list',
        baseRoute: 'management.instances',
        displayName: 'Gateways',
        permissions: ['environment-instance-r'],
      },
      {
        icon: 'gio:verified',
        targetRoute: 'management.audit',
        baseRoute: 'management.audit',
        displayName: 'Audit',
        permissions: ['environment-audit-r'],
        licenseOptions: auditLicenseOptions,
        iconRight$: auditIconRight$,
      },
      {
        icon: 'gio:message-text',
        targetRoute: 'management.messages',
        baseRoute: 'management.messages',
        displayName: 'Messages',
        permissions: ['environment-message-c'],
      },
      {
        icon: 'gio:bar-chart-2',
        targetRoute: 'management.analytics',
        baseRoute: 'management.analytics',
        displayName: 'Analytics',
        permissions: ['environment-platform-r'],
      },
    ];

    if (this.constants.org.settings.alert && this.constants.org.settings.alert.enabled) {
      mainMenuItems.push({
        icon: 'gio:alarm',
        displayName: 'Alerts',
        targetRoute: 'management.alerts.list',
        baseRoute: 'management.alerts',
        permissions: ['environment-alert-r'],
      });
    }

    mainMenuItems.push({
      icon: 'gio:settings',
      targetRoute: 'management.settings.analytics.list',
      baseRoute: 'management.settings',
      displayName: 'Settings',
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

  private buildFooterMenuItems(): MenuItem[] {
    return this.filterMenuByPermission([
      {
        icon: 'gio:building',
        targetRoute: 'organization.settings',
        baseRoute: 'organization',
        displayName: 'Organization',
        permissions: ['organization-settings-r'],
      },
    ]);
  }

  private filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    return menuItems.filter((item) => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
<<<<<<< HEAD
  }

  navigateTo(route: string) {
    this.ajsState.go(route);
=======
>>>>>>> 470ea5521c (fix: enable right click on menu items)
  }

  isActive(route: string): boolean {
    return this.ajsState.includes(route);
  }

  updateCurrentEnv(): void {
    this.portalConfigService.get().then((response) => {
      this.constants.env.settings = response.data;
    });
  }

  changeCurrentEnv($event: string): void {
    localStorage.setItem('gv-last-environment-loaded', $event);

    this.currentUserService.refreshEnvironmentPermissions().then(() => {
      this.ajsState.go('management', { environmentId: $event }, { reload: true });
    });
  }
}
