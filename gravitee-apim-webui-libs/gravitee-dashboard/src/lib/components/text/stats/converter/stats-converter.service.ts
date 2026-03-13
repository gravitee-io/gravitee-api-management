/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
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
import { Injectable } from '@angular/core';

import { Converter } from '../../../converter';
import { MeasuresResponse } from '../../../widget/model/response/measures-response';
import { UnitLabel } from '../../../widget/model/response/response';

@Injectable({
  providedIn: 'root',
})
export class StatsConverterService implements Converter {
  convert(data: MeasuresResponse) {
    const metric = data?.metrics?.[0];
    if (!metric?.measures) return [];

    const unitLabel = metric.unit ? UnitLabel[metric.unit] : '';
    return metric.measures.map(({ value }) => {
      const formattedValue = Math.round(value).toLocaleString();
      return unitLabel ? `${formattedValue} ${unitLabel}` : formattedValue;
    });
  }
}
