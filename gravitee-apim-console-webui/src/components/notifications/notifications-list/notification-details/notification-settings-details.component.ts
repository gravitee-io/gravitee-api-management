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

import { Component, Inject, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { combineLatest, Observable, Subject } from 'rxjs';
import { groupBy, map } from 'lodash';
import { StateService } from '@uirouter/angular';

import { UIRouterState } from '../../../../ajs-upgraded-providers';
import { SnackBarService } from '../../../../services-ngx/snack-bar.service';
import { Hooks } from '../../../../entities/notification/hooks';
import { NotificationSettings } from '../../../../entities/notification/notificationSettings';
import { Notifier } from '../../../../entities/notification/notifier';

export interface NotificationSettingsDetailsServices {
  reference: {
    referenceType: 'API' | 'APPLICATION' | 'PORTAL';
    referenceId: string;
  };
  update: (updatedNotification: NotificationSettings) => Observable<NotificationSettings>;
  getHooks: () => Observable<Hooks[]>;
  getSingleNotificationSetting: () => Observable<NotificationSettings>;
  getNotifiers: () => Observable<Notifier[]>;
}

type CategoriesHooksVM = {
  name: string;
  hooks: (Hooks & { checked: boolean })[];
}[];

@Component({
  selector: 'notification-settings-details',
  template: require('./notification-settings-details.component.html'),
  styles: [require('./notification-settings-details.component.scss')],
})
export class NotificationSettingsDetailsComponent implements OnInit {
  public isLoadingData = true;
  public notificationForm: FormGroup;
  public notificationSettingsListPath: string;
  public formInitialValues: unknown;
  public categoriesHooksVM: CategoriesHooksVM;
  public notifier: Notifier;
  public notificationSettings: NotificationSettings;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input() notificationSettingsDetailsServices: NotificationSettingsDetailsServices;

  constructor(@Inject(UIRouterState) private readonly ajsState: StateService, private readonly snackBarService: SnackBarService) {}

  public ngOnInit() {
    this.isLoadingData = true;
    this.notificationSettingsListPath = this.ajsState.$current.parent.name + '.notification-settings';
    combineLatest([
      this.notificationSettingsDetailsServices.getHooks(),
      this.notificationSettingsDetailsServices.getSingleNotificationSetting(),
      this.notificationSettingsDetailsServices.getNotifiers(),
    ])
      .pipe(
        tap(([hooks, notificationSettings, notifiers]) => {
          this.notificationSettings = notificationSettings;
          this.notifier = notifiers.find((i) => i.id === notificationSettings.notifier);

          this.notificationForm = new FormGroup({
            notifier: new FormControl(notificationSettings.config),
            useSystemProxy: new FormControl(notificationSettings.useSystemProxy),
          });

          const hooksChecked: (Hooks & { checked: boolean })[] = hooks.map((hook) => ({
            ...hook,
            checked: notificationSettings.hooks?.includes(hook.id),
          }));

          hooksChecked.map((item) => {
            this.notificationForm.addControl(`${item.id}`, new FormControl(item.checked));
          });

          const categories = groupBy(hooksChecked, 'category');
          this.categoriesHooksVM = map(categories, (hooks, k) => ({
            name: k,
            hooks: hooks,
          }));
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    if (this.notificationForm.invalid) {
      return;
    }

    const notificationSettingsValue = {
      ...this.notificationSettings,
      hooks: Object.keys(
        Object.fromEntries(Object.entries(this.notificationForm.getRawValue()).filter(([key, value]) => value && key !== 'config')),
      ),
      config: this.notificationForm.controls.notifier.value,
      useSystemProxy: this.notificationForm.controls.useSystemProxy.value,
    };

    this.notificationSettingsDetailsServices
      .update(notificationSettingsValue)
      .pipe(
        tap(() => this.snackBarService.success('Notification settings successfully saved!')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
