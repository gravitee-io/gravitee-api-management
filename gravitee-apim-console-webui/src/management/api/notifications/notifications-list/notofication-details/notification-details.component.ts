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
import { takeUntil, tap } from 'rxjs/operators';
import { combineLatest, Subject } from 'rxjs';
import { groupBy, map } from 'lodash';
import { FormControl, FormGroup } from '@angular/forms';

import { UIRouterStateParams } from '../../../../../ajs-upgraded-providers';
import { NotificationSettingsService } from '../../../../../services-ngx/notification-settings.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { NotificationSettings } from '../../../../../entities/notification/notificationSettings';
import { Notifier } from '../../../../../entities/notification/notifier';
import { Hooks } from '../../../../../entities/notification/hooks';

type CategoriesHooksVM = {
  name: string;
  hooks: (Hooks & { checked: boolean })[];
}[];

@Component({
  selector: 'notifications-details',
  template: require('./notification-details.component.html'),
  styles: [require('./notification-details.component.scss')],
})
export class NotificationDetailsComponent implements OnInit, OnDestroy {
  public isLoadingData = true;
  public categoriesHooksVM: CategoriesHooksVM;
  public notificationSettings: NotificationSettings;
  public formInitialValues: unknown;
  public notifier: Notifier;
  notificationForm: FormGroup;

  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly notificationSettingsService: NotificationSettingsService,
    private readonly snackBarService: SnackBarService,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;
    combineLatest([
      this.notificationSettingsService.getHooks(),
      this.notificationSettingsService.getSingleNotificationSetting(this.ajsStateParams.apiId, this.ajsStateParams.notificationId),
      this.notificationSettingsService.getNotifiers(this.ajsStateParams.apiId),
    ])
      .pipe(
        tap(([hooks, notificationSettings, notifiers]) => {
          this.notificationSettings = notificationSettings;
          this.notificationForm = new FormGroup({
            notifier: new FormControl(this.notificationSettings.config),
          });

          const hooksChecked: (Hooks & { checked: boolean })[] = hooks.map((hook) => ({
            ...hook,
            checked: notificationSettings.hooks.includes(hook.id),
          }));

          hooksChecked.map((item) => {
            this.notificationForm.addControl(`${item.id}`, new FormControl(item.checked));
          });

          const categories = groupBy(hooksChecked, 'category');
          this.categoriesHooksVM = map(categories, (hooks, k) => ({
            name: k,
            hooks: hooks,
          }));
          this.notifier = notifiers.find((i) => i.id === this.notificationSettings.notifier);
          this.formInitialValues = this.notificationForm.getRawValue();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
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
    };

    this.notificationSettingsService
      .update(this.ajsStateParams.apiId, this.ajsStateParams.notificationId, notificationSettingsValue)
      .pipe(
        tap(() => this.snackBarService.success('Notification settings successfully saved!')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
