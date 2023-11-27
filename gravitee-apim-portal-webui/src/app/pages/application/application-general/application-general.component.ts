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
import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-relative-time';
import '@gravitee/ui-components/wc/gv-rating-list';
import '@gravitee/ui-components/wc/gv-confirm';
import '@gravitee/ui-components/wc/gv-file-upload';
import { getPictureDisplayName } from '@gravitee/ui-components/src/lib/item';
import { ActivatedRoute, Router } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { UntypedFormArray, UntypedFormBuilder, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';

import {
  Application,
  ApplicationService,
  ApplicationType,
  PermissionsResponse,
  Subscription,
} from '../../../../../projects/portal-webclient-sdk/src/lib';
import { GvHeaderItemComponent } from '../../../components/gv-header-item/gv-header-item.component';
import { EventService, GvEvent } from '../../../services/event.service';
import { NotificationService } from '../../../services/notification.service';
import { SearchQueryParam } from '../../../utils/search-query-param.enum';

const StatusEnum = Subscription.StatusEnum;
@Component({
  selector: 'app-application-general',
  templateUrl: './application-general.component.html',
  styleUrls: ['./application-general.component.css'],
})
export class ApplicationGeneralComponent implements OnInit, OnDestroy {
  applicationForm: UntypedFormGroup;
  application: Application;
  connectedApis: Promise<any[]>;
  permissions: PermissionsResponse;
  canUpdate: boolean;
  canDelete: boolean;
  isSaving: boolean;
  isDeleting: boolean;
  isRenewing: boolean;

  allGrantTypes: { name?: string; disabled: boolean; type?: string; value: boolean }[];
  private applicationTypeEntity: ApplicationType;

  constructor(
    private applicationService: ApplicationService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private notificationService: NotificationService,
    private formBuilder: UntypedFormBuilder,
    private eventService: EventService,
  ) {}

  ngOnDestroy() {
    this.initForm();
  }

  ngOnInit() {
    this.application = this.route.snapshot.data.application;
    this.permissions = this.route.snapshot.data.permissions;
    if (this.application) {
      this.applicationTypeEntity = this.route.snapshot.data.applicationType;
      this.canDelete = this.permissions.DEFINITION && this.permissions.DEFINITION.includes('D');
      this.canUpdate = this.permissions.DEFINITION && this.permissions.DEFINITION.includes('U');

      this.initForm();
      this.updateGrantTypes();
      this.applicationForm.get('picture').valueChanges.subscribe(picture => {
        this.eventService.dispatch(new GvEvent(GvHeaderItemComponent.UPDATE_PICTURE, { data: picture }));
      });
      this.applicationForm.get('background').valueChanges.subscribe(background => {
        this.eventService.dispatch(new GvEvent(GvHeaderItemComponent.UPDATE_BACKGROUND, { data: background }));
      });

      this.connectedApis = this.applicationService
        .getSubscriberApisByApplicationId({
          applicationId: this.application.id,
          statuses: [StatusEnum.ACCEPTED],
        })
        .toPromise()
        .then(response => response.data.map(api => ({ item: api, type: 'api' })));
    }
  }

  reset() {
    this.applicationForm.reset(this.application);
    this.updateGrantTypes();
  }

  isOAuth() {
    return this.application && this.application.settings.oauth != null;
  }

  initForm() {
    let settings;
    if (this.application) {
      if (this.isOAuth()) {
        settings = this.formBuilder.group({
          oauth: this.formBuilder.group({
            client_secret: new UntypedFormControl(this.application.settings.oauth.client_secret, null),
            client_id: new UntypedFormControl(this.application.settings.oauth.client_id, null),
            redirect_uris: new UntypedFormArray([]),
            grant_types: new UntypedFormArray([]),
          }),
        });
      } else {
        settings = this.formBuilder.group({
          app: this.formBuilder.group({
            type: new UntypedFormControl(this.application.settings.app.type, null),
            client_id: new UntypedFormControl(this.application.settings.app.client_id, null),
          }),
        });
      }

      this.applicationForm = this.formBuilder.group({
        id: this.application.id,
        name: new UntypedFormControl(this.application.name, [Validators.required]),
        description: new UntypedFormControl(this.application.description, [Validators.required]),
        domain: new UntypedFormControl(this.application.domain),
        picture: new UntypedFormControl(this.application.picture),
        background: new UntypedFormControl(this.application.background),
        settings,
      });
    }
  }

  onSwitchGrant(event, grantType) {
    if (event.target.value) {
      this.grantTypes.push(new UntypedFormControl(grantType.type));
    } else {
      let index = -1;
      this.grantTypes.controls.forEach((control, i) => {
        if (control.value === grantType.type) {
          index = i;
          return;
        }
      });
      this.grantTypes.removeAt(index);
    }
    this.applicationForm.markAsDirty();
  }

  updateGrantTypes() {
    if (this.isOAuth()) {
      this.grantTypes.clear();
      this.allGrantTypes = this.applicationTypeEntity.allowed_grant_types.map(allowedGrantType => {
        const value = this.application.settings.oauth.grant_types.find(grant => allowedGrantType.type === grant) != null;

        const disabled = this.applicationTypeEntity.mandatory_grant_types.find(grant => allowedGrantType.type === grant.type) != null;

        if (value === true) {
          this.grantTypes.push(new UntypedFormControl(allowedGrantType.type));
        }
        return { ...allowedGrantType, disabled, value };
      });
      if (this.requiresRedirectUris) {
        this.redirectURIs.setValidators(Validators.required);
      }
      this.redirectURIs.clear();
      this.application.settings.oauth.redirect_uris.forEach(value => {
        this.redirectURIs.push(new UntypedFormControl(value));
      });
    }
  }

  get grantTypes() {
    return this.applicationForm.get('settings.oauth.grant_types') as UntypedFormArray;
  }

  get requiresRedirectUris() {
    return this.applicationTypeEntity ? this.applicationTypeEntity.requires_redirect_uris : false;
  }

  addRedirectUri(event) {
    if (event.target.valid) {
      const value = event.target.value;
      if (value && value.trim() !== '') {
        if (!this.redirectURIs.controls.map(c => c.value).includes(value)) {
          this.redirectURIs.push(new UntypedFormControl(value, Validators.required));
          this.applicationForm.markAsDirty();
        }
        event.target.value = '';
      }
    }
  }

  get redirectURIs() {
    return this.applicationForm.get('settings.oauth.redirect_uris') as UntypedFormArray;
  }

  removeRedirectUri(index: number) {
    this.redirectURIs.removeAt(index);
    this.applicationForm.markAsDirty();
  }

  get validRedirectUris() {
    return (this.requiresRedirectUris && this.redirectURIs.length > 0) || !this.requiresRedirectUris;
  }

  submit() {
    this.isSaving = true;
    this.applicationService
      .updateApplicationByApplicationId({ applicationId: this.application.id, application: this.applicationForm.getRawValue() })
      .toPromise()
      .then(application => {
        this.application = application;
        this.reset();
        this.notificationService.success(i18n('application.success.save'));
        this.eventService.dispatch(new GvEvent(GvHeaderItemComponent.RELOAD_EVENT));
      })
      .finally(() => (this.isSaving = false));
  }

  get displayName() {
    return getPictureDisplayName(this.application);
  }

  delete() {
    this.isDeleting = true;
    this.applicationService
      .deleteApplicationByApplicationId({ applicationId: this.application.id })
      .toPromise()
      .then(() => {
        this.router.navigate(['applications']);
        this.notificationService.success(i18n('application.success.delete'));
      })
      .finally(() => (this.isDeleting = false));
  }

  renewSecret() {
    this.isRenewing = true;
    this.applicationService
      .renewApplicationSecret({ applicationId: this.application.id })
      .toPromise()
      .then(application => {
        this.application = application;
        this.reset();
        this.notificationService.success(i18n('application.success.renewSecret'));
      })
      .finally(() => (this.isRenewing = false));
  }

  toLocaleDateString(date: string) {
    return new Date(date).toLocaleDateString(this.translateService.currentLang);
  }

  @HostListener(':gv-list:click', ['$event.detail'])
  onGvListClick(detail: any) {
    const queryParams = {};
    queryParams[SearchQueryParam.APPLICATION] = this.application.id;
    this.router.navigate([`/catalog/api/${detail.item.id}`], { queryParams });
  }
}
