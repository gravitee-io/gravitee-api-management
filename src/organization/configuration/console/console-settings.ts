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
import { cloneDeep, get, isEmpty, set } from 'lodash';
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
            title: [toFormState(this.settings, 'management.title')],
            url: [toFormState(this.settings, 'management.url')],
            support: [toFormState(this.settings, 'management.support.enabled')],
            userCreation: [toFormState(this.settings, 'management.userCreation.enabled')],
            automaticValidation: [toFormState(this.settings, 'management.automaticValidation.enabled')],
          }),
          theme: this.fb.group({
            name: [toFormState(this.settings, 'theme.name')],
            logo: [toFormState(this.settings, 'theme.logo')],
            loader: [toFormState(this.settings, 'theme.loader')],
          }),
          scheduler: this.fb.group({
            tasks: [toFormState(this.settings, 'scheduler.tasks')],
            notifications: [toFormState(this.settings, 'scheduler.notifications')],
          }),
          alert: [toFormState(this.settings, 'alert.enabled')],
          cors: this.fb.group({
            allowOrigin: [toFormState(this.settings, 'cors.allowOrigin', []), [CorsUtil.allowOriginValidator()]],
            allowMethods: [toFormState(this.settings, 'cors.allowMethods', [])],
            allowHeaders: [toFormState(this.settings, 'cors.allowHeaders', [])],
            exposedHeaders: [toFormState(this.settings, 'cors.exposedHeaders', [])],
            maxAge: [toFormState(this.settings, 'cors.maxAge')],
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
        this.formSettings.get('email.enabled').valueChanges.subscribe((checked) => {
          controlKeys
            .filter((k) => !isReadonlySetting(this.settings, `email.${k}`))
            .forEach((k) => {
              return checked ? this.formSettings.get(`email.${k}`).enable() : this.formSettings.get(`email.${k}`).disable();
            });
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

const isReadonlySetting = (consoleSettings: ConsoleSettings, property: string): boolean => {
  return ConsoleSettingsService.isReadonly(consoleSettings, property);
};

const toFormState = (consoleSettings: ConsoleSettings, path: string, defaultValue: unknown = undefined) => {
  return { value: get(consoleSettings, path, defaultValue), disabled: isReadonlySetting(consoleSettings, path) };
};
