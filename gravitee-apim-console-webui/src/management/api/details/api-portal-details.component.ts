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
import { combineLatest, EMPTY, Observable, of, Subject } from 'rxjs';
import { catchError, map, switchMap, takeUntil, tap } from 'rxjs/operators';

import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { Constants } from '../../../entities/Constants';
import { ApiService } from '../../../services-ngx/api.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { GioPermissionService } from '../../../shared/components/gio-permission/gio-permission.service';

@Component({
  selector: 'api-portal-details',
  template: require('./api-portal-details.component.html'),
  styles: [require('./api-portal-details.component.scss')],
})
export class ApiPortalDetailsComponent implements OnInit, OnDestroy {
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  public apiDetailsForm: FormGroup;
  public initialApiDetailsFormValue: unknown;
  public labelsAutocompleteOptions = [];

  constructor(
    @Inject(UIRouterStateParams) private readonly ajsStateParams,
    private readonly apiService: ApiService,
    private readonly permissionService: GioPermissionService,
    private readonly snackBarService: SnackBarService,
    @Inject('Constants') private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.labelsAutocompleteOptions = this.constants.env?.settings?.api?.labelsDictionary ?? [];

    this.apiService
      .get(this.ajsStateParams.apiId)
      .pipe(
        takeUntil(this.unsubscribe$),
        switchMap((api) =>
          combineLatest([isImgUrl(api.picture_url), isImgUrl(api.background_url)]).pipe(
            map(([hasPictureImg, hasBackgroundImg]) => ({
              ...api,
              picture_url: hasPictureImg ? api.picture_url : null,
              background_url: hasBackgroundImg ? api.background_url : null,
            })),
          ),
        ),
        tap((api) => {
          const isReadOnly = !this.permissionService.hasAnyMatching(['api-definition-u']);

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
