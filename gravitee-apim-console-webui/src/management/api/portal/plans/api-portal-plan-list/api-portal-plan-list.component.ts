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
import { Component, Inject, OnDestroy, OnInit } from "@angular/core";
import { catchError, takeUntil, tap } from "rxjs/operators";
import { EMPTY, Subject } from 'rxjs';
import { StateService } from "@uirouter/core";

import { UIRouterState, UIRouterStateParams } from "../../../../../ajs-upgraded-providers";
import { PlanService } from "../../../../../services-ngx/plan.service";
import { API_PLAN_STATUS, ApiPlan, ApiPlanStatus } from "../../../../../entities/api";
import { SnackBarService } from "../../../../../services-ngx/snack-bar.service";

export type PlansTableDS = {
  id: string;
  name: string;
  security: string;
  status: string;
}[];

@Component({
  selector: 'api-portal-plan-list',
  template: require('./api-portal-plan-list.component.html'),
  styles: [require('./api-portal-plan-list.component.scss')],
})
export class ApiPortalPlanListComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public displayedColumns = ['name', 'security', 'status', 'tags', 'actions'];
  public plansTableDS: PlansTableDS = [];
  public isLoadingData = true;
  public apiPlanStatus = API_PLAN_STATUS;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly plansService: PlanService,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.searchPlansByStatus(this.ajsStateParams.status ?? 'PUBLISHED');
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  public searchPlansByStatus(status: ApiPlanStatus): void {
    this.plansService
      .getApiPlans(this.ajsStateParams.apiId, status)
      .pipe(
        takeUntil(this.unsubscribe$),
        tap((plans) => {
          this.ajsState.go('.', { status }, { notify: false });
          this.plansTableDS = this.toPlansTableDS(plans);
          this.isLoadingData = false;
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
      )
      .subscribe();
  }

  private toPlansTableDS(plans: ApiPlan[]): PlansTableDS {
    return !!plans && plans.length > 0
      ? plans.map((plan) => ({
          id: plan.id,
          name: plan.name,
          security: plan.security,
          status: plan.status,
          tags: plan.tags?.join(', '),
        }))
      : [];
  }
}
