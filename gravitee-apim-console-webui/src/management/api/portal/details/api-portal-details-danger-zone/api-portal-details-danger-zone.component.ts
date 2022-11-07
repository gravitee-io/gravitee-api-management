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

import { Component, EventEmitter, Inject, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import {
  GioConfirmDialogComponent,
  GioConfirmDialogData,
  GioConfirmAndValidateDialogComponent,
  GioConfirmAndValidateDialogData,
} from '@gravitee/ui-particles-angular';
import { StateService } from '@uirouter/core';
import { EMPTY, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterState } from '../../../../../ajs-upgraded-providers';
import { Api } from '../../../../../entities/api';
import { Constants } from '../../../../../entities/Constants';
import { ApiService } from '../../../../../services-ngx/api.service';
import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';

@Component({
  selector: 'api-portal-details-danger-zone',
  template: require('./api-portal-details-danger-zone.component.html'),
  styles: [require('./api-portal-details-danger-zone.component.scss')],
})
export class ApiPortalDetailsDangerZoneComponent implements OnChanges, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  @Input()
  public api: Api;

  // When some actions are done, we need to reload the details to keep the component up to date
  @Output()
  public reloadDetails = new EventEmitter<void>();

  public dangerActions = {
    canAskForReview: false,
    canStartApi: false,
    canStopApi: false,
    canChangeApiLifecycle: false,
    canPublish: false,
    canUnpublish: false,
    canChangeVisibilityToPublic: false,
    canChangeVisibilityToPrivate: false,
    canDeprecate: false,
    canDelete: false,
  };
  public isReadOnly = false;

  constructor(
    @Inject(UIRouterState) private readonly ajsState: StateService,
    private readonly apiService: ApiService,
    private readonly matDialog: MatDialog,
    private readonly snackBarService: SnackBarService,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.api) {
      this.isReadOnly = this.api.definition_context?.origin === 'kubernetes';

      this.dangerActions = {
        canAskForReview:
          this.constants.env?.settings?.apiReview?.enabled &&
          (this.api.workflow_state === 'DRAFT' || this.api.workflow_state === 'REQUEST_FOR_CHANGES' || !this.api.workflow_state),
        canStartApi:
          (!this.constants.env?.settings?.apiReview?.enabled ||
            (this.constants.env?.settings?.apiReview?.enabled && (!this.api.workflow_state || this.api.workflow_state === 'REVIEW_OK'))) &&
          this.api.state === 'STOPPED',
        canStopApi:
          (!this.constants.env?.settings?.apiReview?.enabled ||
            (this.constants.env?.settings?.apiReview?.enabled && (!this.api.workflow_state || this.api.workflow_state === 'REVIEW_OK'))) &&
          this.api.state === 'STARTED',

        canChangeApiLifecycle: this.canChangeApiLifecycle(this.api),
        canPublish: !this.api.lifecycle_state || this.api.lifecycle_state === 'CREATED' || this.api.lifecycle_state === 'UNPUBLISHED',
        canUnpublish: this.api.lifecycle_state === 'PUBLISHED',

        canChangeVisibilityToPublic: this.api.lifecycle_state !== 'DEPRECATED' && this.api.visibility === 'PRIVATE',
        canChangeVisibilityToPrivate: this.api.lifecycle_state !== 'DEPRECATED' && this.api.visibility === 'PUBLIC',
        canDeprecate: this.api.lifecycle_state !== 'DEPRECATED',
        canDelete: !(this.api.state === 'STARTED' || this.api.lifecycle_state === 'PUBLISHED'),
      };
    }
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  askForReview() {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: 'Review API',
          content: `Are you sure you want to ask for a review of the API?`,
          confirmButton: 'Ask for review',
        },
        role: 'alertdialog',
        id: 'reviewApiDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.askForReview(this.api.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.reloadDetails.emit()),
        map(() => this.snackBarService.success(`Review has been asked.`)),
      )
      .subscribe();
  }

  changeLifecycle(state: 'START' | 'STOP') {
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `${state === 'START' ? 'Start' : 'Stop'} API`,
          content: `Are you sure you want to ${state === 'START' ? 'start' : 'stop'} the API?`,
          confirmButton: `${state === 'START' ? 'Start' : 'Stop'}`,
        },
        role: 'alertdialog',
        id: 'lifecycleDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => (state === 'START' ? this.apiService.start(this.api.id) : this.apiService.stop(this.api.id))),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.reloadDetails.emit()),
        map(() => this.snackBarService.success(`The API has been ${state === 'START' ? 'started' : 'stopped'} with success.`)),
      )
      .subscribe();
  }

  changeApiLifecycle(lifecycleState: 'PUBLISHED' | 'UNPUBLISHED' | 'DEPRECATED') {
    const actionLabel = {
      PUBLISHED: 'Publish',
      UNPUBLISHED: 'Unpublish',
      DEPRECATED: 'Deprecate',
    };
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `${actionLabel[lifecycleState]} API`,
          content: `Are you sure you want to ${actionLabel[lifecycleState].toLowerCase()} the API?`,
          confirmButton: `${actionLabel[lifecycleState]}`,
        },
        role: 'alertdialog',
        id: 'apiLifecycleDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.api.id)),
        switchMap((api) =>
          this.apiService.update({
            ...api,
            lifecycle_state: lifecycleState,
          }),
        ),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.reloadDetails.emit()),
        map(() => this.snackBarService.success(`The API has been ${actionLabel[lifecycleState].toLowerCase()} with success.`)),
      )
      .subscribe();
  }

  changeVisibility(visibility: 'PUBLIC' | 'PRIVATE') {
    const actionLabel = {
      PUBLIC: 'Make Public',
      PRIVATE: 'Make Private',
    };
    this.matDialog
      .open<GioConfirmDialogComponent, GioConfirmDialogData>(GioConfirmDialogComponent, {
        width: '500px',
        data: {
          title: `Change visibility`,
          content: `Are you sure you want to ${actionLabel[visibility].toLowerCase()} the API?`,
          confirmButton: `${actionLabel[visibility]}`,
        },
        role: 'alertdialog',
        id: 'apiLifecycleDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.get(this.api.id)),
        switchMap((api) =>
          this.apiService.update({
            ...api,
            visibility: visibility,
          }),
        ),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.reloadDetails.emit()),
        map(() => this.snackBarService.success(`The API has been ${actionLabel[visibility]} with success.`)),
      )
      .subscribe();
  }

  delete() {
    this.matDialog
      .open<GioConfirmAndValidateDialogComponent, GioConfirmAndValidateDialogData>(GioConfirmAndValidateDialogComponent, {
        width: '500px',
        data: {
          title: `Delete API`,
          content: `Are you sure you want to delete the API?`,
          confirmButton: `Yes, delete it`,
          validationMessage: `Please, type in the name of the api <code>${this.api.name}</code> to confirm.`,
          validationValue: this.api.name,
          warning: `This operation is irreversible.`,
        },
        role: 'alertdialog',
        id: 'apiDeleteDialog',
      })
      .afterClosed()
      .pipe(
        takeUntil(this.unsubscribe$),
        filter((confirm) => confirm === true),
        switchMap(() => this.apiService.delete(this.api.id)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        map(() => this.snackBarService.success(`The API has been deleted.`)),
      )
      .subscribe(() => {
        this.ajsState.go('management.apis.ng-list');
      });
  }

  private canChangeApiLifecycle(api: Api): boolean {
    if (this.constants.env?.settings?.apiReview?.enabled) {
      return !api.workflow_state || api.workflow_state === 'REVIEW_OK';
    } else {
      return api.lifecycle_state === 'CREATED' || api.lifecycle_state === 'PUBLISHED' || api.lifecycle_state === 'UNPUBLISHED';
    }
  }
}
