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
import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { catchError, takeUntil, tap } from "rxjs/operators";
import { EMPTY, Subject } from 'rxjs';
import { GioJsonSchema } from "@gravitee/ui-particles-angular";

import { GeneralFormValue } from './components/runtime-alert-create-general';
import { toNewAlertTriggerEntity } from './runtime-alert-create.adapter';

import { Constants } from '../../../entities/Constants';
import { Scope } from '../../../entities/alert';
import { Rule } from '../../../entities/alerts/rule.metrics';
import { AlertService } from '../../../services-ngx/alert.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { AlertSeverity, AlertTriggerEntity } from "../../../entities/alerts/alertTriggerEntity";

@Component({
  selector: 'runtime-alert-create',
  templateUrl: './runtime-alert-create.component.html',
  styleUrls: ['./runtime-alert-create.component.scss'],
  standalone: false,
})
export class RuntimeAlertCreateComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  protected referenceType: Scope = Scope[this.activatedRoute.snapshot.data.referenceType as keyof typeof Scope];
  protected referenceId: string;
  public selectedRule: Rule;
  public alertForm: FormGroup;
  public schema: GioJsonSchema;

  public isLoading = true;

  public alertId = this.activatedRoute.snapshot.params.alertId;
  public isUpdate: boolean = !!this.alertId;
  public alertToUpdate: AlertTriggerEntity = null;

  constructor(
    @Inject(Constants) public readonly constants: Constants,
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: FormBuilder,
    private readonly alertService: AlertService,
    private readonly snackBarService: SnackBarService,
    private readonly router: Router,
    private readonly changeDetectorRef: ChangeDetectorRef,
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
      generalForm: this.formBuilder.group({
          name: this.formBuilder.control<string>(null, [Validators.required]),
          enabled: this.formBuilder.control<boolean>(false),
          rule: this.formBuilder.control<Rule>(null, [Validators.required]),
          severity: this.formBuilder.control<AlertSeverity>('INFO', [Validators.required, Validators.max(256)]),
          description: this.formBuilder.control<string>(null),
      }),
      timeframeForm: [],
      conditionsForm: this.formBuilder.group({}),
      filtersForm: [],
      notificationsForm: this.formBuilder.array([]),
      dampeningForm: this.formBuilder.group({}),
    });

    this.alertForm.controls?.generalForm?.valueChanges
      .pipe(
        tap((value: GeneralFormValue) => (this.selectedRule = value.rule)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }

  ngOnInit() {
    if(this.isUpdate) {
      this.alertService.getAlert(this.activatedRoute.snapshot.params.apiId, this.alertId)
        .subscribe({
          next: (alert) => {
            this.alertToUpdate = alert;
            this.changeDetectorRef.detectChanges();
            this.isLoading = false;
          }
        })
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  save() {
    if(this.isUpdate) {

    } else {
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

  show() {
    console.log('SHOW: ', this.alertForm.value);
  }

}
