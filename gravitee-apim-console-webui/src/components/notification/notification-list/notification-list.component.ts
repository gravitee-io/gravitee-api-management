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
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { Notifier } from '../../../entities/notification/notifier';

export interface NotificationSummary {
  id: string;
  name: string;
  subscribedEvents: number;
  notifier: Notifier;
  isPortalNotification: boolean;
}

@Component({
  selector: 'notification-list',
  templateUrl: './notification-list.component.html',
  styleUrls: ['./notification-list.component.scss'],
  standalone: false,
})
export class NotificationListComponent {
  public displayedColumns = ['name', 'subscribedEvents', 'notifier', 'actions'];

  @Input()
  public canUpdate = false;
  @Input()
  public canDelete = false;
  @Input()
  public loading = true;
  @Input()
  public notifications: NotificationSummary[];

  @Output()
  public delete = new EventEmitter<NotificationSummary>();
  @Output()
  public edit = new EventEmitter<string>();
}
