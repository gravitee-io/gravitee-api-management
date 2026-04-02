/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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
import { AsyncPipe } from '@angular/common';
import { Component, computed, inject, input, OnInit } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatDivider } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router } from '@angular/router';
import { isEqual } from 'lodash';
import { map, Observable, of, startWith, Subject, take, takeUntil, tap } from 'rxjs';

import { CopyCodeComponent } from '../../../../../components/copy-code/copy-code.component';
import { LoaderComponent } from '../../../../../components/loader/loader.component';
import { Application, ApplicationGrantType, ApplicationType } from '../../../../../entities/application/application';
import { ClientCertificatesResponse } from '../../../../../entities/application/client-certificate';
import { UserApplicationPermissions } from '../../../../../entities/permission/permission';
import { ApplicationCertificateService } from '../../../../../services/application-certificate.service';
import { ApplicationService } from '../../../../../services/application.service';
import { ConfigService } from '../../../../../services/config.service';
import { ApplicationTabSettingsCertificatesComponent } from '../application-tab-settings-certificates/application-tab-settings-certificates.component';

interface ApplicationSettingsVM {
  name: string;
  description: string | undefined;
  domain: string | undefined;
  appType: string | undefined;
  appClientId: string | undefined;
  oauthRedirectUris: string[] | undefined;
  oauthGrantTypes: string[] | undefined;
}

interface ApplicationSettingsForm {
  name: FormControl<string>;
  description: FormControl<string | undefined>;
  domain: FormControl<string | undefined>;
  appType: FormControl<string | undefined>;
  appClientId: FormControl<string | undefined>;
  oauthRedirectUris: FormControl<string[] | undefined>;
  oauthGrantTypes: FormControl<string[] | undefined>;
}

interface ApplicationGrantTypeVM {
  type: string;
  name: string;
  isDisabled: boolean;
}

@Component({
  selector: 'app-application-tab-settings-edit',
  imports: [
    ApplicationTabSettingsCertificatesComponent,
    CopyCodeComponent,
    LoaderComponent,
    MatButtonModule,
    MatCardModule,
    MatDivider,
    MatChipsModule,
    MatFormFieldModule,
    MatIcon,
    MatInput,
    MatSelectModule,
    ReactiveFormsModule,
    AsyncPipe,
  ],
  templateUrl: './application-tab-settings-edit.component.html',
  styleUrl: './application-tab-settings-edit.component.scss',
})
export class ApplicationTabSettingsEditComponent implements OnInit {
  protected readonly configService = inject(ConfigService);
  private readonly certService = inject(ApplicationCertificateService);

  applicationId = input.required<string>();
  applicationTypeConfiguration = input.required<ApplicationType>();
  userApplicationPermissions = input.required<UserApplicationPermissions>();

  protected get mtlsEnabled(): boolean {
    return this.configService.configuration?.portalNext?.mtls?.enabled === true;
  }

  protected readonly certificates = rxResource<ClientCertificatesResponse | undefined, string | null>({
    params: () => (this.mtlsEnabled ? this.applicationId() : null),
    stream: ({ params }) => (params ? this.certService.list(params, 1, 1) : of(undefined)),
  });

  showCertificates = computed(() => {
    if (this.certificates.error()) return true;
    const response = this.certificates.value();
    if (!response) return false;
    return (response.metadata?.paginateMetaData?.totalElements ?? 0) > 0;
  });

  application$!: Observable<Application>;

  initialValues: ApplicationSettingsVM = {
    name: '',
    description: undefined,
    domain: undefined,
    appType: undefined,
    appClientId: undefined,
    oauthRedirectUris: undefined,
    oauthGrantTypes: undefined,
  };

  applicationSettingsForm: FormGroup<ApplicationSettingsForm> = new FormGroup<ApplicationSettingsForm>({
    name: new FormControl<string>('', { nonNullable: true }),
    description: new FormControl<string | undefined>(undefined, { nonNullable: true }),
    domain: new FormControl<string | undefined>(undefined, { nonNullable: true }),
    appType: new FormControl<string | undefined>(undefined, { nonNullable: true }),
    appClientId: new FormControl<string | undefined>(undefined, { nonNullable: true }),
    oauthRedirectUris: new FormControl<string[] | undefined>(undefined, { nonNullable: true }),
    oauthGrantTypes: new FormControl<string[] | undefined>(undefined, { nonNullable: true }),
  });

  grantTypesList: ApplicationGrantTypeVM[] | undefined;

  formUnchanged$: Observable<boolean> = of(true);

  private unsubscribe$ = new Subject();
  private application!: Application;

  constructor(
    private readonly applicationService: ApplicationService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.buildGrantTypeList();
    this.application$ = this.applicationService.get(this.applicationId());

    this.application$.pipe(take(1)).subscribe(application => {
      this.application = application;

      this.addValidatorsOnForm();

      this.initialValues = this.convertToVM(application);
      this.reset();
      this.formUnchanged$ = this.applicationSettingsForm.valueChanges.pipe(
        startWith(this.initialValues),
        map(value => isEqual(this.initialValues, value)),
      );
    });
  }

  addRedirectUri(event: MatChipInputEvent) {
    const newRedirectUri = (event.value || '').trim();
    if (newRedirectUri) {
      const currentRedirectUris = this.applicationSettingsForm.controls.oauthRedirectUris.value;
      if (currentRedirectUris) {
        this.applicationSettingsForm.controls.oauthRedirectUris.setValue([...currentRedirectUris, newRedirectUri]);
      } else {
        this.applicationSettingsForm.controls.oauthRedirectUris.setValue([newRedirectUri]);
      }
    }
    // Clear the input value
    event.chipInput!.clear();
  }

  removeRedirectUri(redirectUriToRemove: string) {
    const currentRedirectUris = this.applicationSettingsForm.controls.oauthRedirectUris.value;
    if (currentRedirectUris) {
      const redirectUris: string[] = [...currentRedirectUris];
      const index = currentRedirectUris.indexOf(redirectUriToRemove);
      if (index > -1) {
        redirectUris.splice(index, 1);
        this.applicationSettingsForm.controls.oauthRedirectUris.setValue(redirectUris);
      }
    }
  }

  reset() {
    this.applicationSettingsForm.patchValue(this.initialValues);
  }

  submit(): void {
    const updatedApplication: Application = this.computeApplicationToSave(this.applicationSettingsForm.getRawValue());

    this.applicationService
      .save(updatedApplication)
      .pipe(
        tap(app => {
          this.initialValues = this.convertToVM(app);
          this.reset();
          const url = this.router.url;
          return this.router.navigate([url], { onSameUrlNavigation: 'reload' });
        }),
        takeUntil(this.unsubscribe$),
      )
      .subscribe({ error: err => console.error(err) });
  }

  private convertToVM(application: Application): ApplicationSettingsVM {
    return {
      name: application.name,
      description: application.description,
      domain: application.domain,
      appType: application.settings.app?.type,
      appClientId: application.settings.app?.client_id,
      oauthGrantTypes: application.settings.oauth?.grant_types,
      oauthRedirectUris: application.settings.oauth?.redirect_uris,
    };
  }

  private isGrantTypeMandatory(t: ApplicationGrantType): boolean {
    return this.applicationTypeConfiguration().mandatory_grant_types?.some(grantType => grantType.type === t.type) ?? false;
  }

  private buildGrantTypeList() {
    this.grantTypesList = this.applicationTypeConfiguration().allowed_grant_types?.map(t => {
      return <ApplicationGrantTypeVM>{
        type: t.type,
        name: this.isGrantTypeMandatory(t) ? `${t.name} - (Mandatory)` : t.name,
        isDisabled: this.isGrantTypeMandatory(t),
      };
    });
  }

  private addValidatorsOnForm() {
    this.applicationSettingsForm.controls.name.setValidators(Validators.required);
    if (this.application.settings.oauth) {
      this.applicationSettingsForm.controls.oauthGrantTypes.setValidators([Validators.required, Validators.minLength(1)]);
      if (this.applicationTypeConfiguration().requires_redirect_uris) {
        this.applicationSettingsForm.controls.oauthRedirectUris.setValidators([Validators.required, Validators.minLength(1)]);
      }
    }
  }

  private computeApplicationToSave(appVM: ApplicationSettingsVM) {
    const appToUpdate = {
      ...this.application,
      name: appVM.name,
      description: appVM.description,
      domain: appVM.domain,
    };
    if (appToUpdate.settings.app) {
      appToUpdate.settings.app.type = appVM.appType;
      appToUpdate.settings.app.client_id = appVM.appClientId;
    } else if (appToUpdate.settings.oauth) {
      if (appVM.oauthGrantTypes) {
        appToUpdate.settings.oauth.grant_types = appVM.oauthGrantTypes;

        // Responses types depend on the selected grant types. They have to be taken from the type configuration
        const allowedGrantTypes = this.applicationTypeConfiguration().allowed_grant_types;
        if (!allowedGrantTypes) {
          appToUpdate.settings.oauth.response_types = [];
        } else {
          appToUpdate.settings.oauth.response_types = allowedGrantTypes
            .filter(grantType => appVM.oauthGrantTypes?.some(type => type === grantType.type))
            .flatMap(grantType => grantType.response_types ?? []);
        }
      }
      if (appVM.oauthRedirectUris) {
        appToUpdate.settings.oauth.redirect_uris = appVM.oauthRedirectUris;
      }
    }
    return appToUpdate;
  }
}
