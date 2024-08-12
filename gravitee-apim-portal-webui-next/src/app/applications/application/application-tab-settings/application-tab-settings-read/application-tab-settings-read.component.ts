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
import { NgIf } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

import { CopyCodeComponent } from '../../../../../components/copy-code/copy-code.component';
import { Application, ApplicationType } from '../../../../../entities/application/application';

interface ReadOnlyApplicationSettingsVM {
  isSimple: boolean;
  isRedirectUriRequired: boolean | undefined;
  type: string | undefined;
  typeDescription: string | undefined;
  redirectUris: string[] | undefined;
  grantTypes: string[] | undefined;
  clientId: string | undefined;
  clientSecret: string | undefined;
}

@Component({
  selector: 'app-application-tab-settings-read',
  standalone: true,
  imports: [CopyCodeComponent, MatButtonModule, MatCardModule, NgIf],
  templateUrl: './application-tab-settings-read.component.html',
  styleUrl: './application-tab-settings-read.component.scss',
})
export class ApplicationTabSettingsReadComponent implements OnInit {
  @Input()
  application!: Application;

  @Input()
  applicationTypeConfiguration!: ApplicationType;

  readOnlyValues: ReadOnlyApplicationSettingsVM = {
    isSimple: true,
    isRedirectUriRequired: undefined,
    type: undefined,
    typeDescription: undefined,
    redirectUris: undefined,
    grantTypes: undefined,
    clientId: undefined,
    clientSecret: undefined,
  };

  ngOnInit(): void {
    if (this.applicationTypeConfiguration.id === 'simple') {
      this.readOnlyValues = {
        isSimple: true,
        isRedirectUriRequired: false,
        type: undefined,
        typeDescription: this.application.settings.app?.type,
        redirectUris: undefined,
        grantTypes: undefined,
        clientId: this.application.settings.app?.client_id,
        clientSecret: undefined,
      };
    } else {
      this.readOnlyValues = {
        isSimple: false,
        isRedirectUriRequired: this.applicationTypeConfiguration.requires_redirect_uris,
        type: this.applicationTypeConfiguration.name,
        typeDescription: this.applicationTypeConfiguration.description,
        redirectUris: this.application.settings.oauth!.redirect_uris,
        grantTypes: this.application.settings.oauth!.grant_types.map(
          type => this.applicationTypeConfiguration.allowed_grant_types!.find(grantType => grantType.type === type)!.name ?? '',
        ),
        clientId: this.application.settings.oauth?.client_id,
        clientSecret: this.application.settings.oauth?.client_secret,
      };
    }
  }
}
