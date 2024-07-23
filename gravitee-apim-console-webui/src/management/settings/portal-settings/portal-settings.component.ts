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
import { Component, DestroyRef, OnInit, inject, Inject } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { combineLatest, Observable, of, Subject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { isEmpty } from 'lodash';

import { PortalSettingsService } from '../../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { PortalSettings } from '../../../entities/portal/portalSettings';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { CorsUtil } from '../../../shared/utils';
import { Constants } from '../../../entities/Constants';
import { IntegrationsService } from '../../../services-ngx/integrations.service';

interface PortalForm {
  company: FormGroup<{
    name: FormControl<string>;
  }>;
  security: FormGroup<{
    keyless: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    apikey: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    customApiKey: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    sharedApiKey: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    oauth2: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    jwt: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    push: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
  }>;
  api: FormGroup<{
    labelsDictionary: FormControl<string[]>;
    primaryOwnerMode: FormControl<string>;
  }>;
  dashboards: FormGroup<{
    apiStatus: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
  }>;
  portal: FormGroup<{
    apikeyHeader: FormControl<string>;
    url: FormControl<string>;
    homepageTitle: FormControl<string>;
    homepageTitleToggle: FormControl<string>;
    apis: FormGroup<{
      tilesMode: FormGroup<{
        enabled: FormControl<boolean>;
      }>;
    }>;
    support: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    rating: FormGroup<{
      enabled: FormControl<boolean>;
      comment: FormGroup<{
        mandatory: FormControl<boolean>;
      }>;
    }>;
    userCreation: FormGroup<{
      enabled: FormControl<boolean>;
      automaticValidation: FormGroup<{
        enabled: FormControl<boolean>;
      }>;
    }>;
    analytics: FormGroup<{
      enabled: FormControl<boolean>;
      trackingId: FormControl<string>;
    }>;
    uploadMedia: FormGroup<{
      enabled: FormControl<boolean>;
      maxSizeInOctet: FormControl<number>;
    }>;
  }>;
  portalNext: FormGroup<{
    access: FormGroup<{ enabled: FormControl<boolean> }>;
  }>;
  scheduler: FormGroup<{
    tasks: FormControl<number>;
    notifications: FormControl<string>;
  }>;
  documentation: FormGroup<{
    url: FormControl<string>;
  }>;
  openAPIDocViewer: FormGroup<{
    openAPIDocType: FormGroup<{
      swagger: FormGroup<{
        enabled: FormControl<boolean>;
      }>;
      redoc: FormGroup<{
        enabled: FormControl<boolean>;
      }>;
      defaultType: FormControl<string>;
    }>;
  }>;
  cors: FormGroup<{
    allowOrigin: FormControl<string[]>;
    allowMethods: FormControl<string[]>;
    allowHeaders: FormControl<string[]>;
    exposedHeaders: FormControl<string[]>;
    maxAge: FormControl<number>;
  }>;
  email: FormGroup<{
    enabled: FormControl<boolean>;
    host: FormControl<string>;
    port: FormControl<number>;
    username: FormControl<string>;
    password: FormControl<string>;
    protocol: FormControl<string>;
    subject: FormControl<string>;
    from: FormControl<string>;
    properties: FormGroup<{
      auth: FormControl<boolean>;
      startTlsEnable: FormControl<boolean>;
      sslTrust: FormControl<string>;
    }>;
  }>;
}

@Component({
  selector: 'portal-settings',
  templateUrl: './portal-settings.component.html',
  styleUrls: ['./portal-settings.component.scss'],
})
export class PortalSettingsComponent implements OnInit {
  settings: PortalSettings;
  portalForm: FormGroup<PortalForm>;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public formInitialValues: unknown;
  public defaultHttpHeaders = CorsUtil.defaultHttpHeaders.map((e) => e);
  public httpMethods = CorsUtil.httpMethods;
  public isLoadingData = true;
  private destroyRef = inject(DestroyRef);
  primaryOwnerModeList = [
    {
      id: 'HYBRID',
      label: 'HYBRID: an API primary owner can be either a user or a group (Default)',
    },
    {
      id: 'USER',
      label: 'USER: an API primary owner can only be a user',
    },
    {
      id: `GROUP`,
      label: 'GROUP: an API primary owner can only be a group',
    },
  ];
  hasEnterpriseLicense$: Observable<boolean> = of(false);
  portalUrl: string = undefined;
  environmentRootRouterLink: string;

  constructor(
    private readonly portalSettingsService: PortalSettingsService,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly licenseService: GioLicenseService,
    public readonly integrationsService: IntegrationsService,
    @Inject(Constants) public readonly constants: Constants,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;
    this.hasEnterpriseLicense$ = this.licenseService.getLicense$().pipe(map((license) => license.tier !== 'oss'));
    this.environmentRootRouterLink = '/' + this.constants.org.currentEnv.id;

    combineLatest([this.portalSettingsService.get()])
      .pipe(
        tap(([portalSettings]) => {
          this.settings = portalSettings;
          this.initialPortalForm();
          const isPortalNextEnabled = portalSettings?.portalNext?.access?.enabled;
          this.portalUrl = isEmpty(portalSettings.portal.url)
            ? undefined
            : this.constants.env.baseURL.replace('{:envId}', this.constants.org.currentEnv.id) +
              '/portal/redirect' +
              (isPortalNextEnabled ? '?version=next' : '');
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  initialPortalForm() {
    this.portalForm = new FormGroup<PortalForm>({
      company: new FormGroup({
        name: new FormControl({
          value: this.settings.company.name,
          disabled: this.isReadonly('company.name'),
        }),
      }),
      security: new FormGroup({
        keyless: new FormGroup({
          enabled: new FormControl({
            value: this.settings.plan.security.keyless.enabled,
            disabled: this.isReadonly('plan.security.keyless.enabled'),
          }),
        }),
        apikey: new FormGroup({
          enabled: new FormControl({
            value: this.settings.plan.security.apikey.enabled,
            disabled: this.isReadonly('plan.security.apikey.enabled'),
          }),
        }),
        customApiKey: new FormGroup({
          enabled: new FormControl({
            value: this.settings.plan.security.customApiKey.enabled,
            disabled: this.isReadonly('plan.security.apikey.allowCustom.enabled') || !this.settings.plan.security.apikey.enabled,
          }),
        }),
        sharedApiKey: new FormGroup({
          enabled: new FormControl({
            value: this.settings.plan.security.sharedApiKey.enabled,
            disabled: this.isReadonly('plan.security.apikey.sharedApiKey.enabled') || !this.settings.plan.security.apikey.enabled,
          }),
        }),
        oauth2: new FormGroup({
          enabled: new FormControl({
            value: this.settings.plan.security.oauth2.enabled,
            disabled: this.isReadonly('plan.security.oauth2.enabled'),
          }),
        }),
        jwt: new FormGroup({
          enabled: new FormControl({
            value: this.settings.plan.security.jwt.enabled,
            disabled: this.isReadonly('plan.security.jwt.enabled'),
          }),
        }),
        push: new FormGroup({
          enabled: new FormControl({
            value: this.settings.plan.security.push.enabled,
            disabled: this.isReadonly('plan.security.push.enabled'),
          }),
        }),
      }),
      api: new FormGroup({
        labelsDictionary: new FormControl({
          value: this.settings.api.labelsDictionary,
          disabled: this.isReadonly('api.labelsDictionary'),
        }),
        primaryOwnerMode: new FormControl({
          value: this.settings.api.primaryOwnerMode,
          disabled: this.isReadonly('api.primaryOwnerMode'),
        }),
      }),
      dashboards: new FormGroup({
        apiStatus: new FormGroup({
          enabled: new FormControl({
            value: this.settings.dashboards.apiStatus.enabled,
            disabled: this.isReadonly('console.dashboards.apiStatus.enabled'),
          }),
        }),
      }),
      portal: new FormGroup({
        apikeyHeader: new FormControl({
          value: this.settings.portal.apikeyHeader,
          disabled: this.isReadonly('portal.apikey.header'),
        }),
        url: new FormControl({
          value: this.settings.portal.url,
          disabled: this.isReadonly('portal.url'),
        }),
        homepageTitle: new FormControl({
          value: this.settings.portal.homepageTitle,
          disabled: this.isReadonly('portal.homepageTitle'),
        }),
        homepageTitleToggle: new FormControl(this.settings.portal.homepageTitle),
        apis: new FormGroup({
          tilesMode: new FormGroup({
            enabled: new FormControl({
              value: this.settings.portal.apis.tilesMode.enabled,
              disabled: this.isReadonly('portal.apis.tilesMode.enabled'),
            }),
          }),
        }),
        support: new FormGroup({
          enabled: new FormControl({
            value: this.settings.portal.support.enabled,
            disabled: this.isReadonly('portal.support.enabled'),
          }),
        }),
        rating: new FormGroup({
          enabled: new FormControl({
            value: this.settings.portal.rating.enabled,
            disabled: this.isReadonly('portal.rating.enabled'),
          }),
          comment: new FormGroup({
            mandatory: new FormControl({
              value: this.settings.portal.rating.comment.mandatory,
              disabled: this.isReadonly('portal.rating.comment.mandatory'),
            }),
          }),
        }),
        userCreation: new FormGroup({
          enabled: new FormControl({
            value: this.settings.portal.userCreation.enabled,
            disabled: this.isReadonly('portal.userCreation.enabled'),
          }),
          automaticValidation: new FormGroup({
            enabled: new FormControl({
              value: this.settings.portal.userCreation.automaticValidation.enabled,
              disabled: this.isReadonly('portal.userCreation.automaticValidation.enabled') || !this.settings.portal.userCreation.enabled,
            }),
          }),
        }),
        analytics: new FormGroup({
          enabled: new FormControl({
            value: this.settings.portal.analytics.enabled,
            disabled: this.isReadonly('portal.analytics.enabled'),
          }),
          trackingId: new FormControl({
            value: this.settings.portal.analytics.trackingId,
            disabled: this.isReadonly('portal.analytics.trackingId'),
          }),
        }),
        uploadMedia: new FormGroup({
          enabled: new FormControl({
            value: this.settings.portal.uploadMedia.enabled,
            disabled: this.isReadonly('portal.uploadMedia.enabled'),
          }),
          maxSizeInOctet: new FormControl({
            value: this.settings.portal.uploadMedia.maxSizeInOctet,
            disabled: this.isReadonly('portal.uploadMedia.maxSizeInOctet'),
          }),
        }),
      }),
      portalNext: new FormGroup({
        access: new FormGroup({
          enabled: new FormControl({
            value: !!this.settings.portalNext?.access?.enabled,
            disabled: this.isReadonly('portalNext.access.enabled'),
          }),
        }),
      }),
      scheduler: new FormGroup({
        tasks: new FormControl({
          value: this.settings.scheduler.tasks,
          disabled: this.isReadonly('portal.scheduler.tasks'),
        }),
        notifications: new FormControl({
          value: this.settings.scheduler.notifications,
          disabled: this.isReadonly('portal.scheduler.notifications'),
        }),
      }),
      documentation: new FormGroup({
        url: new FormControl({
          value: this.settings.documentation.url,
          disabled: this.isReadonly('documentation.url'),
        }),
      }),
      openAPIDocViewer: new FormGroup({
        openAPIDocType: new FormGroup({
          swagger: new FormGroup({
            enabled: new FormControl({
              value: this.settings.openAPIDocViewer.openAPIDocType.swagger.enabled,
              disabled:
                this.isReadonly('open.api.doc.type.swagger.enabled') || !this.settings.openAPIDocViewer.openAPIDocType.redoc.enabled,
            }),
          }),
          redoc: new FormGroup({
            enabled: new FormControl({
              value: this.settings.openAPIDocViewer.openAPIDocType.redoc.enabled,
              disabled:
                this.isReadonly('open.api.doc.type.redoc.enabled') || !this.settings.openAPIDocViewer.openAPIDocType.swagger.enabled,
            }),
          }),
          defaultType: new FormControl({
            value: this.settings.openAPIDocViewer.openAPIDocType.defaultType,
            disabled: this.isReadonly('open.api.doc.type.default'),
          }),
        }),
      }),
      cors: new FormGroup({
        allowOrigin: new FormControl({
          value: this.settings.cors.allowOrigin,
          disabled: this.isReadonly('http.api.portal.cors.allow-origin'),
        }),
        allowMethods: new FormControl({
          value: this.settings.cors.allowMethods,
          disabled: this.isReadonly('http.api.portal.cors.allow-methods'),
        }),
        allowHeaders: new FormControl({
          value: this.settings.cors.allowHeaders,
          disabled: this.isReadonly('http.api.portal.cors.allow-headers'),
        }),
        exposedHeaders: new FormControl({
          value: this.settings.cors.exposedHeaders,
          disabled: this.isReadonly('http.api.portal.cors.exposed-headers'),
        }),
        maxAge: new FormControl({
          value: this.settings.cors.maxAge,
          disabled: this.isReadonly('http.api.portal.cors.maxAge'),
        }),
      }),
      email: new FormGroup({
        enabled: new FormControl({
          value: this.settings.email.enabled,
          disabled: this.isReadonly('email.enabled'),
        }),
        host: new FormControl({
          value: this.settings.email.host,
          disabled: this.isReadonly('email.host'),
        }),
        port: new FormControl({
          value: this.settings.email.port,
          disabled: this.isReadonly('email.port'),
        }),
        username: new FormControl({
          value: this.settings.email.username,
          disabled: this.isReadonly('email.username'),
        }),
        password: new FormControl({
          value: this.settings.email.password,
          disabled: this.isReadonly('email.password') || !this.settings.email.enabled,
        }),
        protocol: new FormControl({
          value: this.settings.email.protocol,
          disabled: this.isReadonly('email.protocol') || !this.settings.email.enabled,
        }),
        subject: new FormControl({
          value: this.settings.email.subject,
          disabled: this.isReadonly('email.subject') || !this.settings.email.enabled,
        }),
        from: new FormControl({
          value: this.settings.email.from,
          disabled: this.isReadonly('email.from') || !this.settings.email.enabled,
        }),
        properties: new FormGroup({
          auth: new FormControl({
            value: this.settings.email.properties.auth,
            disabled: this.isReadonly('email.properties.auth') || !this.settings.email.enabled,
          }),
          startTlsEnable: new FormControl({
            value: this.settings.email.properties.startTlsEnable,
            disabled: this.isReadonly('email.properties.starttls.enable') || !this.settings.email.enabled,
          }),
          sslTrust: new FormControl({
            value: this.settings.email.properties.sslTrust,
            disabled: this.isReadonly('email.properties.ssl.trust') || !this.settings.email.enabled,
          }),
        }),
      }),
    });

    if (!this.permissionService.hasAnyMatching(['environment-settings-u'])) {
      this.portalForm.disable();
    }

    this.formInitialValues = this.portalForm.getRawValue();

    this.portalForm
      .get('security.apikey.enabled')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((selectedValue) => {
        if (!selectedValue) {
          this.portalForm.get('security.customApiKey.enabled').setValue(false);
          this.portalForm.get('security.sharedApiKey.enabled').setValue(false);
        }
        if (selectedValue) {
          this.portalForm.get('security.customApiKey.enabled').enable();
          this.portalForm.get('security.sharedApiKey.enabled').enable();
        }
      });

    this.portalForm
      .get('portal.homepageTitleToggle')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((selectedValue) => {
        if (!selectedValue) {
          this.portalForm.get('portal.homepageTitle').setValue(null);
        }
      });

    this.portalForm
      .get('portal.userCreation.enabled')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((selectedValue) => {
        if (!selectedValue) {
          this.portalForm.get('portal.userCreation.automaticValidation.enabled').disable();
        }
        if (selectedValue) {
          this.portalForm.get('portal.userCreation.automaticValidation.enabled').enable();
        }
      });

    this.portalForm
      .get('openAPIDocViewer.openAPIDocType.redoc.enabled')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((selectedValue) => {
        if (!selectedValue) {
          this.portalForm.get('openAPIDocViewer.openAPIDocType.defaultType').setValue('Swagger');
          this.portalForm.get('openAPIDocViewer.openAPIDocType.swagger.enabled').disable();
        }
        if (selectedValue) {
          this.portalForm.get('openAPIDocViewer.openAPIDocType.swagger.enabled').enable();
        }
      });

    this.portalForm
      .get('openAPIDocViewer.openAPIDocType.swagger.enabled')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((selectedValue) => {
        if (!selectedValue) {
          this.portalForm.get('openAPIDocViewer.openAPIDocType.defaultType').setValue('Redoc');
          this.portalForm.get('openAPIDocViewer.openAPIDocType.redoc.enabled').disable();
        }
        if (selectedValue) {
          this.portalForm.get('openAPIDocViewer.openAPIDocType.redoc.enabled').enable();
        }
      });
  }

  onSubmit() {
    delete this.portalForm.value.portal.homepageTitleToggle;

    const updatedSettingsPayload = {
      ...this.settings,
      company: {
        ...this.settings.company,
        ...this.portalForm.get('company').value,
      },
      plan: {
        security: {
          ...this.settings.plan.security,
          ...this.portalForm.get('security').value,
        },
      },
      api: {
        ...this.settings.api,
        ...this.portalForm.get('api').value,
      },
      dashboards: {
        ...this.settings.dashboards,
        ...this.portalForm.get('dashboards').value,
      },
      portal: {
        ...this.settings.portal,
        ...this.portalForm.get('portal').value,
        apis: {
          ...this.settings.portal.apis,
          ...this.portalForm.get('portal.apis').value,
        },
      },
      scheduler: {
        ...this.settings.scheduler,
        ...this.portalForm.get('scheduler').value,
      },
      documentation: {
        ...this.settings.documentation,
        ...this.portalForm.get('documentation').value,
      },
      openAPIDocViewer: {
        ...this.settings.openAPIDocViewer,
        ...this.portalForm.get('openAPIDocViewer').value,
        openAPIDocType: {
          ...this.settings.openAPIDocViewer.openAPIDocType,
          ...this.portalForm.get('openAPIDocViewer.openAPIDocType').value,
        },
      },
      cors: {
        ...this.settings.cors,
        ...this.portalForm.get('cors').value,
      },
      email: {
        ...this.settings.email,
        ...this.portalForm.get('email').value,
        properties: {
          ...this.settings.email.properties,
          ...this.portalForm.get('email.properties').value,
        },
      },
      portalNext: {
        ...this.settings.portalNext,
        ...this.portalForm.get('portalNext').value,
        access: {
          ...this.settings.portalNext.access,
          ...this.portalForm.get('portalNext.access').value,
        },
      },
    };

    this.portalSettingsService
      .save(updatedSettingsPayload)
      .pipe(
        tap(() => this.snackBarService.success('Settings successfully updated!')),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.ngOnInit());
  }

  isReadonly(property: string): boolean {
    return PortalSettingsService.isReadonly(this.settings, property);
  }
}
