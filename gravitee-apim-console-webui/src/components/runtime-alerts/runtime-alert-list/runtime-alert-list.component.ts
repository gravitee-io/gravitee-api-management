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

import { AlertTriggerEntity } from '../../../entities/alerts/alertTriggerEntity';

@Component({
  selector: 'runtime-alert-list',
  templateUrl: './runtime-alert-list.component.html',
  styleUrls: ['./runtime-alert-list.component.scss'],
  standalone: false,
})
export class RuntimeAlertListComponent {
  @Input() public alerts: AlertTriggerEntity[];
  @Input() canCreateAlert: boolean;
  @Output() public createAlert: EventEmitter<void> = new EventEmitter();
  @Output() public deleteAlert: EventEmitter<AlertTriggerEntity> = new EventEmitter();
  @Output() public enableAlert: EventEmitter<AlertTriggerEntity> = new EventEmitter();
  @Output() public disableAlert: EventEmitter<AlertTriggerEntity> = new EventEmitter();
  public displayedColumns = ['name', 'severity', 'description', 'counters', 'lastAlert', 'lastMessage', 'actions'];
}
