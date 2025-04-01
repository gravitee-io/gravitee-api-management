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
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { EMPTY, Subject } from 'rxjs';
import { catchError, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { onlyApiV1V2Filter, onlyApiV2Filter } from '../../../util/apiFilter.operator';
import { Failover } from '../../../entities/management-api-v2';

@Component({
  selector: 'api-failover',
  templateUrl: './api-failover.component.html',
  styleUrls: ['./api-failover.component.scss'],
  standalone: false,
})
export class ApiFailoverComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public failoverForm: UntypedFormGroup;
  public initialFailoverFormValue: Failover;

  public get enabled() {
    return this.failoverForm.get('enabled');
  }

  public get maxAttempts() {
    return this.failoverForm.get('maxAttempts');
  }

  public get retryTimeout() {
    return this.failoverForm.get('retryTimeout');
  }

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: UntypedFormBuilder,
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
          const isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || api.definitionContext?.origin === 'KUBERNETES';
          this.createForm(isReadOnly, api.proxy?.failover);
          this.setupDisablingFields();
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  private createForm(isReadOnly: boolean, failover?: Failover) {
    const isFailoverReadOnly = isReadOnly || !failover;

    this.failoverForm = this.formBuilder.group({
      enabled: [{ value: !!failover, disabled: isReadOnly }, []],
      maxAttempts: [{ value: failover?.maxAttempts ?? null, disabled: isFailoverReadOnly }, [Validators.required]],
      retryTimeout: [{ value: failover?.retryTimeout ?? null, disabled: isFailoverReadOnly }, [Validators.required]],
    });
    this.initialFailoverFormValue = this.failoverForm.getRawValue();
  }

  private setupDisablingFields() {
    const controlKeys = ['maxAttempts', 'retryTimeout'];
    this.enabled.valueChanges.pipe(takeUntil(this.unsubscribe$)).subscribe((checked) => {
      controlKeys.forEach((k) => {
        return checked ? this.failoverForm.get(k).enable() : this.failoverForm.get(k).disable();
      });
    });
  }

  onSubmit() {
    const { enabled, maxAttempts, retryTimeout } = this.failoverForm.getRawValue();
    return this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV2Filter(this.snackBarService),
        switchMap(({ proxy, ...api }) =>
          this.apiService.update(api.id, {
            ...api,
            proxy: {
              ...proxy,
              failover: enabled
                ? {
                    cases: ['TIMEOUT'],
                    maxAttempts,
                    retryTimeout,
                  }
                : undefined,
            },
          }),
        ),
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
}
