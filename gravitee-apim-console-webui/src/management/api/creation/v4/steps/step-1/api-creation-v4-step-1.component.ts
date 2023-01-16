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

import { Component, EventEmitter, Inject, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { StateService } from '@uirouter/core';

import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { API_CREATION_PAYLOAD, ApiCreationStepperService } from '../../models/ApiCreationStepperService';
import { UIRouterState } from '../../../../../../ajs-upgraded-providers';

@Component({
  selector: 'api-creation-v4-step-1',
  template: require('./api-creation-v4-step-1.component.html'),
  styles: [require('./api-creation-v4-step-1.component.scss'), require('../../api-creation-v4.component.scss')],
})
export class ApiCreationV4Step1Component implements OnInit {
  public form: FormGroup;

  @Output()
  public exit = new EventEmitter<void>();

  constructor(
    @Inject(UIRouterState) readonly ajsState: StateService,
    private readonly formBuilder: FormBuilder,
    private readonly confirmDialog: MatDialog,
    private readonly stepper: ApiCreationStepperService,
    @Inject(API_CREATION_PAYLOAD) readonly currentStepPayload: ApiCreationPayload,
  ) {}

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      name: this.formBuilder.control(this.currentStepPayload?.name, [Validators.required]),
      version: this.formBuilder.control(this.currentStepPayload?.version, [Validators.required]),
      description: this.formBuilder.control(this.currentStepPayload?.description, [Validators.required]),
    });
    if (this.currentStepPayload && Object.keys(this.currentStepPayload).length > 0) {
      this.form.markAsDirty();
    }
  }

  onExit() {
    if (this.form.dirty) {
      this.confirmDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Are you sure?',
            content: 'You still need to create your API. If you leave this page, you will lose any info you added.',
            confirmButton: 'Discard changes',
            cancelButton: 'Keep creating',
          },
        })
        .afterClosed()
        .subscribe((confirmed) => {
          if (confirmed) {
            this.ajsState.go('management.apis.new');
          }
        });
      return;
    }
    this.ajsState.go('management.apis.new');
  }

  save() {
    const formValue = this.form.getRawValue();
    this.stepper.goToNextStep({ name: formValue.name, description: formValue.description, version: formValue.version });
  }
}
