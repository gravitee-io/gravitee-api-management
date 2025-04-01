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
import { FormControl, FormGroup, UntypedFormBuilder, Validators } from '@angular/forms';
import { EMPTY, Observable, Subject } from 'rxjs';
import { catchError, filter, switchMap, takeUntil, tap } from 'rxjs/operators';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';

import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';
import { ApiV2Service } from '../../../services-ngx/api-v2.service';
import { onlyApiV4Filter } from '../../../util/apiFilter.operator';
import { ApiV4 } from '../../../entities/management-api-v2';
import { Failover } from '../../../entities/management-api-v2/api/v4/failover';
import {
  GioInformationDialogComponent,
  GioConnectorDialogData,
} from '../component/gio-information-dialog/gio-information-dialog.component';

export type FailoverForm = {
  enabled: FormControl<boolean>;
  maxRetries: FormControl<number>;
  slowCallDuration: FormControl<number>;
  openStateDuration: FormControl<number>;
  maxFailures: FormControl<number>;
  perSubscription: FormControl<boolean>;
};

@Component({
  selector: 'api-failover-v4',
  templateUrl: './api-failover-v4.component.html',
  styleUrls: ['./api-failover-v4.component.scss'],
  standalone: false,
})
export class ApiFailoverV4Component implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public failoverForm: FormGroup<FailoverForm>;
  public initialFailoverFormValue: Failover;
  public hasKafkaEndpointsGroup = false;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly apiService: ApiV2Service,
    private readonly snackBarService: SnackBarService,
    private readonly permissionService: GioPermissionService,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.apiService
      .get(this.activatedRoute.snapshot.params.apiId)
      .pipe(
        onlyApiV4Filter(this.snackBarService),
        tap((api: ApiV4) => {
          const isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']) || api.definitionContext?.origin === 'KUBERNETES';
          this.hasKafkaEndpointsGroup = api?.endpointGroups?.some((endpointGroup) => endpointGroup.type === 'kafka');
          this.createForm(isReadOnly, api?.failover);
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
    const isFailoverReadOnly = isReadOnly || !failover?.enabled;

    this.failoverForm = this.formBuilder.group({
      enabled: [{ value: failover?.enabled, disabled: isReadOnly }, []],
      maxRetries: [
        {
          value: failover?.maxRetries ?? 2,
          disabled: isFailoverReadOnly,
        },
        [Validators.required, Validators.min(0)],
      ],
      slowCallDuration: [
        { value: failover?.slowCallDuration ?? 2000, disabled: isFailoverReadOnly },
        [Validators.required, Validators.min(50)],
      ],
      openStateDuration: [
        { value: failover?.openStateDuration ?? 10000, disabled: isFailoverReadOnly },
        [Validators.required, Validators.min(500)],
      ],
      maxFailures: [{ value: failover?.maxFailures ?? 5, disabled: isFailoverReadOnly }, [Validators.required, Validators.min(1)]],
      perSubscription: [
        {
          value: failover?.perSubscription ?? true,
          disabled: isFailoverReadOnly,
        },
        [Validators.required],
      ],
    });
    this.initialFailoverFormValue = this.failoverForm.getRawValue();
  }

  private setupDisablingFields() {
    const controlKeys = ['maxRetries', 'slowCallDuration', 'openStateDuration', 'maxFailures', 'perSubscription'];
    this.failoverForm
      .get('enabled')
      .valueChanges.pipe(takeUntil(this.unsubscribe$))
      .subscribe((checked) => {
        controlKeys.forEach((k) => {
          return checked ? this.failoverForm.get(k).enable() : this.failoverForm.get(k).disable();
        });
      });
  }

  onSubmit() {
    const { enabled, maxRetries, slowCallDuration, openStateDuration, maxFailures, perSubscription } = this.failoverForm.getRawValue();
    let confirmUpdate$: Observable<boolean>;
    if (enabled && !perSubscription) {
      confirmUpdate$ = this.matDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
          width: '500px',
          data: {
            title: `Update API`,
            content: `Are you sure you want to use a global circuit breaker? It may impact your consumers.`,
            confirmButton: `Yes, update it`,
          },
          role: 'alertdialog',
          id: 'failoverGlobalCircuitBreakerDialog',
        })
        .afterClosed();
    } else {
      confirmUpdate$ = new Observable((subscriber) => {
        subscriber.next(true);
        subscriber.complete();
      });
    }
    return confirmUpdate$
      .pipe(
        filter((confirm) => {
          return confirm === true;
        }),
        switchMap(() => this.apiService.get(this.activatedRoute.snapshot.params.apiId)),
        onlyApiV4Filter(this.snackBarService),
        switchMap(({ ...api }) =>
          this.apiService.update(api.id, {
            ...api,
            failover: {
              enabled,
              maxRetries,
              slowCallDuration,
              openStateDuration,
              maxFailures,
              perSubscription,
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

  onMoreInfoClick(event: MouseEvent) {
    event.stopPropagation();
    this.matDialog
      .open<GioInformationDialogComponent, GioConnectorDialogData, boolean>(GioInformationDialogComponent, {
        data: {
          name: 'Failover',
          information: {
            description:
              'Circuit breaker is a mechanism used to detect failures and encapsulates the logic of preventing a failure from constantly recurring. In the case of failover, circuit breaker records the slow calls to the server as failures. Once the failures reach a certain threshold, the circuit breaker trips and all further calls to the circuit breaker return with an error, without a new request to the remote server being made at all. After a certain amount of time in open state, the circuit breaker will try to reset by trying a call to see if the problem is fixed, this state is the half-open state. ',
            schemaImg: 'assets/failover-circuit-breaker.png',
          },
        },
        role: 'alertdialog',
        id: 'moreInfoDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }
}
