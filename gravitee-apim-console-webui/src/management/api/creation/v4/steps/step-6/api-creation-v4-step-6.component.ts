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

import { Component } from '@angular/core';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { catchError, takeUntil, tap } from 'rxjs/operators';
import { MatDialog } from '@angular/material/dialog';
import { EMPTY, Subject } from 'rxjs';
import { kebabCase } from 'lodash';

import { ApiCreationStepService } from '../../services/api-creation-step.service';
import { ApiV4Service } from '../../../../../../services-ngx/api-v4.service';
import { SnackBarService } from '../../../../../../services-ngx/snack-bar.service';
import { fakeNewApiEntity } from '../../../../../../entities/api-v4/NewApiEntity.fixture';
import { HttpListener } from '../../../../../../entities/api-v4';

@Component({
  selector: 'api-creation-v4-step-6',
  template: require('./api-creation-v4-step-6.component.html'),
  styles: [require('./api-creation-v4-step-6.component.scss'), require('../api-creation-steps-common.component.scss')],
})
export class ApiCreationV4Step6Component {
  private unsubscribe$: Subject<void> = new Subject<void>();
  constructor(
    private readonly stepService: ApiCreationStepService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    private readonly apiV4Service: ApiV4Service,
  ) {}

  createApi(): void {
    const apiCreationPayload = this.stepService.payload;
    // eslint-disable-next-line
    console.info('API Creation Payload', apiCreationPayload);

    this.apiV4Service
      .create(
        // Note : WIP 🚧
        // Use the fakeNewApiEntity to create a new API temporarily
        // The real API creation will be done when we complete other api creation steps
        fakeNewApiEntity((api) => {
          const listener = api.listeners[0] as HttpListener;
          listener.paths = [{ path: `/fake/${kebabCase(apiCreationPayload.name + '-' + apiCreationPayload.version)}` }];
          return {
            ...api,
            name: apiCreationPayload.name,
          };
        }),
      )
      .pipe(
        takeUntil(this.unsubscribe$),
        tap(
          (api) => {
            // eslint-disable-next-line
            console.info('API created successfully', api);
            // TODO: add redirection when api details page work with v4
            this.snackBarService.success('API created successfully!');
          },
          catchError((err) => {
            this.snackBarService.error(err.error?.message ?? 'An error occurred while created the API.');
            return EMPTY;
          }),
        ),
      )
      .subscribe();
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
