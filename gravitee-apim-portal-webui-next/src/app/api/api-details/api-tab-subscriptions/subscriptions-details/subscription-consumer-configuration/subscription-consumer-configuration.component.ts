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
import { Component, computed, input, InputSignal } from '@angular/core';
import { MatAnchor } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatListModule } from '@angular/material/list';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';

import { SubscriptionConsumerConfiguration } from '../../../../../../entities/subscription';

@Component({
  imports: [MatCardModule, MatListModule, MatAnchor, MatTableModule, RouterLink],
  selector: 'app-subscription-consumer-configuration',
  standalone: true,
  templateUrl: './subscription-consumer-configuration.component.html',
  styleUrls: ['./subscription-consumer-configuration.component.scss'],
})
export class SubscriptionConsumerConfigurationComponent {
  canConfigure: InputSignal<boolean> = input<boolean>(false);
  consumerConfiguration: InputSignal<SubscriptionConsumerConfiguration> = input.required<SubscriptionConsumerConfiguration>();
  hasChannel = computed(() => !!this.consumerConfiguration().channel);
  headers = computed(() => this.consumerConfiguration().entrypointConfiguration?.headers ?? []);
  displayedColumns = ['name', 'value'];
}
