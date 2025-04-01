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

import { AfterViewInit, Component, ElementRef, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import * as Highcharts from 'highcharts';
import { asyncScheduler, combineLatest, Observable, ReplaySubject } from 'rxjs';
import { map, observeOn } from 'rxjs/operators';

export interface HealthAvailabilityTimeFrameOption {
  timestamp: {
    start: number;
    interval: number;
  };
  data: number[];
}

interface HealthAvailabilityTimeFrameColors {
  colorBad: string;
  colorWarning: string;
  colorGood: string;
}

@Component({
  selector: 'health-availability-time-frame',
  templateUrl: './health-availability-time-frame.component.html',
  styleUrls: ['./health-availability-time-frame.component.scss'],
  standalone: false,
})
export class HealthAvailabilityTimeFrameComponent implements AfterViewInit, OnChanges {
  @Input()
  public option: HealthAvailabilityTimeFrameOption;
  private optionChange$ = new ReplaySubject<HealthAvailabilityTimeFrameOption>(1);

  @ViewChild('colorBad') colorBadElement: ElementRef;
  @ViewChild('colorWarning') colorWarningElement: ElementRef;
  @ViewChild('colorGood') colorGoodElement: ElementRef;

  private colors$ = new ReplaySubject<HealthAvailabilityTimeFrameColors>(1);

  Highcharts: typeof Highcharts = Highcharts;
  chartOptions$: Observable<Highcharts.Options> = combineLatest([this.optionChange$, this.colors$]).pipe(
    observeOn(asyncScheduler),
    map(([option, colors]) => getChartOption(option, colors)),
  );

  ngAfterViewInit() {
    this.colors$.next({
      colorBad: getComputedStyle(this.colorBadElement.nativeElement).color,
      colorWarning: getComputedStyle(this.colorWarningElement.nativeElement).color,
      colorGood: getComputedStyle(this.colorGoodElement.nativeElement).color,
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.option) {
      this.optionChange$.next(changes.option.currentValue);
    }
  }
}

const getChartOption = (option: HealthAvailabilityTimeFrameOption, colors: HealthAvailabilityTimeFrameColors): Highcharts.Options => {
  return {
    chart: {
      type: 'column',
      backgroundColor: 'transparent',
      marginBottom: 20,
    },
    title: undefined,
    plotOptions: {
      series: {
        pointStart: option.timestamp.start,
        pointInterval: option.timestamp.interval,
        marker: {
          enabled: false,
        },
      },
    },
    series: [
      {
        name: 'Availability',
        data: [...option.data],
        color: colors.colorGood,
        type: 'column',
        label: {
          format: '{value} %',
        },
        zones: [
          {
            value: 80,
            color: colors.colorBad,
          },
          {
            value: 95,
            color: colors.colorWarning,
          },
          {
            color: colors.colorGood,
          },
        ],
      },
    ],
    xAxis: {
      type: 'datetime',
      dateTimeLabelFormats: {
        month: '%e. %b',
        year: '%b',
      },
      crosshair: true,
    },
    yAxis: {
      visible: false,
      max: 100,
    },

    tooltip: {
      pointFormat: '<tr><td>{series.name}: </td><td><b>{point.y:.1f} %</b></td></tr>',
      shared: true,
      useHTML: true,
      style: {
        zIndex: 1000,
      },
    },
    legend: {
      enabled: false,
    },
    credits: {
      enabled: false,
    },
  };
};
