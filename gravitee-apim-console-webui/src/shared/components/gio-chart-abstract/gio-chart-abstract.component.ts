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
import { DestroyRef, Directive, inject, signal, WritableSignal } from '@angular/core';
import * as Highcharts from 'highcharts';
import { GioMenuService } from '@gravitee/ui-particles-angular';
import { tap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Directive()
export abstract class GioChartAbstractComponent {
  Highcharts: typeof Highcharts = Highcharts;
  private chart: WritableSignal<Highcharts.Chart | undefined> = signal<Highcharts.Chart>(undefined);

  constructor() {
    inject(GioMenuService)
      .reduced$.pipe(
        tap(() => {
          if (this.chart()) {
            setTimeout(() => {
              this.chart().reflow();
            }, 0);
          }
        }),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe();
  }

  protected chartInstance(chart: Highcharts.Chart) {
    this.chart.set(chart);
  }
}
