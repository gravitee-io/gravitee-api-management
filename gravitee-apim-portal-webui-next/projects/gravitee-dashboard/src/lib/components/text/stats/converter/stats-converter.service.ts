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
import { MeasureUnit } from '../../../widget/model/request/enum/measure-name';
import { MeasuresResponse } from '../../../widget/model/response/measures-response';

@Injectable({
  providedIn: 'root',
})
export class StatsConverterService implements Converter {
  convert(data: MeasuresResponse) {
    return (
      data?.metrics?.[0]?.measures?.map(({ name, value }) => {
        const unit = MeasureUnit[name];
        const formattedValue = Math.round(value).toLocaleString();
        return unit ? `${formattedValue} ${unit}` : formattedValue;
      }) ?? []
    );
  }
}
