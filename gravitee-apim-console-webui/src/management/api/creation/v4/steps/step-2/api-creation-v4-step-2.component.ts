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
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';

import { EntrypointService } from '../../../../../../services-ngx/entrypoint.service';
import { ApiCreationStepService } from '../../services/api-creation-step.service';

type EntrypointVM = {
  value: {
    id: string;
    name: string;
  };
  description: string;
  isEnterprise: boolean;
};

@Component({
  selector: 'api-creation-v4-step-2',
  template: require('./api-creation-v4-step-2.component.html'),
  styles: [require('./api-creation-v4-step-2.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class ApiCreationV4Step2Component implements OnInit, OnDestroy {
  private unsubscribe$: Subject<void> = new Subject<void>();

  public formGroup: FormGroup;

  public entrypoints: EntrypointVM[];

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly entrypointService: EntrypointService,
    private readonly matDialog: MatDialog,
    private readonly stepService: ApiCreationStepService,
  ) {}

  ngOnInit(): void {
    const currentStepPayload = this.stepService.payload;

    this.formGroup = this.formBuilder.group({
      selectedEntrypoints: this.formBuilder.control(currentStepPayload.selectedEntrypoints ?? [], [Validators.required]),
    });

    this.entrypointService
      .v4ListEntrypointPlugins()
      .pipe(
        takeUntil(this.unsubscribe$),
        map((entrypointPlugins) => entrypointPlugins.filter((entrypoint) => entrypoint.supportedApiType === 'async')),
      )
      .subscribe((entrypointPlugins) => {
        this.entrypoints = entrypointPlugins.map((entrypoint) => ({
          value: {
            id: entrypoint.id,
            name: entrypoint.name,
          },
          description: entrypoint.description,
          isEnterprise: entrypoint.id.endsWith('-advanced'),
        }));
      });
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.unsubscribe();
  }

  save(): void {
    const selectedEntrypoints = this.formGroup.getRawValue().selectedEntrypoints ?? [];

    this.stepService.goToNextStep((previousPayload) => ({ ...previousPayload, selectedEntrypoints }));
  }

  goBack(): void {
    this.stepService.goToPreviousStep();
  }

  onMoreInfoClick(event, entrypoint: EntrypointVM) {
    event.stopPropagation();
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: entrypoint.value.name,
          content: `${entrypoint.description} <br> 🚧 More information coming soon 🚧`,
          confirmButton: `Ok`,
        },
        role: 'alertdialog',
        id: 'moreInfoDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }
}
