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
import { CUSTOM_ELEMENTS_SCHEMA, Component, Input, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

import { ApiService, ApplicationService, PortalService } from '../../../../projects/portal-webclient-sdk/src/lib';
import { NotificationService } from '../../services/notification.service';
import { CurrentUserService } from '../../services/current-user.service';
import { GvFormControlDirective } from '../../directives/gv-form-control.directive';
import { GvCheckboxControlValueAccessorDirective } from '../../directives/gv-checkbox-control-value-accessor.directive';

type ContactFormType = FormGroup<{
  api: FormControl<string>;
  application: FormControl<string>;
  subject: FormControl<string>;
  content: FormControl<string>;
  copy_to_sender: FormControl<boolean>;
}>;

@Component({
  selector: 'app-gv-contact',
  templateUrl: './gv-contact.component.html',
  styleUrls: ['./gv-contact.component.css'],
  imports: [ReactiveFormsModule, NgIf, GvFormControlDirective, GvCheckboxControlValueAccessorDirective, TranslatePipe],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class GvContactComponent implements OnInit {
  private applicationService = inject(ApplicationService);
  private apiService = inject(ApiService);
  private portalService = inject(PortalService);
  private notificationService = inject(NotificationService);
  private currentUserService = inject(CurrentUserService);

  @Input() apiId: string;

  contactForm: ContactFormType;
  applications: {
    label: string;
    value: string;
  }[];
  apis: {
    label: string;
    value: string;
  }[];
  isSending: boolean;

  ngOnInit(): void {
    this.contactForm = new FormGroup({
      api: new FormControl(this.apiId || null),
      application: new FormControl(null),
      subject: new FormControl(null, Validators.required),
      content: new FormControl(null, Validators.required),
      copy_to_sender: new FormControl(false) as FormControl<boolean>,
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
      this.notificationService.warning('errors.email.required');
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
      .createTicket({ ticketInput: this.contactForm.getRawValue() })
      .toPromise()
      .then(() => {
        this.notificationService.success('gv-contact.success');
        this.reset();
      })
      .finally(() => (this.isSending = false));
  }
}
