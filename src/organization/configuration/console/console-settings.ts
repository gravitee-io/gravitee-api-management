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

import { Observable, Subject } from 'rxjs';
import { map, startWith, takeUntil, tap } from 'rxjs/operators';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { MatChipInputEvent, MatChipList } from '@angular/material/chips';
import { cloneDeep, isEmpty, set } from 'lodash';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';

import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';
import { CorsUtil } from '../../../shared/utils';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'gio-org-config-console-settings',
  template: require('./console-settings.html'),
  styles: [require('./console-settings.scss')],
})
export class ConsoleSettingsComponent implements OnInit, OnDestroy {
  isLoading = true;

  formSettings: FormGroup;

  settings: ConsoleSettings;

  providedConfigurationMessage = 'Configuration provided by the system';

  httpMethods = CorsUtil.httpMethods;

  allowHeadersInputFormControl = new FormControl();
  allowHeadersFilteredOptions$: Observable<string[]>;

  exposedHeadersInputFormControl = new FormControl();
  exposedHeadersFilteredOptions$: Observable<string[]>;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  private allowAllOriginsConfirmDialog?: MatDialogRef<GioConfirmDialogComponent, boolean>;

  constructor(
    private readonly fb: FormBuilder,
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

        this.formSettings = this.fb.group({
          management: this.fb.group({
            title: [{ value: this.settings?.management?.title, disabled: this.isReadonlySetting('management.title') }],
            url: [{ value: this.settings?.management?.url, disabled: this.isReadonlySetting('management.url') }],
            support: [
              { value: this.settings?.management?.support?.enabled, disabled: this.isReadonlySetting('management.support.enabled') },
            ],
            userCreation: [
              {
                value: this.settings?.management?.userCreation?.enabled,
                disabled: this.isReadonlySetting('management.userCreation.enabled'),
              },
            ],
            automaticValidation: [
              {
                value: this.settings?.management?.automaticValidation?.enabled,
                disabled: this.isReadonlySetting('management.automaticValidation.enabled'),
              },
            ],
          }),
          theme: this.fb.group({
            name: [{ value: this.settings?.theme?.name, disabled: this.isReadonlySetting('theme.name') }],
            logo: [{ value: this.settings?.theme?.logo, disabled: this.isReadonlySetting('theme.logo') }],
            loader: [{ value: this.settings?.theme?.loader, disabled: this.isReadonlySetting('theme.loader') }],
          }),
          scheduler: this.fb.group({
            tasks: [{ value: this.settings?.scheduler?.tasks, disabled: this.isReadonlySetting('scheduler.tasks') }],
            notifications: [
              { value: this.settings?.scheduler?.notifications, disabled: this.isReadonlySetting('scheduler.notifications') },
            ],
          }),
          alert: [{ value: this.settings?.alert?.enabled, disabled: this.isReadonlySetting('alert.enabled') }],
          cors: this.fb.group({
            allowOrigin: [
              { value: this.settings?.cors?.allowOrigin ?? [], disabled: this.isReadonlySetting('cors.allowOrigin') },
              [CorsUtil.allowOriginValidator()],
            ],
            allowMethods: [{ value: this.settings?.cors?.allowMethods ?? [], disabled: this.isReadonlySetting('cors.allowMethods') }],
            allowHeaders: [{ value: this.settings?.cors?.allowHeaders ?? [], disabled: this.isReadonlySetting('cors.allowHeaders') }],
            exposedHeaders: [{ value: this.settings?.cors?.exposedHeaders ?? [], disabled: this.isReadonlySetting('cors.exposedHeaders') }],
            maxAge: [{ value: this.settings?.cors?.maxAge, disabled: this.isReadonlySetting('cors.maxAge') }],
          }),

          email: this.fb.group({
            enabled: [{ value: this.settings?.email?.enabled, disabled: this.isReadonlySetting('email.enabled') }],
            host: [{ value: this.settings?.email?.host, disabled: this.isReadonlySetting('email.host') }],
            port: [{ value: this.settings?.email?.port, disabled: this.isReadonlySetting('email.port') }],
            username: [{ value: this.settings?.email?.username, disabled: this.isReadonlySetting('email.username') }],
            password: [{ value: this.settings?.email?.password, disabled: this.isReadonlySetting('email.password') }],
            protocol: [{ value: this.settings?.email?.protocol, disabled: this.isReadonlySetting('email.protocol') }],
            subject: [{ value: this.settings?.email?.subject, disabled: this.isReadonlySetting('email.subject') }],
            from: [{ value: this.settings?.email?.from, disabled: this.isReadonlySetting('email.from') }],
            properties: this.fb.group({
              auth: [{ value: this.settings?.email?.properties?.auth, disabled: this.isReadonlySetting('email.properties.auth') }],
              startTlsEnable: [
                {
                  value: this.settings?.email?.properties?.startTlsEnable,
                  disabled: this.isReadonlySetting('email.properties.startTlsEnable'),
                },
              ],
              sslTrust: [
                { value: this.settings?.email?.properties?.sslTrust, disabled: this.isReadonlySetting('email.properties.sslTrust') },
              ],
            }),
          }),
        });

        // Disable `management.automaticValidation` if `management.userCreation` is not checked
        this.formSettings
          .get('management.userCreation')
          .valueChanges.pipe(takeUntil(this.unsubscribe$))
          .subscribe((checked) => {
            if (checked) {
              this.formSettings.get('management.automaticValidation').enable();
              return;
            }
            this.formSettings.get('management.automaticValidation').disable();
          });

        // Disable all `email` group if `email.enabled` is not checked without impacting the already readonly properties
        this.formSettings.get('email.enabled').valueChanges.subscribe((checked) => {
          const controlKey = [
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

          if (checked) {
            controlKey.filter((k) => !this.isReadonlySetting(`email.${k}`)).forEach((k) => this.formSettings.get(`email.${k}`).enable());
            return;
          }
          controlKey.filter((k) => !this.isReadonlySetting(`email.${k}`)).forEach((k) => this.formSettings.get(`email.${k}`).disable());
        });
      });

    this.allowHeadersFilteredOptions$ = this.allowHeadersInputFormControl.valueChanges.pipe(
      startWith(''),
      map((value: string | null) => {
        return CorsUtil.defaultHttpHeaders.filter((defaultHeader) => defaultHeader.toLowerCase().includes((value ?? '').toLowerCase()));
      }),
    );

    this.exposedHeadersFilteredOptions$ = this.exposedHeadersInputFormControl.valueChanges.pipe(
      startWith(''),
      map((value: string | null) => {
        return CorsUtil.defaultHttpHeaders.filter((defaultHeader) => defaultHeader.toLowerCase().includes((value ?? '').toLowerCase()));
      }),
    );
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  isReadonlySetting(property: string): boolean {
    return ConsoleSettingsService.isReadonly(this.settings, property);
  }

  addChipToFormControl(event: MatChipInputEvent, formControlPath: string, matChipList: MatChipList): void {
    const input = event.chipInput.inputElement;
    const chipToAdd = (event.value ?? '').trim();
    const formControl = this.formSettings.get(formControlPath);

    // Add new Chip in form control
    if (!isEmpty(chipToAdd)) {
      // Delete Chip if already existing
      const formControlValue = [...formControl.value].filter((v) => v !== chipToAdd);

      formControl.setValue([...formControlValue, chipToAdd]);
    }

    // Reset the input value
    if (input) {
      input.value = '';
    }

    // Check error state
    matChipList.errorState = formControl.errors !== null;
  }

  confirmAllowAllOrigins(event: MatChipInputEvent) {
    const chipToAdd = (event.value ?? '').trim();
    const formControl = this.formSettings.get('cors.allowOrigin');

    // Confirm allow all origins
    if ('*' === chipToAdd && !this.allowAllOriginsConfirmDialog) {
      this.allowAllOriginsConfirmDialog = this.matDialog.open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '300px',
        data: {
          title: 'Are you sure you want to remove all cross-origin restrictions?',
          confirmButton: 'Yes, I want to allow all origins.',
        },
        role: 'alertdialog',
        id: 'allowAllOriginsConfirmDialog',
      });
      this.allowAllOriginsConfirmDialog
        .afterClosed()
        .pipe(takeUntil(this.unsubscribe$))
        .subscribe((confirm) => {
          if (!confirm) {
            formControl.setValue([...formControl.value].filter((v) => v !== chipToAdd));
          }
          this.allowAllOriginsConfirmDialog = undefined;
        });
    }
  }

  removeChipToFormControl(value: string, formControlPath: string, matChipList: MatChipList) {
    const formControl = this.formSettings.get(formControlPath);
    // Remove Chip in form control
    formControl.setValue([...formControl.value].filter((v) => v !== value));

    // Check error state
    matChipList.errorState = formControl.errors !== null;
  }

  addSelectedToFormControl(event: MatAutocompleteSelectedEvent, formControlPath: string): void {
    const optionToAdd = event.option.viewValue;

    // Add selected option in form control
    if (!isEmpty(optionToAdd)) {
      const formControl = this.formSettings.get(formControlPath);
      // Delete Chip if already existing
      const formControlValue = [...formControl.value].filter((v) => v !== optionToAdd);

      formControl.setValue([...formControlValue, optionToAdd]);
    }
    this.allowHeadersInputFormControl.setValue(null);
    this.allowHeadersInputFormControl.updateValueAndValidity();
  }

  onSubmit() {
    if (this.formSettings.invalid) {
      return;
    }

    const formSettingsValue = this.formSettings.getRawValue();

    const settingsToSave = cloneDeep(this.settings);

    set(settingsToSave, 'management.title', formSettingsValue.management.title);
    set(settingsToSave, 'management.url', formSettingsValue.management.url);
    set(settingsToSave, 'management.support', {
      enabled: formSettingsValue.management.support,
    });
    set(settingsToSave, 'management.userCreation', {
      enabled: formSettingsValue.management.userCreation,
    });
    set(settingsToSave, 'management.automaticValidation', {
      enabled: formSettingsValue.management.automaticValidation,
    });

    set(settingsToSave, 'theme.name', formSettingsValue.theme.name);
    set(settingsToSave, 'theme.logo', formSettingsValue.theme.logo);
    set(settingsToSave, 'theme.loader', formSettingsValue.theme.loader);

    set(settingsToSave, 'scheduler.tasks', formSettingsValue.scheduler.tasks);
    set(settingsToSave, 'scheduler.notifications', formSettingsValue.scheduler.notifications);

    set(settingsToSave, 'alert', { enabled: formSettingsValue.alert });

    set(settingsToSave, 'cors.allowOrigin', formSettingsValue.cors.allowOrigin);
    set(settingsToSave, 'cors.allowMethods', formSettingsValue.cors.allowMethods);
    set(settingsToSave, 'cors.allowHeaders', formSettingsValue.cors.allowHeaders);
    set(settingsToSave, 'cors.exposedHeaders', formSettingsValue.cors.exposedHeaders);
    set(settingsToSave, 'cors.maxAge', formSettingsValue.cors.maxAge);

    set(settingsToSave, 'email.enabled', formSettingsValue.email.enabled);
    set(settingsToSave, 'email.host', formSettingsValue.email.host);
    set(settingsToSave, 'email.port', formSettingsValue.email.port);
    set(settingsToSave, 'email.username', formSettingsValue.email.username);
    set(settingsToSave, 'email.password', formSettingsValue.email.password);
    set(settingsToSave, 'email.protocol', formSettingsValue.email.protocol);
    set(settingsToSave, 'email.subject', formSettingsValue.email.subject);
    set(settingsToSave, 'email.from', formSettingsValue.email.from);
    set(settingsToSave, 'email.properties.auth', formSettingsValue.email.properties.auth);
    set(settingsToSave, 'email.properties.startTlsEnable', formSettingsValue.email.properties.startTlsEnable);
    set(settingsToSave, 'email.properties.sslTrust', formSettingsValue.email.properties.sslTrust);

    this.consoleSettingsService
      .save(settingsToSave)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
      )
      .subscribe();
  }
}
