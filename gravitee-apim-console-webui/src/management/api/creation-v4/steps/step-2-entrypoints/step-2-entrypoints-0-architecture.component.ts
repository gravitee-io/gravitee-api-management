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
import { Subject } from 'rxjs';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';

import { Step2Entrypoints1ListComponent } from './step-2-entrypoints-1-list.component';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ApiType } from '../../../../../entities/management-api-v2';
import { UTMTags, ApimFeature } from '../../../../../shared/components/gio-license/gio-license-data';

@Component({
  selector: 'step-2-entrypoints-0-architecture',
  templateUrl: './step-2-entrypoints-0-architecture.component.html',
  styleUrls: ['./step-2-entrypoints-0-architecture.component.scss', '../api-creation-steps-common.component.scss'],
})
export class Step2Entrypoints0ArchitectureComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private initialValue: { type: ApiType };

  public form: UntypedFormGroup;
  private licenseOptions = { feature: ApimFeature.APIM_EN_MESSAGE_REACTOR, context: UTMTags.API_CREATION_TRY_MESSAGE };

  public get shouldUpgrade$() {
    return this.licenseService?.isMissingFeature$(this.licenseOptions);
  }

  constructor(
    private readonly formBuilder: UntypedFormBuilder,
    private readonly stepService: ApiCreationStepService,
    private readonly confirmDialog: MatDialog,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.form = this.formBuilder.group({
      type: this.formBuilder.control(currentStepPayload.type ?? null, [Validators.required]),
    });

    this.initialValue = this.form.getRawValue();
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  goBack() {
    this.stepService.goToPreviousStep();
  }

  save() {
    const previousType = this.stepService.payload.type;
    const selectedType = this.form.value.type;

    if (previousType && selectedType !== previousType) {
      // When changing the type, all previously filled steps must be deleted to restart from scratch.
      this.confirmDialog
        .open<GioConfirmDialogComponent, GioConfirmDialogData, boolean>(GioConfirmDialogComponent, {
          data: {
            title: 'Are you sure?',
            content: 'By changing the architecture type, all previously entered data in Endpoints and Security steps will be reset.',
            confirmButton: 'Validate',
            cancelButton: 'Discard',
          },
        })
        .afterClosed()
        .subscribe((confirmed) => {
          if (confirmed) {
            this.stepService.removeAllNextSteps();
            this.form.value.type === 'PROXY' ? this.doSaveSync() : this.doSaveAsync();
          } else {
            this.form.setValue(this.initialValue);
          }
        });
      return;
    }
    this.form.value.type === 'PROXY' ? this.doSaveSync() : this.doSaveAsync();
  }

  private doSaveSync() {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      type: 'PROXY',
    }));
    this.stepService.goToNextStep({
      groupNumber: 2,
      component: Step2Entrypoints1ListComponent,
    });
  }

  private doSaveAsync() {
    this.stepService.validStep((previousPayload) => ({
      ...previousPayload,
      type: 'MESSAGE',
    }));
    this.stepService.goToNextStep({
      groupNumber: 2,
      component: Step2Entrypoints1ListComponent,
    });
  }

  public onRequestUpgrade() {
    this.licenseService.openDialog(this.licenseOptions);
  }
}
