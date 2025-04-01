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

import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { GioConfirmDialogComponent, GioConfirmDialogData, GioLicenseService } from '@gravitee/ui-particles-angular';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { Step2Entrypoints0ArchitectureComponent } from '../step-2-entrypoints/step-2-entrypoints-0-architecture.component';
import { Step2Entrypoints1ListComponent } from '../step-2-entrypoints/step-2-entrypoints-1-list.component';

@Component({
  selector: 'step-1-api-details',
  templateUrl: './step-1-api-details.component.html',
  styleUrls: ['./step-1-api-details.component.scss', '../api-creation-steps-common.component.scss'],
  standalone: false,
})
export class Step1ApiDetailsComponent implements OnInit {
  private unsubscribe$: Subject<void> = new Subject<void>();
  private isOEM: boolean;

  public form: UntypedFormGroup;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly formBuilder: UntypedFormBuilder,
    private readonly matDialog: MatDialog,
    private readonly stepService: ApiCreationStepService,
    private readonly licenseService: GioLicenseService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.form = this.formBuilder.group({
      name: this.formBuilder.control(currentStepPayload?.name, [Validators.required]),
      version: this.formBuilder.control(currentStepPayload?.version, [Validators.required]),
      description: this.formBuilder.control(currentStepPayload?.description),
    });
    if (currentStepPayload && Object.keys(currentStepPayload).length > 0) {
      this.form.markAsDirty();
    }

    this.licenseService
      .isOEM$()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe((isOEM) => (this.isOEM = isOEM));
  }

  onExit() {
    if (this.form.dirty) {
      this.matDialog
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
            this.router.navigate(['..'], { relativeTo: this.activatedRoute });
          }
        });
      return;
    }
    this.router.navigate(['..'], { relativeTo: this.activatedRoute });
  }

  save() {
    const formValue = this.form.getRawValue();
    const apiDetailsValue = { name: formValue.name, description: formValue.description ?? '', version: formValue.version };

    if (this.isOEM) {
      this.stepService.validStep((previousPayload) => ({
        ...previousPayload,
        ...apiDetailsValue,
        type: 'PROXY',
      }));

      this.stepService.goToNextStep({
        groupNumber: 2,
        component: Step2Entrypoints1ListComponent,
      });
    } else {
      this.stepService.validStep((previousPayload) => ({
        ...previousPayload,
        ...apiDetailsValue,
      }));

      this.stepService.goToNextStep({
        groupNumber: 2,
        component: Step2Entrypoints0ArchitectureComponent,
      });
    }
  }
}
