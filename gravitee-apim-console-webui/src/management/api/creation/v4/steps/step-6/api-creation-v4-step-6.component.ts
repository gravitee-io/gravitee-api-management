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
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { takeUntil } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { Subject } from 'rxjs';

import { ApiCreationPayload } from '../../models/ApiCreationPayload';
import { API_CREATION_PAYLOAD } from '../../models/ApiCreationStepperService';

@Component({
  selector: 'api-creation-v4-step-6',
  template: require('./api-creation-v4-step-6.component.html'),
  styles: [require('./api-creation-v4-step-6.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class ApiCreationV4Step6Component {
  private unsubscribe$: Subject<void> = new Subject<void>();
  constructor(@Inject(API_CREATION_PAYLOAD) readonly currentStepPayload: ApiCreationPayload, private readonly matDialog: MatDialog) {}

  createApi(): void {
    // TODO: send info to correct endpoint to create, not publish, the new API
  }

  deployApi(): void {
    // TODO: send info to correct endpoint to create and publish the new API
  }

  onChangeStepInfo(stepNumber: number) {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `Change information`,
          content: `🚧 Going back to Step ${stepNumber} is under construction 🚧`,
          confirmButton: `Ok`,
        },
        role: 'alertdialog',
        id: 'changeStepInfoDialog',
      })
      .afterClosed()
      .pipe(takeUntil(this.unsubscribe$))
      .subscribe();
  }
}
