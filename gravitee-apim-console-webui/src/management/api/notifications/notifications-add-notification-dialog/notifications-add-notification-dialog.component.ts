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
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup, Validators } from '@angular/forms';

import { Notifier } from '../../../../entities/notification/notifier';
import { NewNotificationSettings } from '../../../../entities/notification/newNotificationSettings';
import { NotificationSettings } from '../../../../entities/notification/notificationSettings';
import { UIRouterStateParams } from '../../../../ajs-upgraded-providers';

export interface NotificationsAddNotificationDialogData {
  notifier: Notifier[];
}

export type NotificationsAddNotificationDialogResult = NewNotificationSettings;

@Component({
  selector: 'notifications-add-notification-dialog',
  template: require('./notifications-add-notification-dialog.component.html'),
  styles: [require('./notifications-add-notification-dialog.component.scss')],
})
export class NotificationsAddNotificationDialogComponent {
  notificationForm: FormGroup;
  notifierList: Notifier[];
  notificationTemplate: NotificationSettings[];

  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: NotificationsAddNotificationDialogData,
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    public dialogRef: MatDialogRef<NotificationsAddNotificationDialogData, NotificationsAddNotificationDialogResult>,
  ) {
    this.notifierList = dialogData.notifier;
    this.notificationForm = new FormGroup({
      name: new FormControl(null, [Validators.required]),
      notifier: new FormControl(this.notifierList[0].id, [Validators.required]),
    });
  }

  onSubmit() {
    const { name, notifier } = this.notificationForm.getRawValue();

    this.dialogRef.close({
      name,
      notifier,
      config_type: 'GENERIC',
      hooks: [],
      referenceType: 'API',
      referenceId: this.ajsStateParams.apiId,
    });
  }
}
