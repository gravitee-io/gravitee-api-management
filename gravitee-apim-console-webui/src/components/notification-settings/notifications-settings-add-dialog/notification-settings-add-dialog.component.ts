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
import { MAT_LEGACY_DIALOG_DATA as MAT_DIALOG_DATA, MatLegacyDialogRef as MatDialogRef } from '@angular/material/legacy-dialog';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';

import { Notifier } from '../../../entities/notification/notifier';
import { NewNotificationSettings } from '../../../entities/notification/newNotificationSettings';
import { NotificationSettings } from '../../../entities/notification/notificationSettings';

type Reference = {
  referenceType: 'API' | 'APPLICATION' | 'PORTAL';
  referenceId: string;
};

export interface NotificationSettingsAddDialogData {
  notifier: Notifier[];
  reference: Reference;
}

export type NotificationSettingsAddDialogResult = NewNotificationSettings;

@Component({
  selector: 'notification-settings-add-dialog',
  templateUrl: './notification-settings-add-dialog.component.html',
  styleUrls: ['./notification-settings-add-dialog.component.scss'],
})
export class NotificationSettingsAddDialogComponent {
  notificationForm: UntypedFormGroup;
  notifierList: Notifier[];
  notificationTemplate: NotificationSettings[];
  reference: Reference;
  display = false;

  constructor(
    @Inject(MAT_DIALOG_DATA) dialogData: NotificationSettingsAddDialogData,
    public dialogRef: MatDialogRef<NotificationSettingsAddDialogData, NotificationSettingsAddDialogResult>,
  ) {
    this.notifierList = dialogData.notifier;
    this.reference = dialogData.reference;
    this.notificationForm = new UntypedFormGroup({
      name: new UntypedFormControl(null, [Validators.required]),
      notifier: new UntypedFormControl(this.notifierList[0].id, [Validators.required]),
    });
    this.display = true;
  }

  onSubmit() {
    const { name, notifier } = this.notificationForm.getRawValue();

    this.dialogRef.close({
      name,
      notifier,
      config_type: 'GENERIC',
      hooks: [],
      ...this.reference,
    });
  }
}
