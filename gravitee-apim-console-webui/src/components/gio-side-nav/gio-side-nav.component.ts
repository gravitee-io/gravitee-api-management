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
import { StateParams, StateService } from '@uirouter/core';
import { GioLicenseService, LicenseOptions, SelectorItem } from '@gravitee/ui-particles-angular';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { castArray } from 'lodash';

import { CurrentUserService, PortalSettingsService, UIRouterState, UIRouterStateParams } from '../../ajs-upgraded-providers';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../entities/Constants';
import UserService from '../../services/user.service';
import PortalConfigService from '../../services/portalConfig.service';
import { ApimFeature, UTMTags } from '../../shared/components/gio-license/gio-license-data';

interface MenuItem {
  icon: string;
  targetRoute: string;
  baseRoute: string | string[];
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
  public currentEnvironmentId: string;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams: StateParams,
    private readonly permissionService: GioPermissionService,
    @Inject(PortalSettingsService) private readonly portalConfigService: PortalConfigService,
    @Inject(CurrentUserService) private readonly currentUserService: UserService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly gioLicenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    this.mainMenuItems = this.buildMainMenuItems();
    this.footerMenuItems = this.buildFooterMenuItems();
    this.environments = this.constants.org.environments.map((env) => ({ value: env.id, displayValue: env.name }));

    const currentEnvironment = this.constants.org.environments.find(
      (environment) =>
        environment.id === this.ajsStateParams.environmentId ||
        (environment.hrids && environment.hrids.includes(this.ajsStateParams.environmentId)),
    );
    this.currentEnvironmentId = currentEnvironment ? currentEnvironment.id : this.constants.org.currentEnv.id;

    // FIXME: to remove after migration. This allow to get the current environment when user "Go back to APIM" from organisation settings
    this.updateCurrentEnv();
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
      { icon: 'gio:home', targetRoute: 'home', baseRoute: 'home', displayName: 'Dashboard' },
      {
        icon: 'gio:upload-cloud',
        targetRoute: 'management.apis-list',
        baseRoute: ['management.apis-list', 'management.apis', 'management.apis-new', 'management.apis-new-v2', 'management.apis-new-v4'],
        displayName: 'APIs',
      },
      {
        icon: 'gio:multi-window',
        targetRoute: 'management.applications.list',
        baseRoute: 'management.applications',
        displayName: 'Applications',
        permissions: ['environment-application-r'],
      },
      {
        icon: 'gio:cloud-server',
        targetRoute: 'management.instances-list',
        baseRoute: ['management.instances-list', 'management.instances.detail'],
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

    if (!this.constants.isOEM && this.constants.org.settings.alert && this.constants.org.settings.alert.enabled) {
      mainMenuItems.push({
        icon: 'gio:alarm',
        displayName: 'Alerts',
        targetRoute: 'management.alerts.list',
        baseRoute: 'management.alerts',
        permissions: ['environment-alert-r'],
        licenseOptions: alertEngineLicenseOptions,
        iconRight$: alertEngineIconRight$,
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

  private getMenuItemIconRight$(licenseOptions: LicenseOptions) {
    return this.gioLicenseService.isMissingFeature$(licenseOptions).pipe(map((notAllowed) => (notAllowed ? 'gio:lock' : null)));
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
  }

  isActive(baseRoute: string | string[]): boolean {
    return castArray(baseRoute).some((baseRoute) => this.ajsState.includes(baseRoute));
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
