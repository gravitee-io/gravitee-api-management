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
import { Component, Inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormControl, FormGroup } from '@angular/forms';

import { Scope } from '../../../entities/alert';
import { Constants } from '../../../entities/Constants';

@Component({
  selector: 'runtime-alert-create',
  templateUrl: './runtime-alert-create.component.html',
  styleUrls: ['./runtime-alert-create.component.scss'],
})
export class RuntimeAlertCreateComponent {
  public referenceType: Scope = this.activatedRoute.snapshot.data.referenceType;
  public referenceId: string;
  public alertForm: FormGroup;

  constructor(private readonly activatedRoute: ActivatedRoute, @Inject(Constants) public readonly constants: Constants) {
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

    this.alertForm = new FormGroup({
      generalForm: new FormControl(),
      timeframeForm: new FormControl(),
    });
  }
}
