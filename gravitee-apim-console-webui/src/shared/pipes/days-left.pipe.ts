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

const MS_PER_DAY = 86_400_000;

@Pipe({
  name: 'daysLeft',
  pure: true,
  standalone: false,
})
export class DaysLeftPipe implements PipeTransform {
  transform(value: string | Date | null | undefined, threshold?: number): string | null {
    if (!value) {
      return null;
    }
    const days = Math.ceil((+new Date(value) - +new Date()) / MS_PER_DAY);
    if (threshold != null && days > threshold) {
      return null;
    }
    if (days <= 0) {
      return 'Expires today';
    }
    return days === 1 ? '1 day left' : `${days} days left`;
  }
}
