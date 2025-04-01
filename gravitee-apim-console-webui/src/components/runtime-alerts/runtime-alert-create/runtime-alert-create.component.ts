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
import { Component, Inject, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { EMPTY, Subject } from 'rxjs';

import { GeneralFormValue } from './components/runtime-alert-create-general';
import { toNewAlertTriggerEntity } from './runtime-alert-create.adapter';

import { Constants } from '../../../entities/Constants';
import { Scope } from '../../../entities/alert';
import { Rule } from '../../../entities/alerts/rule.metrics';
import { AlertService } from '../../../services-ngx/alert.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';

@Component({
  selector: 'runtime-alert-create',
  templateUrl: './runtime-alert-create.component.html',
  styleUrls: ['./runtime-alert-create.component.scss'],
  standalone: false,
})
export class RuntimeAlertCreateComponent implements OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  protected referenceType: Scope = Scope[this.activatedRoute.snapshot.data.referenceType as keyof typeof Scope];
  protected referenceId: string;
  public alertForm: FormGroup;
  protected selectedRule: Rule;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    @Inject(Constants) public readonly constants: Constants,
    private readonly formBuilder: FormBuilder,
    private readonly alertService: AlertService,
    private readonly snackBarService: SnackBarService,
    private readonly router: Router,
  ) {
    switch (this.referenceType) {
      case Scope.API:
        this.referenceId = this.activatedRoute.snapshot.params.apiId;
        break;
      case Scope.APPLICATION:
        this.referenceId = this.activatedRoute.snapshot.queryParams.applicationId;
        break;
      case Scope.ENVIRONMENT:
        this.referenceId = this.constants.org.currentEnv.id;
        break;
    }

    this.alertForm = this.formBuilder.group({
      generalForm: [],
      timeframeForm: [],
      conditionsForm: [],
      filtersForm: [],
    });

    this.alertForm.controls.generalForm.valueChanges
      .pipe(
        tap((value: GeneralFormValue) => (this.selectedRule = value.rule)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  save() {
    return this.alertService
      .createAlert(this.referenceId, toNewAlertTriggerEntity(this.referenceId, Scope[this.referenceType], this.alertForm.getRawValue()))
      .pipe(
        tap(() => {
          this.snackBarService.success('Alert successfully created!');
        }),
        catchError(() => {
          this.snackBarService.error('Alert creation failed!');
          return EMPTY;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.router.navigate(['..'], { relativeTo: this.activatedRoute }));
  }
}
