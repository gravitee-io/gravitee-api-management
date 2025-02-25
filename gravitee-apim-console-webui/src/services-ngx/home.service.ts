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
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

import { timeFrameRangesParams, TimeRangeParams } from '../shared/utils/timeFrameRanges';

@Injectable({
  providedIn: 'root',
})
export class HomeService {
  private initialTimeRange = timeFrameRangesParams('1m');
  private timeRangeParams$ = new BehaviorSubject<TimeRangeParams>(this.initialTimeRange);

  public timeRangeParams() {
    return this.timeRangeParams$.asObservable();
  }

  public setTimeRangeParams(value: TimeRangeParams): void {
    this.timeRangeParams$.next(value);
  }

  public resetTimeRange(): void {
    this.timeRangeParams$.next(this.initialTimeRange);
  }
}
