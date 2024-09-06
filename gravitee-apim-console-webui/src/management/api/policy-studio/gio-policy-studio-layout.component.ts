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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { StateParams, StateService } from '@uirouter/angularjs';
import { catchError, defaultIfEmpty, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { combineLatest, EMPTY, forkJoin, Observable, of, Subject } from 'rxjs';
import { GioLicenseService } from '@gravitee/ui-particles-angular';
import { isEqual } from 'lodash';

import { PolicyStudioService } from './policy-studio.service';
import { ApiDefinition, toApiDefinition, toApiPlansDefinition, toApiPlanV2, toApiV2 } from './models/ApiDefinition';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { AjsRootScope, UIRouterState, UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApimFeature, UTMTags } from '../../../shared/components/gio-license/gio-license-data';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { onlyApiV2Filter } from '../../../util/apiFilter.operator';
import { ApiV2, PlanV2 } from '../../../entities/management-api-v2';
import { ApiPlanV2Service } from '../../../services-ngx/api-plan-v2.service';

interface MenuItem {
  label: string;
  uiSref: Observable<string>;
  license?: any;
  notAllowed$?: Observable<boolean>;
}

@Component({
  selector: 'gio-policy-studio-layout',
  template: require('./gio-policy-studio-layout.component.html'),
  styles: [require('./gio-policy-studio-layout.component.scss')],
})
export class GioPolicyStudioLayoutComponent implements OnInit, OnDestroy {
  private unsubscribe$ = new Subject<void>();
  policyStudioMenu: MenuItem[] = [
    { label: 'Design', uiSref: of('.design') },
    { label: 'Configuration', uiSref: of('.config') },
  ];
  activeLink = this.policyStudioMenu[0];
  apiDefinition: ApiDefinition;
  isDirty: boolean;

  constructor(
    readonly policyStudioService: PolicyStudioService,
    readonly gioLicenseService: GioLicenseService,
    readonly snackBarService: SnackBarService,
    readonly apiService: ApiV2Service,
    readonly apiPlanService: ApiPlanV2Service,
    readonly permissionService: GioPermissionService,
    @Inject(UIRouterState) readonly ajsStateService: StateService,
    @Inject(UIRouterStateParams) readonly ajsStateParams: StateParams,
    @Inject(AjsRootScope) readonly ajsRootScope,
  ) {}

  ngOnInit(): void {
    this.isDirty = false;
    const debugLicense = { feature: ApimFeature.APIM_DEBUG_MODE, context: UTMTags.CONTEXT_API_V2 };
    const notAllowed$ = this.gioLicenseService.isMissingFeature$(debugLicense);

    this.policyStudioMenu = this.policyStudioMenu.filter((i) => i.label !== 'Debug');
    this.policyStudioMenu.push({
      label: 'Debug',
      uiSref: notAllowed$.pipe(map((notAllowed) => (notAllowed ? null : '.debug'))),
      license: debugLicense,
      notAllowed$,
    });

    combineLatest([
      this.apiService.get(this.ajsStateParams.apiId).pipe(onlyApiV2Filter(this.snackBarService)),
      this.apiPlanService.list(this.ajsStateParams.apiId, undefined, ['PUBLISHED', 'DEPRECATED'], undefined, 1, 9999),
    ])
      .pipe(
        tap(([api, plansResponse]) => {
          this.policyStudioService.setApiDefinition(this.toApiDefinition(api, plansResponse.data as PlanV2[]));
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
    this.policyStudioService.getApiDefinitionToSave$().pipe(takeUntil(this.unsubscribe$)).subscribe(this.onDefinitionChange.bind(this));
  }

  onDefinitionChange(apiDefinition: ApiDefinition) {
    this.isDirty = true;
    this.apiDefinition = apiDefinition;
  }

  onSubmit() {
    const updatePlans$ = this.apiPlanService
      .list(this.ajsStateParams.apiId, undefined, ['PUBLISHED', 'DEPRECATED'], undefined, 1, 9999)
      .pipe(
        switchMap((plansResponse) => {
          const plans = plansResponse.data as PlanV2[];

          const planToUpdate = plans
            .map((plan) => {
              const planDefinition = this.apiDefinition.plans.find((p) => p.id === plan.id);
              if (planDefinition) {
                const planToUpdate = toApiPlanV2(planDefinition, plan);

                return isEqual(plan, planToUpdate) ? null : planToUpdate;
              }
              return null;
            })
            .filter((p) => p !== null);

          return forkJoin(...planToUpdate.map((plan) => this.apiPlanService.update(this.ajsStateParams.apiId, plan.id, plan)));
        }),
        defaultIfEmpty(null),
      );

    const updateApi$ = this.apiService.get(this.apiDefinition.id).pipe(
      onlyApiV2Filter(this.snackBarService),
      switchMap((api) => this.apiService.update(api.id, toApiV2(this.apiDefinition, api))),
    );

    combineLatest([updatePlans$, updateApi$])
      .pipe(
        tap(() => {
          this.ngOnInit();
          this.snackBarService.success('Configuration successfully saved!');
        }),
        catchError(({ error }) => {
          this.snackBarService.error(error?.message ?? 'An error occurred while saving the configuration.');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  onReset() {
    this.policyStudioService.reset();
    this.ajsStateService.reload();
  }

  private toApiDefinition(api: ApiV2, _plans: PlanV2[]): ApiDefinition {
    const plans = _plans && this.permissionService.hasAnyMatching(['api-plan-r', 'api-plan-u']) ? _plans : [];

    const apiDefinition = toApiDefinition(api);
    const plansDefinition = toApiPlansDefinition(plans);
    return { ...apiDefinition, plans: plansDefinition };
  }

  ngOnDestroy(): void {
    this.policyStudioService.reset();
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }
}
