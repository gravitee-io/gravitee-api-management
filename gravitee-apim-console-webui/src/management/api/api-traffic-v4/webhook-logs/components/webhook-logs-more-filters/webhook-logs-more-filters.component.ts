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
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GioIconsModule } from '@gravitee/ui-particles-angular';

import { WebhookLogsMoreFiltersFormComponent } from './components/webhook-logs-more-filters-form/webhook-logs-more-filters-form.component';

import { DEFAULT_PERIOD } from '../../../runtime-logs/models';
import { WebhookMoreFiltersForm } from '../../models/webhook-logs.models';

@Component({
  selector: 'webhook-logs-more-filters',
  templateUrl: './webhook-logs-more-filters.component.html',
  styleUrls: ['./webhook-logs-more-filters.component.scss'],
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, GioIconsModule, WebhookLogsMoreFiltersFormComponent],
})
export class WebhookLogsMoreFiltersComponent implements OnChanges {
  @Output() closeMoreFiltersEvent = new EventEmitter<void>();
  @Output() applyMoreFiltersEvent = new EventEmitter<WebhookMoreFiltersForm>();
  @Input() showMoreFilters = false;
  @Input() formValues: WebhookMoreFiltersForm = { period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] };
  @Input() callbackUrls: string[] = [];

  currentValues: WebhookMoreFiltersForm = { period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] };
  isInvalid = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.formValues) {
      this.currentValues = {
        period: changes.formValues.currentValue?.period ?? DEFAULT_PERIOD,
        from: changes.formValues.currentValue?.from ?? null,
        to: changes.formValues.currentValue?.to ?? null,
        callbackUrls: changes.formValues.currentValue?.callbackUrls ?? [],
      };
    }
  }

  onValuesChange(values: WebhookMoreFiltersForm): void {
    this.currentValues = values;
  }

  onInvalidChange(isInvalid: boolean): void {
    this.isInvalid = isInvalid;
  }

  resetMoreFilters(): void {
    this.currentValues = { period: DEFAULT_PERIOD, from: null, to: null, callbackUrls: [] };
    this.apply();
  }

  close(): void {
    this.closeMoreFiltersEvent.emit();
  }

  apply(): void {
    this.applyMoreFiltersEvent.emit(this.currentValues);
    this.close();
  }
}
