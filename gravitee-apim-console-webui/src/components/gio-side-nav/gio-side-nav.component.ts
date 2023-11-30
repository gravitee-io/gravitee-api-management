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
import { GioLicenseService, LicenseOptions, SelectorItem } from '@gravitee/ui-particles-angular';
import { map } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';
import { ActivatedRoute, Router } from '@angular/router';

import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { Constants } from '../../entities/Constants';
import { ApimFeature, UTMTags } from '../../shared/components/gio-license/gio-license-data';
import { Environment } from '../../entities/environment/environment';

interface MenuItem {
  icon: string;
  // @Deprecated
  targetRoute?: string;
  routerLink?: string;
  // @Deprecated
  baseRoute?: string | string[];
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
export class GioSideNavComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();

  public mainMenuItems: MenuItem[] = [];
  public footerMenuItems: MenuItem[] = [];

  public environments: SelectorItem[] = [];

  public currentEnv: Environment;

  public readonly currentEnvironment: Environment;

  constructor(
    private readonly permissionService: GioPermissionService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly gioLicenseService: GioLicenseService,
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.environments = this.activatedRoute.snapshot.data.environmentResolver.environments.map((env) => ({
      value: env.id,
      displayValue: env.name,
    }));
    this.currentEnv = this.activatedRoute.snapshot.data.environmentResolver.currentEnvironment;

    this.mainMenuItems = this.buildMainMenuItems();
    this.footerMenuItems = this.buildFooterMenuItems();
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
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
      { icon: 'gio:home', routerLink: './home', baseRoute: 'home', displayName: 'Dashboard' },
      {
        icon: 'gio:upload-cloud',
        targetRoute: 'management.apis-list',
        routerLink: './apis',
        baseRoute: ['management.apis-list', 'management.apis', 'management.apis-new', 'management.apis-new-v2', 'management.apis-new-v4'],
        displayName: 'APIs',
      },
      {
        icon: 'gio:multi-window',
        routerLink: './applications',
        targetRoute: 'management.applications.list',
        baseRoute: 'management.applications',
        displayName: 'Applications',
        permissions: ['environment-application-r'],
      },
      {
        icon: 'gio:cloud-server',
        displayName: 'Gateways',
        routerLink: './gateways',
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
      routerLink: './settings',
      baseRoute: ['management.settings'],
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
        routerLink: '/_organization',
        baseRoute: 'organization',
        displayName: 'Organization',
        permissions: ['organization-settings-r'],
      },
    ]);
  }

  private filterMenuByPermission(menuItems: MenuItem[]): MenuItem[] {
    return menuItems.filter((item) => !item.permissions || this.permissionService.hasAnyMatching(item.permissions));
  }

  changeCurrentEnv($event: string): void {
    localStorage.setItem('gv-last-environment-loaded', $event);

    this.router.navigate(['/', $event]);
  }
}
