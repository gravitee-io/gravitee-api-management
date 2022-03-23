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
import { Component, Input, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';

import { Api, Application, Plan } from '../../../../../../projects/portal-webclient-sdk/src/lib';
import { ApplicationTypeOption } from '../application-creation.component';

@Component({
  selector: 'app-application-creation-step5',
  templateUrl: './application-creation-step5.component.html',
  styleUrls: ['../application-creation.component.css'],
})
export class ApplicationCreationStep5Component implements OnInit {
  @Input() canValidate: boolean;
  @Input() creationError: boolean;
  @Input() creationSuccess: boolean;
  @Input() applicationForm: FormGroup;
  @Input() subscribeList: any[];
  @Input() subscriptionErrors: { message: string; api: Api }[];
  @Input() applicationType: ApplicationTypeOption;
  @Input() createdApplication: Application;
  @Input() currentStep: number;
  @Input() apiKeyModeTitle: string;

  validationListOptions: any;

  constructor(private translateService: TranslateService) {}

  get grantTypeNames() {
    if (!this.isSimpleApp && this.applicationForm.contains('settings')) {
      const { settings } = this.applicationForm.getRawValue();
      const types = this.applicationType.allowed_grant_types;
      return settings.oauth.grant_types.map(type => {
        return types.find(a => a.type === type).name;
      });
    }
    return [];
  }

  get pictureSrc() {
    return this.applicationForm.get('picture').value;
  }

  get appName() {
    return this.applicationForm.get('name').value;
  }

  get appDescription() {
    return this.applicationForm.get('description').value;
  }

  get appClientId() {
    if (this.isSimpleApp && this.applicationForm.contains('settings')) {
      const { settings } = this.applicationForm.getRawValue();
      return settings.app.client_id;
    }
    return null;
  }

  get redirectURIs() {
    if (!this.isSimpleApp && this.applicationForm.contains('settings')) {
      const { settings } = this.applicationForm.getRawValue();
      return settings.oauth.redirect_uris;
    }
    return [];
  }

  get requiresRedirectUris() {
    return this.applicationType.requires_redirect_uris;
  }

  get isSimpleApp() {
    return this.applicationType.id.toLowerCase() === 'simple';
  }

  ngOnInit(): void {
    this.translateService
      .get([
        i18n('applicationCreation.subscription.comment'),
        i18n('applicationCreation.subscription.validation.type'),
        i18n('applicationCreation.subscription.validation.auto'),
        i18n('applicationCreation.subscription.validation.manual'),
      ])
      .toPromise()
      .then(translations => {
        const values = Object.values(translations);

        this.validationListOptions = {
          data: [
            { field: 'api.name', label: 'Api' },
            { field: 'plan.name', label: 'Plan' },
            {
              field: 'request',
              label: values[0],
            },
            {
              field: item => (item.plan.validation.toUpperCase() === Plan.ValidationEnum.AUTO ? values[2] : values[3]),
              label: values[1],
            },
          ],
        };
      });
  }
}
