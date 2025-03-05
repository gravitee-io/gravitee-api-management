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
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { SubscriptionConsumerConfiguration } from '../../../../../entities/management-api-v2';
import { MatIcon } from '@angular/material/icon';
import { GioClipboardModule } from '@gravitee/ui-particles-angular';

type PushConfigVM = {
  channel: string;
  callbackUrl: string;
  authType: string;
};

@Component({
  selector: 'api-subscription-edit-push-config',
  templateUrl: './api-subscription-edit-push-config.component.html',
  styleUrls: ['./api-subscription-edit-push-config.component.scss'],
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIcon, GioClipboardModule],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApiSubscriptionEditPushConfigComponent {
  @Input()
  consumerConfiguration!: SubscriptionConsumerConfiguration;

  get pushConfig(): PushConfigVM {
    return {
      channel: this.consumerConfiguration?.channel || '',
      callbackUrl: this.consumerConfiguration?.entrypointConfiguration?.callbackUrl ?? 'unknown',
      authType: this.consumerConfiguration?.entrypointConfiguration?.auth?.type ?? 'none',
    };
  }

  openConsumerConfigurationDialog() {}
}
