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
import { Pipe, PipeTransform } from '@angular/core';

import { TimePeriodConfiguration } from '../entities/plan/plan';

@Pipe({
  name: 'toPeriodTimeUnitLabelPipe',
  standalone: true,
})
export class ToPeriodTimeUnitLabelPipe implements PipeTransform {
  transform(value: TimePeriodConfiguration): string {
    if (value.period_time === 1) {
      return `${value.period_time_unit?.slice(0, -1).toLowerCase() ?? ''}`;
    } else {
      return `${value.period_time} ${value.period_time_unit?.toLowerCase() ?? ''}`;
    }
  }
}
