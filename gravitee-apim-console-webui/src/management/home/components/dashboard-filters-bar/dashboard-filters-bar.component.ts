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

import { Component, DestroyRef, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCard, MatCardContent } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GioIconsModule } from '@gravitee/ui-particles-angular';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { OwlMomentDateTimeModule } from '@danielmoncada/angular-datetime-picker-moment-adapter';
import moment, { Moment } from 'moment/moment';

import {
  customTimeFrames,
  DATE_TIME_FORMATS,
  timeFrameRangesParams,
  timeFrames,
  TimeRangeParams,
} from '../../../../shared/utils/timeFrameRanges';
import { HomeService } from '../../../../services-ngx/home.service';

@Component({
  selector: 'app-dashboard-filters-bar',
  standalone: true,
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatTooltipModule,
    FormsModule,
    ReactiveFormsModule,
    GioIconsModule,
    MatCard,
    MatCardContent,
    MatInputModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
  ],
  providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: DATE_TIME_FORMATS }],
  templateUrl: './dashboard-filters-bar.component.html',
  styleUrl: './dashboard-filters-bar.component.scss',
})
export class DashboardFiltersBarComponent implements OnInit {
  public options = [...timeFrames, ...customTimeFrames];
  public form: FormGroup;
  public minDate: Moment;
  public nowDate: Moment = moment().add(1, 'd');

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly destroyRef: DestroyRef,
    private readonly homeService: HomeService,
  ) {}

  ngOnInit() {
    this.initForm();
  }

  private initForm() {
    this.form = this.formBuilder.group({
      period: '1m',
      from: null,
      to: null,
    });

    this.form
      .get('from')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((from): void => {
        this.minDate = from;
      });

    this.form
      .get('period')
      .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((periodId): void => {
        if (periodId !== 'custom') {
          this.homeService.setTimeRangeParams(timeFrameRangesParams(periodId));
        }
      });
  }

  get periodFormValue() {
    return this.form.get('period') as FormControl;
  }

  public refreshData() {
    this.homeService.setTimeRangeParams(timeFrameRangesParams(this.form.get('period').value));
  }

  public applyCustomDateRange() {
    const customTimeRangeParams: TimeRangeParams = {
      id: this.form.get('period').value,
      from: this.form.get('from').value.valueOf(),
      to: this.form.get('to').value.valueOf(),
      interval: 1000,
    };
    this.homeService.setTimeRangeParams(customTimeRangeParams);
  }
}
