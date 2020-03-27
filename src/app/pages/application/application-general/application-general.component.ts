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
import '@gravitee/ui-components/wc/gv-list';
import '@gravitee/ui-components/wc/gv-info';
import '@gravitee/ui-components/wc/gv-rating-list';
import '@gravitee/ui-components/wc/gv-confirm';
import {
  Application,
  ApplicationService,
  ApiService,
  SubscriptionService,
  PermissionsService,
  PermissionsResponse, Subscription
} from '@gravitee/ng-portal-webclient';
import { ActivatedRoute, Router } from '@angular/router';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { TranslateService } from '@ngx-translate/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { LoaderService } from '../../../services/loader.service';
import { NotificationService } from '../../../services/notification.service';
import StatusEnum = Subscription.StatusEnum;

@Component({
  selector: 'app-application-general',
  templateUrl: './application-general.component.html',
  styleUrls: ['./application-general.component.css']
})
export class ApplicationGeneralComponent implements OnInit {

  applicationForm: FormGroup;
  application: Application;
  linkedApis: Promise<any[]>;
  miscellaneous: any[];
  permissions: PermissionsResponse;
  canUpdate: boolean;
  canDelete: boolean;

  constructor(
    private applicationService: ApplicationService,
    private subscriptionService: SubscriptionService,
    private apiService: ApiService,
    private translateService: TranslateService,
    private route: ActivatedRoute,
    private router: Router,
    private loaderService: LoaderService,
    private notificationService: NotificationService,
    private formBuilder: FormBuilder,
    private permissionsService: PermissionsService,
  ) {
  }

  ngOnInit() {
    this.application = this.route.snapshot.data.application;
    if (this.application) {
      this.permissionsService.getCurrentUserPermissions({ applicationId: this.application.id }).toPromise()
        .then((permissions) => (this.permissions = permissions))
        .catch(() => (this.permissions = {}))
        .finally(() => {
          this.canDelete = this.permissions.DEFINITION && this.permissions.DEFINITION.includes('D');
          this.canUpdate = this.permissions.DEFINITION && this.permissions.DEFINITION.includes('U');
        });

      this.reset();
      this.translateService.get([i18n('application.miscellaneous.owner'), i18n('application.miscellaneous.type'),
        i18n('application.miscellaneous.createdDate'), i18n('application.miscellaneous.lastUpdate')])
        .subscribe(
          ({
             'application.miscellaneous.owner': owner,
             'application.miscellaneous.type': type,
             'application.miscellaneous.createdDate': createdDate,
             'application.miscellaneous.lastUpdate': lastUpdate,
           }) => {
            this.translateService.get('application.types', { type: this.application.applicationType })
              .toPromise().then((applicationType => {
              this.miscellaneous = [
                { key: owner, value: this.application.owner.display_name },
                { key: type, value: applicationType },
                {
                  key: createdDate,
                  value: new Date(this.application.created_at),
                  date: 'short'
                },
                {
                  key: lastUpdate,
                  value: new Date(this.application.updated_at),
                  date: 'relative'
                },
              ];
            }));
          });

      this.linkedApis = this.applicationService.getSubscriberApisByApplicationId({
        applicationId: this.application.id,
        statuses: [StatusEnum.ACCEPTED, StatusEnum.PAUSED],
      })
        .toPromise()
        .then((response) => {
          return response.data.map((api) => ({
            name: api.name,
            description: api.description,
            picture: (api._links ? api._links.picture : '')
          }));
        })
        .catch(() => []);
    }
  }

  reset() {
    this.applicationForm = this.formBuilder.group(this.application);
    this.applicationForm.setControl('picture', new FormControl(''));
    this.applicationForm.setControl('settings', new FormGroup({
      app: new FormGroup({
        type: new FormControl(''),
      }),
      oauth: new FormGroup({
        client_id: new FormControl(''),
        client_secret: new FormControl(''),
      })
    }));
    this.applicationForm.patchValue(this.application);
  }

  submit() {
    if (!this.loaderService.get() && this.canUpdate) {
      this.applicationService.updateApplicationByApplicationId(
        { applicationId: this.application.id, Application: this.applicationForm.value }).subscribe((application) => {
        this.application = application;
        this.reset();
        this.notificationService.success(i18n('application.success.save'));
        document.dispatchEvent(new Event(':gv-header-item:refresh'));
      });
    }
  }

  onFileLoad(picture) {
    this.applicationForm.value.picture = picture;
    this.applicationForm.patchValue(this.applicationForm.value);
    this.applicationForm.markAsDirty();
  }

  delete() {
    this.applicationService.deleteApplicationByApplicationId({ applicationId: this.application.id }).toPromise().then(() => {
      this.router.navigate(['applications']);
      this.notificationService.success(i18n('application.success.delete'));
    });
  }

  isLoading() {
    return this.loaderService.isLoading;
  }
}
