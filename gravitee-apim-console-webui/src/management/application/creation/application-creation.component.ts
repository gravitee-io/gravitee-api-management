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

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, Inject, inject, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { map, tap } from 'rxjs/operators';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { GioSaveBarModule } from '@gravitee/ui-particles-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';

import { ApplicationCreationFormComponent, ApplicationForm } from './components/application-creation-form.component';

import { ApplicationTypesService } from '../../../services-ngx/application-types.service';
import { ApplicationService } from '../../../services-ngx/application.service';
import { SnackBarService } from '../../../services-ngx/snack-bar.service';
import { toDictionary } from '../../../util/gio-form-header.util';
import { Constants } from '../../../entities/Constants';

const TYPES_INFOS = {
  SIMPLE: {
    icon: 'gio:hand',
  },
  BROWSER: {
    icon: 'gio:laptop',
  },
  WEB: {
    icon: 'gio:language',
  },
  NATIVE: {
    icon: 'gio:tablet-device',
  },
  BACKEND_TO_BACKEND: {
    icon: 'gio:share-2',
  },
};

@Component({
  selector: 'application-creation',
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatSnackBarModule, ApplicationCreationFormComponent, GioSaveBarModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./application-creation.component.scss'],
  templateUrl: './application-creation.component.html',
})
export class ApplicationCreationComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private isCreating = false;

  public applicationFormGroup = new FormGroup<ApplicationForm>({
    name: new FormControl(undefined, Validators.required),
    description: new FormControl(undefined, Validators.required),
    domain: new FormControl(),
    type: new FormControl(undefined, Validators.required),

    appType: new FormControl(),
    appClientId: new FormControl(),
    appClientCertificate: new FormControl(),

    oauthGrantTypes: new FormControl(),
    oauthRedirectUris: new FormControl([]),
    additionalClientMetadata: new FormControl([]),
    groups: new FormControl([]),
  });

  requireUserGroups = false;
  public applicationTypes$ = this.applicationTypesService.getEnabledApplicationTypes().pipe(
    map(types =>
      types.map(type => {
        const typeInfo = TYPES_INFOS[type.id.toUpperCase()];
        return {
          ...type,
          id: type.id.toUpperCase(),
          title: type.name,
          subtitle: type.description,
          icon: typeInfo.icon,
        };
      }),
    ),
    tap(types => {
      // Set the first type as default
      this.applicationFormGroup.get('type').setValue(types[0].id.toUpperCase());
    }),
  );

  constructor(
    private readonly applicationTypesService: ApplicationTypesService,
    private readonly applicationService: ApplicationService,
    private readonly snackBarService: SnackBarService,
    private readonly activatedRoute: ActivatedRoute,
    private readonly router: Router,
    @Inject(Constants) private readonly constants: Constants,
  ) {}

  ngOnInit(): void {
    this.setUserGroupRequiredValidator();
  }

  onSubmit() {
    if (this.applicationFormGroup.invalid || this.isCreating === true) {
      return;
    }
    this.isCreating = true;
    const applicationPayload = this.applicationFormGroup.value;

    this.applicationService
      .create({
        name: applicationPayload.name,
        description: applicationPayload.description,
        domain: applicationPayload.domain,
        groups: applicationPayload.groups,
        settings: {
          ...(applicationPayload.type === 'SIMPLE'
            ? {
                app: {
                  client_id: applicationPayload.appClientId,
                  type: applicationPayload.appType,
                },
              }
            : {
                oauth: {
                  application_type: applicationPayload.type,
                  grant_types: applicationPayload.oauthGrantTypes ?? [],
                  redirect_uris: applicationPayload.oauthRedirectUris ?? [],
                  additional_client_metadata: toDictionary(applicationPayload.additionalClientMetadata) ?? undefined,
                },
              }),
          tls: {
            client_certificate: applicationPayload.appClientCertificate,
          },
        },
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: application => {
          this.snackBarService.success('Application created');
          this.router.navigate(['../', application.id], { relativeTo: this.activatedRoute });
        },
        error: error => {
          this.snackBarService.error(error?.error?.message ?? 'An error occurred while creating the application!');
          this.isCreating = false;
        },
      });
  }

  private setUserGroupRequiredValidator() {
    if (this.constants.org.settings.userGroup?.required?.enabled) {
      this.applicationFormGroup.controls.groups?.setValidators(Validators.required);
      this.requireUserGroups = true;
    }
  }
}
