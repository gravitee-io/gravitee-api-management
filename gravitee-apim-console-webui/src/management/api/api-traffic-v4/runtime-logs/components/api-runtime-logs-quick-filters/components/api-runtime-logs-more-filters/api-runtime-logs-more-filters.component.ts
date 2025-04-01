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

import { DEFAULT_FILTERS, MoreFiltersForm } from '../../../../models';

@Component({
  selector: 'api-runtime-logs-more-filters',
  templateUrl: './api-runtime-logs-more-filters.component.html',
  styleUrls: ['./api-runtime-logs-more-filters.component.scss'],
  standalone: false,
})
export class ApiRuntimeLogsMoreFiltersComponent {
  @Output() closeMoreFiltersEvent: EventEmitter<void> = new EventEmitter();
  @Output() applyMoreFiltersEvent: EventEmitter<MoreFiltersForm> = new EventEmitter();
  @Input() showMoreFilters = false;
  @Input() formValues: MoreFiltersForm;
  currentValues: MoreFiltersForm;
  isInvalid: boolean;

  resetMoreFilters() {
    this.currentValues = { period: DEFAULT_FILTERS.period, from: null, to: null, statuses: null, applications: null };
    this.apply();
  }

  close() {
    this.closeMoreFiltersEvent.emit();
  }

  apply() {
    this.applyMoreFiltersEvent.emit(this.currentValues);
    this.close();
  }
}
