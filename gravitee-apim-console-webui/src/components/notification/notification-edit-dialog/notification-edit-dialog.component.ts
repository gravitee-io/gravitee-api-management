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

import { ChangeDetectionStrategy, Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormControl, FormGroup } from '@angular/forms';
import { groupBy, map } from 'lodash';

import { Hooks } from '../../../entities/notification/hooks';
import { Notifier } from '../../../entities/notification/notifier';
import { NotificationSettings } from '../../../entities/notification/notificationSettings';
import { GroupData } from '../../../management/api/user-group-access/members/api-general-members.component';

export interface NotificationEditDialogData {
  hooks: Hooks[];
  notifier: Notifier;
  notification: NotificationSettings;
  primaryOwner?: string;
  isPrimaryOwner?: boolean;
  isPortalNotification?: boolean;
  isReadonly?: boolean;
  groupData?: GroupData[];
}

export type NotificationEditDialogResult = NotificationSettings;

@Component({
  selector: 'notification-edit-dialog',
  templateUrl: './notification-edit-dialog.component.html',
  styleUrls: ['./notification-edit-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class NotificationEditDialogComponent {
  protected readonly hooks: Hooks[] = this.dialogData.hooks;
  protected readonly groupData: GroupData[] = this.dialogData.groupData;
  protected readonly categories: Category[] = groupHooks(this.dialogData.hooks);
  protected readonly notifier: Notifier = this.dialogData.notifier;
  protected readonly notification: NotificationSettings = this.dialogData.notification;
  protected readonly primaryOwner: string = this.dialogData.primaryOwner;
  protected readonly isPrimaryOwner: boolean = this.dialogData.isPrimaryOwner;
  protected readonly isPortalNotification: boolean = this.dialogData.isPortalNotification;
  protected readonly isReadonly: boolean = this.isReadyOnly();

  protected staticForm = this.buildForm();

  constructor(
    @Inject(MAT_DIALOG_DATA) private readonly dialogData: NotificationEditDialogData,
    public dialogRef: MatDialogRef<NotificationEditDialogData, NotificationEditDialogResult>,
  ) {}

  public submit() {
    if (this.staticForm.invalid) {
      return;
    }
    const raw = this.staticForm.getRawValue();

    const cleansedGroups = raw.groups ? Object.values(raw.groups).filter(g => g !== this.primaryOwner) : [];

    this.dialogRef.close({
      ...this.notification,
      config: raw?.notifier?.config,
      useSystemProxy: raw?.notifier?.useSystemProxy,
      groups: cleansedGroups,
      hooks: Object.values(raw.hooks)
        .map(category => {
          return Object.entries(category)
            .filter(([_, value]) => value)
            .map(([key, _]) => key);
        })
        .flat(),
    });
    this.notification.groups = cleansedGroups;
  }

  private buildForm() {
    return new FormGroup({
      ...(this.notifier != null
        ? {
            notifier: new FormGroup({
              config: new FormControl(this.notification.config),
              useSystemProxy: new FormControl(this.notification.useSystemProxy),
            }),
          }
        : {}),
      groups: new FormControl({
        value: this.withPrimaryOwner(this.notification),
        disabled: false,
      }),
      hooks: toFormGroup(this.categories, this.notification, this.isReadonly),
    });
  }

  private withPrimaryOwner(notification: NotificationSettings): string[] {
    notification.groups?.push(this.primaryOwner);
    return notification.groups;
  }

  private isReadyOnly() {
    return this.notification.origin && this.notification.origin !== 'MANAGEMENT';
  }
}

type Category = {
  name: string;
  hooks: Hooks[];
};

function groupHooks(hooks: Hooks[]): Category[] {
  const group = groupBy(hooks, 'category');
  return map(group, (h, k) => ({ name: k, hooks: h }));
}

const toFormGroup = (categories: Category[], notification: NotificationSettings, readonly: boolean) =>
  categories.reduce(categoryToGroup(notification, readonly), new FormGroup({}));

const categoryToGroup = (notification: NotificationSettings, readonly: boolean) => (group: FormGroup, category: Category) => {
  group.addControl(category.name, category.hooks.reduce(addHookToGroup(notification, readonly), new FormGroup({})));
  return group;
};

const addHookToGroup = (notification: NotificationSettings, readonly: boolean) => (group: FormGroup, hook: Hooks) => {
  group.addControl(
    hook.id,
    new FormControl({
      value: notification.hooks.includes(hook.id) || notification.groupHooks?.includes(hook.id),
      disabled: readonly || notification.groupHooks?.includes(hook.id),
    }),
  );
  return group;
};
