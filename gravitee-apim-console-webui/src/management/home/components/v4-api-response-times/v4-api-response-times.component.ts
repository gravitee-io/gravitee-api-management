import { Component, DestroyRef, OnInit } from "@angular/core";
import { GioLoaderModule } from "@gravitee/ui-particles-angular";
import { MatCardModule } from "@angular/material/card";
import { switchMap } from "rxjs/operators";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

import {
  GioChartLineData,
  GioChartLineOptions
} from "../../../../shared/components/gio-chart-line/gio-chart-line.component";
import { AnalyticsService } from "../../../../services-ngx/analytics.service";
import { HomeService } from "../../../../services-ngx/home.service";
import { SnackBarService } from "../../../../services-ngx/snack-bar.service";
import { GioChartLineModule } from "../../../../shared/components/gio-chart-line/gio-chart-line.module";



@Component({
  selector: 'v4-api-response-times',
  standalone: true,
  imports: [
    GioChartLineModule,
    GioLoaderModule,
    MatCardModule,
  ],
  templateUrl: './v4-api-response-times.component.html',
  styleUrl: './v4-api-response-times.component.scss'
})
export class V4ApiResponseTimesComponent implements OnInit {
  public isLoading = true;
  public chartInput: GioChartLineData[];
  public chartOptions: GioChartLineOptions;

  constructor(
    private readonly analyticsService: AnalyticsService,
    private readonly homeService: HomeService,
    private readonly destroyRef: DestroyRef,
    private readonly snackBarService: SnackBarService,
  ) {}


  ngOnInit() {
    this.homeService
      .timeRangeParams()
      .pipe(
        switchMap(({ interval, from, to }) => {
          this.isLoading = true;
          return this.analyticsService.getV4AverageResponseTimes({
            interval,
            from,
            to,
          });
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (averageResponseTimes) => {
          this.chartInput = this.analyticsService.createChartInput(averageResponseTimes);
          this.chartOptions = this.analyticsService.createChartOptions(averageResponseTimes);
          this.isLoading = false;
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
        },
      });
  }

}
