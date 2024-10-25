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
import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import {
  GioQuickTimeRangeModule
} from '../../../../../home/components/gio-quick-time-range/gio-quick-time-range.module';
import { ReactiveFormsModule, UntypedFormControl, Validators } from '@angular/forms';
import {
  GioQuickTimeRangeComponent,
  TimeRangeParams
} from "../../../../../home/components/gio-quick-time-range/gio-quick-time-range.component";

@Component({
  selector: 'api-analytics-filters-bar',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatCardModule, GioIconsModule, GioQuickTimeRangeModule, ReactiveFormsModule],
  templateUrl: './api-analytics-filters-bar.component.html',
  styleUrl: './api-analytics-filters-bar.component.scss',
})
export class ApiAnalyticsFiltersBarComponent implements OnInit {
  private static readonly INITIAL_VALUE = '1d';
  @Output()
  public filtersChange = new EventEmitter<TimeRangeParams>();
  timeRangeControl = new UntypedFormControl(ApiAnalyticsFiltersBarComponent.INITIAL_VALUE, Validators.required);

  private lastEmitted = ApiAnalyticsFiltersBarComponent.INITIAL_VALUE;

  ngOnInit() {
    this.timeRangeControl.valueChanges.subscribe((val) => {
      this.lastEmitted = val;
      this.filtersChange.next(GioQuickTimeRangeComponent.getTimeFrameRangesParams(val));
    })
    this.timeRangeControl.setValue(ApiAnalyticsFiltersBarComponent.INITIAL_VALUE); // this trig a first event with initial value
  }

  public refresh() {
    this.filtersChange.next(GioQuickTimeRangeComponent.getTimeFrameRangesParams(this.lastEmitted));
  }
}
