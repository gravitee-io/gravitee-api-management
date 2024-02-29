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
import { Component, Inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup } from '@angular/forms';
import { takeUntil, tap } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { GeneralFormValue } from './components/runtime-alert-create-general';

import { Constants } from '../../../entities/Constants';
import { Scope } from '../../../entities/alert';
import { Rule } from '../../../entities/alerts/rule.metrics';

@Component({
  selector: 'runtime-alert-create',
  templateUrl: './runtime-alert-create.component.html',
  styleUrls: ['./runtime-alert-create.component.scss'],
})
export class RuntimeAlertCreateComponent implements OnInit {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();
  public referenceType: Scope = Scope[this.activatedRoute.snapshot.data.referenceType as keyof typeof Scope];
  public referenceId: string;
  public alertForm: FormGroup;
  public selectedRule: Rule;

  constructor(
    private readonly activatedRoute: ActivatedRoute,
    @Inject(Constants) public readonly constants: Constants,
    private readonly formBuilder: FormBuilder,
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
  }

  ngOnInit(): void {
    this.alertForm.controls.generalForm.valueChanges
      .pipe(
        tap((value: GeneralFormValue) => (this.selectedRule = value.rule)),
        takeUntil(this.unsubscribe$),
      )
      .subscribe();
  }
}
