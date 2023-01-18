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

import { UIRouterState } from '../../../../../../ajs-upgraded-providers';
import { ApiCreationStepService } from '../../services/api-creation-step.service';

@Component({
  selector: 'api-creation-v4-step-1',
  template: require('./api-creation-v4-step-1.component.html'),
  styles: [require('./api-creation-v4-step-1.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class ApiCreationV4Step1Component implements OnInit {
  public form: FormGroup;

  @Output()
  public exit = new EventEmitter<void>();

  constructor(
    @Inject(UIRouterState) readonly ajsState: StateService,
    private readonly formBuilder: FormBuilder,
    private readonly confirmDialog: MatDialog,
    private readonly stepService: ApiCreationStepService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.form = this.formBuilder.group({
      name: this.formBuilder.control(currentStepPayload?.name, [Validators.required]),
      version: this.formBuilder.control(currentStepPayload?.version, [Validators.required]),
      description: this.formBuilder.control(currentStepPayload?.description, [Validators.required]),
    });
    if (currentStepPayload && Object.keys(currentStepPayload).length > 0) {
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
    this.stepService.goToNextStep((previousPayload) => ({
      ...previousPayload,
      name: formValue.name,
      description: formValue.description,
      version: formValue.version,
    }));
  }
}
