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
import { Component, EventEmitter, forwardRef, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Subject } from 'rxjs';

import { timeFrames } from '../../../../shared/utils/timeFrameRanges';

@Component({
  selector: 'gio-quick-time-range',
  templateUrl: './gio-quick-time-range.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: forwardRef(() => GioQuickTimeRangeComponent),
    },
  ],
})
export class GioQuickTimeRangeComponent implements ControlValueAccessor {
  private unsubscribe$: Subject<void> = new Subject<void>();

  static getTimeFrameRangesParams(id: string) {
    return timeFrames.find((timeFrame) => timeFrame.id === id)?.timeFrameRangesParams();
  }

  @Output()
  onRefreshClicked = new EventEmitter<void>();
  selectedTimeFrame: string;
  options = timeFrames;

  private _onChange: (value: any) => void = () => ({});

  private _onTouched: () => void = () => ({});

  registerOnChange(fn: any): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this._onTouched = fn;
  }

  writeValue(value: string): void {
    this.selectedTimeFrame = value;
  }

  ngOnDestroy(): void {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  emitValueChange(value: string) {
    this._onChange(value);
  }

  onRefresh($event: Event) {
    $event.stopPropagation();
    this._onTouched();
    this.onRefreshClicked.emit();
  }
}
