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
  private selectedApiIds$ = new BehaviorSubject<string[]>([]);

  public timeRangeParams() {
    return this.timeRangeParams$.asObservable();
  }

  public selectedApiIds() {
    return this.selectedApiIds$.asObservable();
  }

  public setTimeRangeParams(value: TimeRangeParams): void {
    this.clearSelectedApiIds();
    this.timeRangeParams$.next(value);
  }

  public resetTimeRange(): void {
    this.clearSelectedApiIds();
    this.timeRangeParams$.next(this.initialTimeRange);
  }

  public toggleSelectedApiId(apiId: string): void {
    const selectedApiIds = this.selectedApiIds$.value;
    if (selectedApiIds.includes(apiId)) {
      this.selectedApiIds$.next(selectedApiIds.filter(id => id !== apiId));
      return;
    }
    this.selectedApiIds$.next([...selectedApiIds, apiId]);
  }

  public isApiSelected(apiId: string): boolean {
    return this.selectedApiIds$.value.includes(apiId);
  }

  public clearSelectedApiIds(): void {
    if (this.selectedApiIds$.value.length === 0) {
      return;
    }
    this.selectedApiIds$.next([]);
  }
}
