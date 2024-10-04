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
import { AsyncPipe, NgForOf, NgIf } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipInputEvent, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule, MatLabel } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { isEqual } from 'lodash';
import { map, Observable, startWith, Subject, take, takeUntil, tap } from 'rxjs';
import { of } from 'rxjs/internal/observable/of';

import { CopyCodeComponent } from '../../../../../components/copy-code/copy-code.component';
import { PictureComponent } from '../../../../../components/picture/picture.component';
import { Application, ApplicationGrantType, ApplicationType } from '../../../../../entities/application/application';
import { ApplicationService } from '../../../../../services/application.service';

interface ApplicationSettingsVM {
  name: string;
  description: string | undefined;
  picture: string | undefined;
  appType: string | undefined;
  appClientId: string | undefined;
  oauthRedirectUris: string[] | undefined;
  oauthGrantTypes: string[] | undefined;
}

interface ApplicationSettingsForm {
  name: FormControl<string>;
  description: FormControl<string | undefined>;
  picture: FormControl<string | undefined>;
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
  standalone: true,
  imports: [
    CopyCodeComponent,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIcon,
    MatInput,
    MatLabel,
    MatSelectModule,
    NgForOf,
    NgIf,
    PictureComponent,
    ReactiveFormsModule,
    AsyncPipe,
  ],
  templateUrl: './application-tab-settings-edit.component.html',
  styleUrl: './application-tab-settings-edit.component.scss',
})
export class ApplicationTabSettingsEditComponent implements OnInit {
  @Input()
  applicationId!: string;

  @Input()
  applicationTypeConfiguration!: ApplicationType;

  application$!: Observable<Application>;

  initialValues: ApplicationSettingsVM = {
    name: '',
    description: undefined,
    picture: undefined,
    appType: undefined,
    appClientId: undefined,
    oauthRedirectUris: undefined,
    oauthGrantTypes: undefined,
  };

  applicationSettingsForm: FormGroup<ApplicationSettingsForm> = new FormGroup<ApplicationSettingsForm>({
    name: new FormControl<string>('', { nonNullable: true }),
    description: new FormControl<string | undefined>(undefined, { nonNullable: true }),
    picture: new FormControl<string | undefined>(undefined, { nonNullable: true }),
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
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.buildGrantTypeList();
    this.application$ = this.applicationService.get(this.applicationId);

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

  deletePicture(): void {
    this.applicationSettingsForm.controls.picture.setValue(undefined);
  }

  async onPictureChange($event: Event) {
    const target = $event.target as HTMLInputElement;
    const files = target.files; // Here we use only the first file (single file)
    if (files && files.length > 0) {
      this.applicationSettingsForm.controls.picture.setValue(await toBase64(files[0]));
    }
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
      picture: application.picture,
      appType: application.settings.app?.type,
      appClientId: application.settings.app?.client_id,
      oauthGrantTypes: application.settings.oauth?.grant_types,
      oauthRedirectUris: application.settings.oauth?.redirect_uris,
    };
  }

  private isGrantTypeMandatory(t: ApplicationGrantType): boolean {
    return this.applicationTypeConfiguration.mandatory_grant_types?.some(grantType => grantType.type === t.type) ?? false;
  }

  private buildGrantTypeList() {
    this.grantTypesList = this.applicationTypeConfiguration.allowed_grant_types?.map(t => {
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
      if (this.applicationTypeConfiguration.requires_redirect_uris) {
        this.applicationSettingsForm.controls.oauthRedirectUris.setValidators([Validators.required, Validators.minLength(1)]);
      }
    }
  }

  private computeApplicationToSave(appVM: ApplicationSettingsVM) {
    const appToUpdate = {
      ...this.application,
      name: appVM.name,
      description: appVM.description,
      picture: appVM.picture,
    };
    if (appToUpdate.settings.app) {
      appToUpdate.settings.app.type = appVM.appType;
      appToUpdate.settings.app.client_id = appVM.appClientId;
    } else if (appToUpdate.settings.oauth) {
      if (appVM.oauthGrantTypes) {
        appToUpdate.settings.oauth.grant_types = appVM.oauthGrantTypes;

        // Responses types depend on the selected grant types. They have to be taken from the type configuration
        if (!this.applicationTypeConfiguration.allowed_grant_types) {
          appToUpdate.settings.oauth.response_types = [];
        } else {
          appToUpdate.settings.oauth.response_types = this.applicationTypeConfiguration.allowed_grant_types
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

const toBase64 = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      // because we use `readAsDataURL`, reader.result is guaranteed to be a string
      resolve(<string>reader.result);
    };
    reader.onerror = error => reject(error);
  });
