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
import { takeUntil, tap } from 'rxjs/operators';
import { combineLatest, Subject } from 'rxjs';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { SnackBarService } from '../../../../../services-ngx/snack-bar.service';
import { ApplicationService } from '../../../../../services-ngx/application.service';
import { Application, ApplicationType } from '../../../../../entities/application/application';

@Component({
  selector: 'application-general-ng',
  template: require('./application-general-ng.component.html'),
  styles: [require('./application-general-ng.component.scss')],
})
export class ApplicationGeneralNgComponent implements OnInit {
  public initialApplication: Application;
  public applicationType: ApplicationType;
  public applicationForm: FormGroup;
  public isLoadingData = true;
  public isReadOnly = false;
  public initialApplicationGeneralFormsValue: unknown;
  private unsubscribe$: Subject<boolean> = new Subject<boolean>();

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute,
    private readonly applicationService: ApplicationService,
    private readonly snackBarService: SnackBarService,
  ) {}

  public ngOnInit() {
    this.isLoadingData = true;
    combineLatest([
      this.applicationService.getById(this.activatedRoute.snapshot.params.applicationId),
      this.applicationService.getApplicationType(this.activatedRoute.snapshot.params.applicationId),
    ])
      .pipe(
        tap(([application, applicationType]) => {
          this.initialApplication = application;
          this.applicationType = applicationType;
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => {
        this.isLoadingData = false;
        this.isReadOnly = this.initialApplication.status === 'ARCHIVED';

        this.applicationForm = new FormGroup({
          details: new FormGroup({
            name: new FormControl({ value: this.initialApplication.name, disabled: this.isReadOnly }),
            description: new FormControl({ value: this.initialApplication.description, disabled: this.isReadOnly }),
            domain: new FormControl({ value: this.initialApplication.domain, disabled: this.isReadOnly }),
            type: new FormControl({ value: this.initialApplication.type, disabled: this.isReadOnly }),
          }),
          images: new FormGroup({
            picture: new FormControl({
              value: this.initialApplication.picture ? [this.initialApplication.picture] : undefined,
              disabled: this.isReadOnly,
            }),
            background: new FormControl({
              value: this.initialApplication.background ? [this.initialApplication.background] : undefined,
              disabled: this.isReadOnly,
            }),
          }),
        });

        if (this.initialApplication.type === 'SIMPLE') {
          this.applicationForm.addControl(
            'OAuth2Form',
            new FormGroup({
              client_id: new FormControl({
                value: this.initialApplication.settings?.app?.client_id ? this.initialApplication.settings.app.client_id : undefined,
                disabled: this.isReadOnly,
              }),
            }),
          );
        }

        if (this.initialApplication.type !== 'SIMPLE') {
          this.applicationForm.addControl(
            'OpenIDForm',
            new FormGroup({
              client_id: new FormControl({
                value: this.initialApplication.settings?.oauth?.client_id ? this.initialApplication.settings.oauth.client_id : undefined,
                disabled: this.isReadOnly,
              }),
              client_secret: new FormControl({
                value: this.initialApplication.settings?.oauth?.client_secret
                  ? this.initialApplication.settings.oauth.client_secret
                  : undefined,
                disabled: this.isReadOnly,
              }),
              grant_types: new FormControl(
                {
                  value: this.initialApplication.settings?.oauth?.grant_types
                    ? this.initialApplication.settings.oauth.grant_types
                    : undefined,
                  disabled: this.isReadOnly,
                },
                [Validators.required],
              ),
              redirect_uris: new FormControl({
                value: this.initialApplication.settings?.oauth?.redirect_uris
                  ? this.initialApplication.settings.oauth.redirect_uris
                  : undefined,
                disabled: this.isReadOnly,
              }),
            }),
          );
        }

        this.initialApplicationGeneralFormsValue = this.applicationForm.getRawValue();
      });
  }

  onSubmit() {
    const imagesValue = this.applicationForm.getRawValue().images;

    const applicationToUpdate = {
      ...this.initialApplication,
      ...this.applicationForm.getRawValue().details,
      settings:
        this.initialApplication.type === 'SIMPLE'
          ? {
              app: {
                ...this.applicationForm.getRawValue().OAuth2Form,
              },
            }
          : {
              oauth: {
                ...this.initialApplication.settings.oauth,
                ...this.applicationForm.getRawValue().OpenIDForm,
                application_type: 'NATIVE',
              },
            },
      ...(imagesValue?.picture?.length ? { picture: imagesValue.picture[0].dataUrl } : { picture: null }),
      ...(imagesValue?.background?.length ? { background: imagesValue.background[0].dataUrl } : { background: null }),
    };

    this.applicationService
      .update(applicationToUpdate)
      .pipe(
        tap(() => this.snackBarService.success('Application details successfully updated!')),
        takeUntil(this.unsubscribe$),
      )
      .subscribe(() => this.ngOnInit());
  }
}
