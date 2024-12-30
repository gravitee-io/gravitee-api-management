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
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { GioLoaderModule } from '@gravitee/ui-particles-angular';
import { switchMap } from 'rxjs/operators';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApiHealthV2Service } from '../../../../../services-ngx/api-health-v2.service';
import { FieldParameter } from '../../../../../entities/management-api-v2/api/v4/healthCheck';

@Component({
  selector: 'global-availability',
  standalone: true,
  imports: [MatCardModule, DecimalPipe, GioLoaderModule],
  templateUrl: './global-availability.component.html',
  styleUrl: './global-availability.component.scss',
})
export class GlobalAvailabilityComponent implements OnInit {
  public isLoading = true;
  public globalAvailability: number;
  private readonly apiId = this.activatedRoute.snapshot.params.apiId;

  constructor(
    private readonly destroyRef: DestroyRef,
    private readonly activatedRoute: ActivatedRoute,
    private readonly snackBarService: SnackBarService,
    public readonly apiHealthV2Service: ApiHealthV2Service,
  ) {}

  ngOnInit() {
    this.apiHealthV2Service
      .activeFilter()
      .pipe(
        switchMap((timeRange) => {
          this.isLoading = true;
          return this.apiHealthV2Service.getApiAvailability(this.apiId, timeRange.from, timeRange.to, FieldParameter.endpoint);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (res) => {
          this.globalAvailability = res.global;
          this.isLoading = false;
        },
        error: ({ error }) => {
          this.snackBarService.error(error.message);
          this.isLoading = false;
        },
      });
  }
}
