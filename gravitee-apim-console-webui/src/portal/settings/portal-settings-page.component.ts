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
import { AsyncPipe } from '@angular/common';
import { Component, DestroyRef, effect, HostListener, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { rxResource, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  GioBannerModule,
  GioFormSlideToggleModule,
  GioLicenseService,
  GioLoaderModule,
  GioSaveBarModule,
} from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';
import { EMPTY } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

import { PortalHeaderComponent } from '../components/header/portal-header.component';
import { PortalSettings, PortalSettingsOpenAPIDocViewer } from '../../entities/portal/portalSettings';
import { PortalSettingsService } from '../../services-ngx/portal-settings.service';
import { SnackBarService } from '../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';
import { HasUnsavedChanges } from '../../shared/guards/has-unsaved-changes.guard';

type OpenApiViewer = PortalSettingsOpenAPIDocViewer['openAPIDocType']['defaultType'];

interface PortalSettingsPageForm {
  portal: FormGroup<{
    apikeyHeader: FormControl<string>;
    kafkaSaslMechanisms: FormControl<string[]>;
    url: FormControl<string>;
    userCreation: FormGroup<{
      enabled: FormControl<boolean>;
      automaticValidation: FormGroup<{
        enabled: FormControl<boolean>;
      }>;
    }>;
  }>;
  openAPIDocViewer: FormGroup<{
    openAPIDocType: FormGroup<{
      defaultType: FormControl<OpenApiViewer>;
    }>;
  }>;
  portalNext: FormGroup<{
    mtls: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    analytics: FormGroup<{
      enabled: FormControl<boolean>;
    }>;
    catalog: FormGroup<{
      fuzzySearch: FormGroup<{
        enabled: FormControl<boolean>;
      }>;
    }>;
  }>;
}

type PortalSettingsPageFormValue = ReturnType<FormGroup<PortalSettingsPageForm>['getRawValue']>;

@Component({
  selector: 'portal-settings-page',
  imports: [
    AsyncPipe,
    GioBannerModule,
    GioFormSlideToggleModule,
    GioLoaderModule,
    GioSaveBarModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatTooltipModule,
    PortalHeaderComponent,
    ReactiveFormsModule,
  ],
  templateUrl: './portal-settings-page.component.html',
  styleUrl: './portal-settings-page.component.scss',
})
export class PortalSettingsPageComponent implements HasUnsavedChanges {
  private readonly destroyRef = inject(DestroyRef);
  private readonly licenseService = inject(GioLicenseService);
  private readonly permissionService = inject(GioPermissionService);
  private readonly portalSettingsService = inject(PortalSettingsService);
  private readonly snackBarService = inject(SnackBarService);
  private readonly canUpdate = this.permissionService.hasAnyMatching(['environment-settings-u']);

  private readonly currentSettings = signal<PortalSettings | null>(null);
  readonly settingsForm = signal<FormGroup<PortalSettingsPageForm> | null>(null);
  readonly formInitialValues = signal<PortalSettingsPageFormValue | null>(null);
  readonly hasEnterpriseLicense$ = this.licenseService.getLicense$().pipe(map(license => license.tier !== 'oss'));

  readonly settingsResource = rxResource({
    stream: () => this.portalSettingsService.get(),
  });

  readonly isSwaggerEnabled = signal(false);
  readonly isRedocEnabled = signal(false);
  readonly availableKafkaMechanisms = ['PLAIN', 'SCRAM-SHA-256', 'SCRAM-SHA-512'];

  constructor() {
    effect(() => {
      if (!this.settingsResource.hasValue()) {
        return;
      }
      this.currentSettings.set(this.settingsResource.value());
    });

    effect(onCleanup => {
      const settings = this.currentSettings();
      if (!settings) {
        return;
      }

      const form = this.createForm(settings);
      const registrationControl = form.controls.portal.controls.userCreation.controls.enabled;
      const registrationSubscription = registrationControl.valueChanges.subscribe(enabled => {
        this.updateAutomaticValidationState(form, settings, enabled);
      });

      this.settingsForm.set(form);
      this.formInitialValues.set(form.getRawValue());
      this.isSwaggerEnabled.set(settings.openAPIDocViewer?.openAPIDocType?.swagger?.enabled ?? false);
      this.isRedocEnabled.set(settings.openAPIDocViewer?.openAPIDocType?.redoc?.enabled ?? false);

      onCleanup(() => registrationSubscription.unsubscribe());
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  beforeUnloadHandler(event: BeforeUnloadEvent): string | void {
    if (this.hasUnsavedChanges()) {
      event.preventDefault();
      event.returnValue = '';
      return '';
    }
  }

  hasUnsavedChanges(): boolean {
    const form = this.settingsForm();
    const initialValues = this.formInitialValues();
    return !!form && !!initialValues && !isEqual(form.getRawValue(), initialValues);
  }

  isSystemReadonly(property: string): boolean {
    const settings = this.currentSettings();
    return settings ? PortalSettingsService.isReadonly(settings, property) : false;
  }

  retry(): void {
    this.settingsResource.reload();
  }

  reset(): void {
    const form = this.settingsForm();
    const initialValues = this.formInitialValues();
    if (!form || !initialValues) {
      return;
    }

    form.reset(initialValues);
    const settings = this.currentSettings();
    if (settings) {
      this.updateAutomaticValidationState(form, settings, initialValues.portal.userCreation.enabled);
    }
    form.markAsPristine();
  }

  save(): void {
    const form = this.settingsForm();
    const settings = this.currentSettings();
    if (!form || !settings || form.invalid || !this.hasUnsavedChanges()) {
      return;
    }

    const payload = this.toSettingsPayload(settings, form.getRawValue());
    this.portalSettingsService
      .save(payload)
      .pipe(
        tap(savedSettings => {
          this.currentSettings.set(savedSettings ?? payload);
          this.snackBarService.success('Portal settings saved successfully.');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'Unable to save portal settings.');
          return EMPTY;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();
  }

  private createForm(settings: PortalSettings): FormGroup<PortalSettingsPageForm> {
    const portal = settings.portal;
    const openApiDocType = settings.openAPIDocViewer?.openAPIDocType;
    const form = new FormGroup<PortalSettingsPageForm>({
      portal: new FormGroup({
        apikeyHeader: new FormControl(
          {
            value: portal?.apikeyHeader ?? '',
            disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'portal.apikey.header'),
          },
          { nonNullable: true },
        ),
        kafkaSaslMechanisms: new FormControl(
          {
            value: portal?.kafkaSaslMechanisms ?? [],
            disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'portal.kafka.saslMechanisms'),
          },
          { nonNullable: true },
        ),
        url: new FormControl(
          {
            value: portal?.url ?? '',
            disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'portal.url'),
          },
          { nonNullable: true },
        ),
        userCreation: new FormGroup({
          enabled: new FormControl(
            {
              value: portal?.userCreation?.enabled ?? false,
              disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'portal.userCreation.enabled'),
            },
            { nonNullable: true },
          ),
          automaticValidation: new FormGroup({
            enabled: new FormControl(
              {
                value: portal?.userCreation?.automaticValidation?.enabled ?? false,
                disabled:
                  !this.canUpdate ||
                  PortalSettingsService.isReadonly(settings, 'portal.userCreation.automaticValidation.enabled') ||
                  !portal?.userCreation?.enabled,
              },
              { nonNullable: true },
            ),
          }),
        }),
      }),
      openAPIDocViewer: new FormGroup({
        openAPIDocType: new FormGroup({
          defaultType: new FormControl(
            {
              value: openApiDocType?.defaultType ?? 'Swagger',
              disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'open.api.doc.type.default'),
            },
            { nonNullable: true },
          ),
        }),
      }),
      portalNext: new FormGroup({
        mtls: new FormGroup({
          enabled: new FormControl(
            {
              value: settings.portalNext?.mtls?.enabled ?? false,
              disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'portal.next.mtls.enabled'),
            },
            { nonNullable: true },
          ),
        }),
        analytics: new FormGroup({
          enabled: new FormControl(
            {
              value: settings.portalNext?.analytics?.enabled ?? false,
              disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'portal.next.analytics.enabled'),
            },
            { nonNullable: true },
          ),
        }),
        catalog: new FormGroup({
          fuzzySearch: new FormGroup({
            enabled: new FormControl(
              {
                value: settings.portalNext?.catalog?.fuzzySearch?.enabled ?? false,
                disabled: !this.canUpdate || PortalSettingsService.isReadonly(settings, 'portal.next.catalog.fuzzySearch.enabled'),
              },
              { nonNullable: true },
            ),
          }),
        }),
      }),
    });

    if (!this.canUpdate) {
      form.disable();
    }
    return form;
  }

  private updateAutomaticValidationState(
    form: FormGroup<PortalSettingsPageForm>,
    settings: PortalSettings,
    registrationEnabled: boolean,
  ): void {
    const automaticValidationControl = form.controls.portal.controls.userCreation.controls.automaticValidation.controls.enabled;
    const isReadonly = PortalSettingsService.isReadonly(settings, 'portal.userCreation.automaticValidation.enabled');
    if (this.canUpdate && registrationEnabled && !isReadonly) {
      automaticValidationControl.enable({ emitEvent: false });
    } else {
      automaticValidationControl.disable({ emitEvent: false });
    }
  }

  private toSettingsPayload(settings: PortalSettings, formValue: PortalSettingsPageFormValue): PortalSettings {
    return {
      ...settings,
      portal: {
        ...settings.portal,
        apikeyHeader: formValue.portal.apikeyHeader,
        kafkaSaslMechanisms: formValue.portal.kafkaSaslMechanisms,
        url: formValue.portal.url,
        userCreation: {
          ...settings.portal?.userCreation,
          enabled: formValue.portal.userCreation.enabled,
          automaticValidation: {
            ...settings.portal?.userCreation?.automaticValidation,
            enabled: formValue.portal.userCreation.automaticValidation.enabled,
          },
        },
      },
      openAPIDocViewer: {
        ...settings.openAPIDocViewer,
        openAPIDocType: {
          ...settings.openAPIDocViewer?.openAPIDocType,
          defaultType: formValue.openAPIDocViewer.openAPIDocType.defaultType,
          swagger: settings.openAPIDocViewer?.openAPIDocType?.swagger ?? { enabled: false },
          redoc: settings.openAPIDocViewer?.openAPIDocType?.redoc ?? { enabled: false },
        },
      },
      portalNext: {
        ...settings.portalNext,
        access: settings.portalNext?.access ?? { enabled: false },
        mtls: {
          ...settings.portalNext?.mtls,
          enabled: formValue.portalNext.mtls.enabled,
        },
        analytics: {
          ...settings.portalNext?.analytics,
          enabled: formValue.portalNext.analytics.enabled,
        },
        catalog: {
          ...settings.portalNext?.catalog,
          fuzzySearch: {
            ...settings.portalNext?.catalog?.fuzzySearch,
            enabled: formValue.portalNext.catalog.fuzzySearch.enabled,
          },
        },
      },
    };
  }
}
