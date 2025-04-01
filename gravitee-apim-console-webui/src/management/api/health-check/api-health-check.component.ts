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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiHealthCheckFormComponent } from '../component/health-check-form/api-health-check-form.component';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../util/apiFilter.operator';
import { Proxy } from '../../../entities/management-api-v2';

@Component({
  selector: 'api-health-check',
  templateUrl: './api-health-check.component.html',
  styleUrls: ['./api-health-check.component.scss'],
  standalone: false,
})
export class ApiHealthCheckComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public healthCheckForm: UntypedFormGroup;

  public isReadOnly = false;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV1V2Filter(this.snackBarService),
        tap((api) => {
          const isReadOnly =
            !this.permissionService.hasAnyMatching(['api-health-c', 'api-health-u']) || api.definitionContext?.origin === 'KUBERNETES';

          this.healthCheckForm = ApiHealthCheckFormComponent.NewHealthCheckFormGroup(api.services.healthCheck, isReadOnly);
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    return this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap((api) => {
          const apiHealthCheck = ApiHealthCheckFormComponent.HealthCheckFromFormGroup(this.healthCheckForm, false);
          this.updateEndpointsHealthCheckConfig(api.proxy?.groups);

          return this.apiService.update(api.id, {
            ...api,
            services: {
              ...api.services,
              healthCheck: apiHealthCheck,
            },
          });
        }),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  updateEndpointsHealthCheckConfig(groups: Proxy['groups']) {
    groups.forEach((group) => {
      group.endpoints?.forEach((endpoint) => {
        // If healthcheck is disabled, set inherit to false
        if (
          (endpoint.healthCheck?.inherit === undefined || endpoint.healthCheck?.inherit === true) &&
          endpoint.healthCheck?.enabled === false
        ) {
          endpoint.healthCheck = {
            inherit: false,
            enabled: false,
          };
        }
        // Enable healthcheck if inherit is true or not defined
        else if (endpoint.healthCheck?.inherit === undefined || endpoint.healthCheck?.inherit === true) {
          endpoint.healthCheck = {
            inherit: true,
          };
        }
      });
    });
  }
}
