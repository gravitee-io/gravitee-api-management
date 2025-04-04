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
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'localizedDate',
  standalone: false,
})
export class LocalizedDatePipe implements PipeTransform {
  transform(value: any, format = 'shortDate', defaultValue: string = null): string {
    if (value == null && defaultValue != null) {
      return defaultValue;
    }
    if (format === 'longDate') {
      return new Date(value).toLocaleString();
    } else if (format === 'time') {
      return new Date(value).toLocaleTimeString();
    }
    return new Date(value).toLocaleDateString();
  }
}
