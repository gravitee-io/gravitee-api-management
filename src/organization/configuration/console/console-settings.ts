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
import { takeUntil } from 'rxjs/operators';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { set } from 'lodash';

import { ConsoleSettingsService } from '../../../services-ngx/console-settings.service';
import { ConsoleSettings } from '../../../entities/consoleSettings';

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

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  private hasIdpDefined = () => {
    return (
      this.settings.authentication?.google?.clientId ||
      this.settings.authentication?.github?.clientId ||
      this.settings.authentication?.oauth2?.clientId
    );
  };

  constructor(private readonly fb: FormBuilder, private readonly consoleSettingsService: ConsoleSettingsService) {}

  ngOnInit(): void {
    this.consoleSettingsService
      .get()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((settings) => {
        this.isLoading = false;
        this.settings = settings;

        // FIXME: Rule kept after Angular migration
        // The properties of this rule do not concern the editable forms of this component
        // To see to move it on the backend or in service
        set(this.settings, 'authentication.localLogin.enabled', this.settings.authentication?.localLogin?.enabled || !this.hasIdpDefined());

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
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  isReadonlySetting(property: string): boolean {
    return ConsoleSettingsService.isReadonly(this.settings, property);
  }
}
