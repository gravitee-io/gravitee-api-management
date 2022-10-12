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
import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { GioConfirmDialogComponent, GioConfirmDialogData } from '@gravitee/ui-particles-angular';
import { combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { catchError, filter, map, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Api } from '../../../entities/api';
import { Category } from '../../../entities/category/Category';
import { Constants } from '../../../entities/Constants';
import { ApiService } from '../../../services-ngx/api.service';
import { CategoryService } from '../../../services-ngx/category.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-portal-details',
  template: require('./api-portal-details.component.html'),
  styles: [require('./api-portal-details.component.scss')],
})
export class ApiPortalDetailsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public apiId: string;
  public apiDetailsForm: FormGroup;
  public initialApiDetailsFormValue: unknown;
  public labelsAutocompleteOptions: string[] = [];
  public apiCategories: Category[] = [];
  public apiOwner: string;
  public apiCreatedAt: number;
  public apiLastDeploymentAt: number;
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
  public canPromote = false;

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly categoryService: CategoryService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    @Inject('Constants') private readonly constants: Constants,
    private readonly matDialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.labelsAutocompleteOptions = this.constants.env?.settings?.api?.labelsDictionary ?? [];

    combineLatest([this.apiService.get(this.ajsStateParams.apiId), this.categoryService.list()])
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap(([api, categories]) =>
          combineLatest([isImgUrl(api.picture_url), isImgUrl(api.background_url)]).pipe(
            map(
              ([hasPictureImg, hasBackgroundImg]) =>
                [
                  {
                    ...api,
                    picture_url: hasPictureImg ? api.picture_url : null,
                    background_url: hasBackgroundImg ? api.background_url : null,
                  },
                  categories,
                ] as const,
            ),
          ),
        ),
        tap(([api, categories]) => {
          const isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']);

          this.apiId = api.id;
          this.apiCategories = categories;
          this.apiOwner = api.owner.displayName;
          this.apiCreatedAt = api.created_at;
          this.apiLastDeploymentAt = api.updated_at;

          this.dangerActions = {
            canAskForReview:
              this.constants.env?.settings?.apiReview?.enabled &&
              (api.workflow_state === 'DRAFT' || api.workflow_state === 'REQUEST_FOR_CHANGES' || !api.workflow_state),
            canStartApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflow_state || api.workflow_state === 'REVIEW_OK'))) &&
              api.state === 'STOPPED',
            canStopApi:
              (!this.constants.env?.settings?.apiReview?.enabled ||
                (this.constants.env?.settings?.apiReview?.enabled && (!api.workflow_state || api.workflow_state === 'REVIEW_OK'))) &&
              api.state === 'STARTED',

            canChangeApiLifecycle: this.canChangeApiLifecycle(api),
            canPublish: !api.lifecycle_state || api.lifecycle_state === 'CREATED' || api.lifecycle_state === 'UNPUBLISHED',
            canUnpublish: api.lifecycle_state === 'PUBLISHED',

            canChangeVisibilityToPublic: api.lifecycle_state !== 'DEPRECATED' && api.visibility === 'PRIVATE',
            canChangeVisibilityToPrivate: api.lifecycle_state !== 'DEPRECATED' && api.visibility === 'PUBLIC',
            canDeprecate: api.lifecycle_state !== 'DEPRECATED',
            canDelete: !(api.state === 'STARTED' || api.lifecycle_state === 'PUBLISHED'),
          };

          this.canPromote = this.dangerActions.canChangeApiLifecycle && api.lifecycle_state !== 'DEPRECATED';

          this.apiDetailsForm = new FormGroup({
            name: new FormControl(
              {
                value: api.name,
                disabled: isReadOnly,
              },
              [Validators.required],
            ),
            version: new FormControl(
              {
                value: api.version,
                disabled: isReadOnly,
              },
              [Validators.required],
            ),
            description: new FormControl(
              {
                value: api.description,
                disabled: isReadOnly,
              },
              [Validators.required],
            ),
            picture: new FormControl({
              value: api.picture_url ? [api.picture_url] : [],
              disabled: isReadOnly,
            }),
            background: new FormControl({
              value: api.background_url ? [api.background_url] : [],
              disabled: isReadOnly,
            }),
            labels: new FormControl({
              value: api.labels,
              disabled: isReadOnly,
            }),
            categories: new FormControl({
              value: api.categories,
              disabled: isReadOnly,
            }),
          });

          this.initialApiDetailsFormValue = this.apiDetailsForm.getRawValue();
        }),
      )
      .subscribe();
  }

  ngOnDestroy() {
    this.unsubscribe$.next(true);
    this.unsubscribe$.unsubscribe();
  }

  onSubmit() {
    const apiDetailsFormValue = this.apiDetailsForm.getRawValue();

    return this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) =>
          combineLatest([getBase64(apiDetailsFormValue.picture[0]), getBase64(apiDetailsFormValue.background[0])]).pipe(
            map(([picture, background]) => ({
              ...api,
              ...(picture !== null ? { picture: picture } : { picture_url: null, picture: null }),
              ...(background !== null ? { background: background } : { background_url: null, background: null }),
              name: apiDetailsFormValue.name,
              version: apiDetailsFormValue.version,
              description: apiDetailsFormValue.description,
              labels: apiDetailsFormValue.labels,
              categories: apiDetailsFormValue.categories,
            })),
          ),
        ),
        switchMap((api) => this.apiService.update(api)),
        tap(() => this.snackBarService.success('Configuration successfully saved!')),
        catchError((err) => {
          this.snackBarService.error(err.error?.message ?? err.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
      )
      .subscribe();
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
        switchMap(() => this.apiService.askForReview(this.apiId)),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
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
        switchMap(() => (state === 'START' ? this.apiService.start(this.apiId) : this.apiService.stop(this.apiId))),
        catchError(({ error }) => {
          this.snackBarService.error(error.message);
          return EMPTY;
        }),
        tap(() => this.ngOnInit()),
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
        switchMap(() => this.apiService.get(this.ajsStateParams.apiId)),
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
        tap(() => this.ngOnInit()),
        map(() => this.snackBarService.success(`The API has been ${actionLabel[lifecycleState].toLowerCase()} with success.`)),
      )
      .subscribe();
  }

  changeVisibility() {
    // TODO
  }

  delete() {
    // TODO
  }

  private canChangeApiLifecycle(api: Api): boolean {
    if (this.constants.env?.settings?.apiReview?.enabled) {
      return !api.workflow_state || api.workflow_state === 'REVIEW_OK';
    } else {
      return api.lifecycle_state === 'CREATED' || api.lifecycle_state === 'PUBLISHED' || api.lifecycle_state === 'UNPUBLISHED';
    }
  }
}

const isImgUrl = (url: string): Promise<boolean> => {
  const img = new Image();
  img.src = url;
  return new Promise((resolve) => {
    img.onerror = () => resolve(false);
    img.onload = () => resolve(true);
  });
};

function getBase64(file?: File): Observable<string | undefined | null> {
  if (!file) {
    // If no file, return null to remove it
    return of(null);
  }
  if (!(file instanceof Blob)) {
    // If file not changed, return undefined to keep it
    return of(undefined);
  }

  return new Observable((subscriber) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => subscriber.next(reader.result.toString());
    reader.onerror = (error) => subscriber.error(error);
    return () => reader.abort();
  });
}
