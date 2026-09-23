/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { Component, input, Input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTooltip } from '@angular/material/tooltip';

import { MatTooltipOnEllipsisDirective } from '../../directives/mat-tooltip-on-ellipsis.directive';
import { BadgeComponent } from '../badge/badge.component';

@Component({
  selector: 'app-api-card',
  imports: [MatButtonModule, MatCardModule, MatTooltip, MatTooltipOnEllipsisDirective, BadgeComponent],
  templateUrl: './api-card.component.html',
  styleUrl: './api-card.component.scss',
})
export class ApiCardComponent {
  readonly typeLabel = input<string>();
  readonly showNotificationBell = input(false);
  readonly notificationsSubscribed = input(false);
  readonly notificationBusy = input(false);

  @Input({ required: true })
  apiId!: string;
  @Input({ required: true })
  title!: string;
  @Input({ required: true })
  version!: string;
  @Input()
  picture: string | undefined;
  @Input()
  isEnabledMcpServer: boolean = false;
  @Input()
  content?: string;

  cardSelect = output<string>();
  notificationToggle = output<string>();

  notificationBellAriaLabel(): string {
    return this.notificationsSubscribed()
      ? $localize`:@@apiCardNotifyUnsubscribeAria:Unsubscribe from notifications for ${this.title}:apiName:`
      : $localize`:@@apiCardNotifySubscribeAria:Subscribe to notifications for ${this.title}:apiName:`;
  }

  notificationBellTooltip(): string {
    return this.notificationsSubscribed()
      ? $localize`:@@apiCardNotifyUnsubscribeTooltip:Notifications on — click to turn off`
      : $localize`:@@apiCardNotifySubscribeTooltip:Get notified about this API`;
  }

  onNotifyClick(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.notificationBusy()) {
      return;
    }
    this.notificationToggle.emit(this.apiId);
  }

  onNotifyKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      event.stopPropagation();
      if (!this.notificationBusy()) {
        this.notificationToggle.emit(this.apiId);
      }
    }
  }
}
