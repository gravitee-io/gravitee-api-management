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
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject, Subscription } from 'rxjs';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { StateService } from '@uirouter/core';

import { UIRouterState, UIRouterStateParams } from '../../../../../../ajs-upgraded-providers';
import { ApiService } from '../../../../../../services-ngx/api.service';
import { Api } from '../../../../../../entities/api';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-proxy-group-edit',
  template: require('./api-proxy-group-edit.component.html'),
  styles: [require('./api-proxy-group-edit.component.scss')],
})
export class ApiProxyGroupEditComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public apiId: string;
  public groupName: string;
  public api: Api;
  public generalForm: FormGroup;
  public groupForm: FormGroup;
  public initialGroupFormValue: unknown;
  public isReadOnly: boolean;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly formBuilder: FormBuilder,
    private readonly apiService: ApiService,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit(): void {
    this.apiId = this.ajsStateParams.apiId;
    this.apiService
      .get(this.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        map((api) => {
          this.api = api;
          this.initForms();
        }),
      )
      .subscribe();
  }

  public ngOnDestroy(): void {
    this.unsubscribe$.next(true);
    this.unsubscribe$.complete();
  }

  public onSubmit(): Subscription {
    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) => {
          const groupIndex = api.proxy.groups.findIndex((group) => group.name === this.ajsStateParams.groupName);

          const updatedGroup = {
            ...api.proxy.groups[groupIndex],
            name: this.generalForm.get('name').value,
            load_balancing: {
              type: this.generalForm.get('lb').value,
            },
          };
          api.proxy.groups.splice(groupIndex, 1, updatedGroup);

          return this.apiService.update(api);
        }),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ajsState.go('management.apis.detail.proxy.ng-endpoints', { apiId: this.ajsStateParams.apiId })),
      )
      .subscribe();
  }

  private initForms(): void {
    const group = this.api.proxy.groups.find((group) => group.name === this.ajsStateParams.groupName);

    this.generalForm = this.formBuilder.group({
      name: [group.name ?? '', [Validators.required, Validators.pattern(/^[^:]*$/)]],
      lb: [group.load_balancing.type ?? null],
    });

    this.groupForm = this.formBuilder.group({
      general: this.generalForm,
    });

    this.initialGroupFormValue = this.groupForm.getRawValue();
  }
}
