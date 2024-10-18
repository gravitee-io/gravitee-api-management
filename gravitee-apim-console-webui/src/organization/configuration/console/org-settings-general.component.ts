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

import { Subject } from 'rxjs';
import { takeUntil, tap } from 'rxjs/operators';
import { Component, OnDestroy, OnInit } from '@angular/core';
<<<<<<< HEAD
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { cloneDeep, get, merge } from 'lodash';
=======
import { FormBuilder, FormGroup } from '@angular/forms';
import { get } from 'lodash';
>>>>>>> 3dcd57d648 (fix: manual merge updated settings)
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { AutocompleteOptions, GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { CorsUtil } from '../../../shared/utils';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'org-settings-general',
  templateUrl: './org-settings-general.component.html',
  styleUrls: ['./org-settings-general.component.scss'],
})
export class OrgSettingsGeneralComponent implements OnInit, OnDestroy {
  isLoading = true;

  formSettings: UntypedFormGroup;

  settings: ConsoleSettings;

  providedConfigurationMessage = 'Configuration provided by the system';

  httpMethods = CorsUtil.httpMethods;

  defaultHttpHeaders: AutocompleteOptions = CorsUtil.defaultHttpHeaders.map((header) => header);

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  private allowAllOriginsConfirmDialog?: MatDialogRef<GioConfirmDialogComponent, boolean>;
  public formInitialValues: unknown;

  constructor(
    private readonly fb: UntypedFormBuilder,
    private readonly consoleSettingsService: ConsoleSettingsService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
  ) {}

  ngOnInit(): void {
    this.consoleSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((settings) => {
        this.isLoading = false;
        this.settings = settings;

        // As in Angular the forms are not typed :
        // The object structure of this.formSettings matches that of ConsoleSettings to be able to send the form result directly
        this.formSettings = this.fb.group({
          management: this.fb.group({
            title: [toFormState(this.settings, 'management.title')],
            url: [toFormState(this.settings, 'management.url')],
            support: this.fb.group({ enabled: [toFormState(this.settings, 'management.support.enabled')] }),
            userCreation: this.fb.group({ enabled: [toFormState(this.settings, 'management.userCreation.enabled')] }),
            automaticValidation: this.fb.group({ enabled: [toFormState(this.settings, 'management.automaticValidation.enabled')] }),
          }),
          scheduler: this.fb.group({
            tasks: [toFormState(this.settings, 'scheduler.tasks', undefined, 'console.scheduler.tasks')],
            notifications: [toFormState(this.settings, 'scheduler.notifications', undefined, 'console.scheduler.notifications')],
          }),
          cors: this.fb.group({
            allowOrigin: [
              toFormState(this.settings, 'cors.allowOrigin', [], 'http.api.management.cors.allow-origin'),
              [CorsUtil.allowOriginValidator()],
            ],
            allowMethods: [toFormState(this.settings, 'cors.allowMethods', [], 'http.api.management.cors.allow-methods')],
            allowHeaders: [toFormState(this.settings, 'cors.allowHeaders', [], 'http.api.management.cors.allow-headers')],
            exposedHeaders: [toFormState(this.settings, 'cors.exposedHeaders', [], 'http.api.management.cors.exposed-headers')],
            maxAge: [toFormState(this.settings, 'cors.maxAge', undefined, 'http.api.management.cors.max-age')],
          }),

          email: this.fb.group({
            enabled: [toFormState(this.settings, 'email.enabled')],
            host: [toFormState(this.settings, 'email.host')],
            port: [toFormState(this.settings, 'email.port')],
            username: [toFormState(this.settings, 'email.username')],
            password: [toFormState(this.settings, 'email.password')],
            protocol: [toFormState(this.settings, 'email.protocol')],
            subject: [toFormState(this.settings, 'email.subject')],
            from: [toFormState(this.settings, 'email.from')],
            properties: this.fb.group({
              auth: [toFormState(this.settings, 'email.properties.auth')],
              startTlsEnable: [toFormState(this.settings, 'email.properties.startTlsEnable')],
              sslTrust: [toFormState(this.settings, 'email.properties.sslTrust')],
            }),
          }),
        });

        // Disable `management.automaticValidation.enabled` if `management.userCreation.enabled` is not checked
        this.formSettings
          .get('management.userCreation.enabled')
          .valueChanges.pipe(takeUntil(this.unsubscribe$))
          // eslint-disable-next-line rxjs/no-nested-subscribe
          .subscribe((checked) => {
            if (checked) {
              this.formSettings.get('management.automaticValidation.enabled').enable();
              return;
            }
            this.formSettings.get('management.automaticValidation.enabled').disable();
          });

        // Disable all `email` group if `email.enabled` is not checked without impacting the already readonly properties
        const controlKeys = [
          'host',
          'port',
          'username',
          'password',
          'protocol',
          'subject',
          'from',
          'properties.auth',
          'properties.startTlsEnable',
          'properties.sslTrust',
        ];
        // eslint-disable-next-line rxjs/no-nested-subscribe
        this.formSettings.get('email.enabled').valueChanges.subscribe((checked) => {
          controlKeys
            .filter((k) => !isReadonlySetting(this.settings, `email.${k}`))
            .forEach((k) => {
              return checked ? this.formSettings.get(`email.${k}`).enable() : this.formSettings.get(`email.${k}`).disable();
            });
        });

        this.formInitialValues = this.formSettings.getRawValue();
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  confirmAllowAllOrigins(): (tag: string, validationCb: (shouldAddTag: boolean) => void) => void {
    return (tag, validationCb) => {
      // Confirm allow all origins
      if ('*' === tag && !this.allowAllOriginsConfirmDialog) {
        this.allowAllOriginsConfirmDialog = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(
          GioConfirmDialogComponent,
          {
            width: '450px',
            data: {
              title: 'Are you sure?',
              content: 'Do you want to remove all cross-origin restrictions?',
              confirmButton: 'Yes, I want to allow all origins.',
            },
            role: 'alertdialog',
            id: 'allowAllOriginsConfirmDialog',
          },
        );

        this.allowAllOriginsConfirmDialog
          .afterClosed()
          .pipe(takeUntil(this.unsubscribe$))
          .subscribe((shouldAddTag) => {
            this.allowAllOriginsConfirmDialog = null;
            validationCb(shouldAddTag);
          });
      } else {
        validationCb(true);
      }
    };
  }

  onSubmit() {
    if (this.formSettings.invalid) {
      return;
    }

    const formSettingsValue = this.formSettings.getRawValue();

    const updatedSettingsPayload = {
      ...this.settings,

      management: {
        ...this.settings.management,
        ...formSettingsValue.management,
        support: {
          ...this.settings.management?.support,
          ...formSettingsValue.management?.support,
        },
        userCreation: {
          ...this.settings.management?.userCreation,
          ...formSettingsValue.management?.userCreation,
        },
        automaticValidation: {
          ...this.settings.management?.automaticValidation,
          ...formSettingsValue.management?.automaticValidation,
        },
      },

      scheduler: {
        ...this.settings.scheduler,
        ...formSettingsValue.scheduler,
      },

      cors: {
        ...this.settings.cors,
        ...formSettingsValue.cors,
      },

      email: {
        ...this.settings.email,
        ...formSettingsValue.email,
        properties: {
          ...this.settings.email?.properties,
          ...formSettingsValue.email?.properties,
        },
      },

      authentication: {
        ...this.settings.authentication,
        ...formSettingsValue.authentication,
      },

      reCaptcha: {
        ...this.settings.reCaptcha,
        ...formSettingsValue.reCaptcha,
      },

      analyticsPendo: {
        ...this.settings.analyticsPendo,
        ...formSettingsValue.analyticsPendo,
      },

      logging: {
        ...this.settings.logging,
        ...formSettingsValue.logging,
        audit: {
          ...this.settings.logging?.audit,
          ...formSettingsValue.logging?.audit,
        },
      },

      maintenance: {
        ...this.settings.maintenance,
        ...formSettingsValue.maintenance,
      },

      newsletter: {
        ...this.settings.newsletter,
        ...formSettingsValue.newsletter,
      },

      v4EmulationEngine: {
        ...this.settings.emulateV4Engine,
        ...formSettingsValue.v4EmulationEngine,
      },

      alertEngine: {
        ...this.settings.alertEngine,
        ...formSettingsValue.alertEngine,
      },
    };

    this.consoleSettingsService
      .save(updatedSettingsPayload)
      .pipe(
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }

  isReadonlySetting(readonlyKey: string): boolean {
    return isReadonlySetting(this.settings, readonlyKey);
  }
}

const isReadonlySetting = (consoleSettings: ConsoleSettings, readonlyKey: string): boolean => {
  return ConsoleSettingsService.isReadonly(consoleSettings, readonlyKey);
};

const toFormState = (consoleSettings: ConsoleSettings, path: string, defaultValue: unknown = undefined, readonlyKey: string = path) => {
  return { value: get(consoleSettings, path, defaultValue), disabled: isReadonlySetting(consoleSettings, readonlyKey) };
};
