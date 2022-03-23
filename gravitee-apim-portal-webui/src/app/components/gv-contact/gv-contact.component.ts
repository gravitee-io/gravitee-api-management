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
import { ApiService, ApplicationService, PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../services/notification.service';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { CurrentUserService } from '../../services/current-user.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

@Component({
  selector: 'app-gv-contact',
  templateUrl: './gv-contact.component.html',
  styleUrls: ['./gv-contact.component.css'],
})
export class GvContactComponent implements OnInit {
  @Input() apiId: string;

  contactForm: FormGroup;
  applications: {
    label: string;
    value: string;
  }[];
  apis: {
    label: string;
    value: string;
  }[];
  isSending: boolean;

  constructor(
    private applicationService: ApplicationService,
    private apiService: ApiService,
    private portalService: PortalService,
    private formBuilder: FormBuilder,
    private notificationService: NotificationService,
    private currentUserService: CurrentUserService,
  ) {}

  ngOnInit(): void {
    this.contactForm = this.formBuilder.group({
      api: this.apiId || null,
      application: null,
      subject: new FormControl(null, Validators.required),
      content: new FormControl(null, Validators.required),
      copy_to_sender: false,
    });

    // Feature request: https://github.com/gravitee-io/issues/issues/6700
    this.applicationService.getApplications({ size: -1 }).subscribe(response => {
      this.applications = response.data.map(application => {
        return { label: `${application.name} (${application.owner.display_name})`, value: application.id };
      });
    });

    // Feature request: https://github.com/gravitee-io/issues/issues/6700
    this.apiService.getApis({ size: -1 }).subscribe(response => {
      this.apis = response.data.map(api => {
        return { label: `${api.name} (${api.version})`, value: api.id };
      });
    });

    const user = this.currentUserService.get().getValue();
    if (user && !user.email) {
      this.notificationService.warning(i18n('errors.email.required'));
      setTimeout(() => {
        this.contactForm.disable();
      }, 0);
    }
  }

  reset() {
    this.contactForm.reset({
      api: this.apiId || null,
      copy_to_sender: false,
    });
  }

  submit() {
    this.isSending = true;
    this.portalService
      .createTicket({ ticketInput: this.contactForm.value })
      .toPromise()
      .then(() => {
        this.notificationService.success(i18n('gv-contact.success'));
        this.reset();
      })
      .finally(() => (this.isSending = false));
  }
}
