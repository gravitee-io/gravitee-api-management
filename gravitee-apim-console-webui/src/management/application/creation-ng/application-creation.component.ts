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
import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { map } from 'rxjs/operators';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { GioSaveBarModule } from '@gravitee/ui-particles-angular';

import { ApplicationCreationFormComponent, ApplicationForm } from './components/application-creation-form.component';

import { ApplicationTypesService } from '../../../services-ngx/application-types.service';

const TYPES_INFOS = {
  SIMPLE: {
    title: 'Simple',
    subtitle: 'A hands-free application. Using this type, you will be able to define the client_id by your own',
    icon: 'gio:hand',
  },
  BROWSER: {
    title: 'SPA',
    subtitle: 'Angular, React, Ember, ...',
    icon: 'gio:laptop',
  },
  WEB: {
    title: 'Web',
    subtitle: 'Java, .Net, ...',
    icon: 'gio:language',
  },
  NATIVE: {
    title: 'Native',
    subtitle: 'iOS, Android, ...',
    icon: 'gio:tablet-device',
  },
  BACKEND_TO_BACKEND: {
    title: 'Backend to backend',
    subtitle: 'Machine to machine',
    icon: 'gio:share-2',
  },
};

@Component({
  selector: 'application-creation',
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, ApplicationCreationFormComponent, GioSaveBarModule],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./application-creation.component.scss'],
  templateUrl: './application-creation.component.html',
})
export class ApplicationCreationComponent implements OnInit {
  public applicationFormGroup = new FormGroup<ApplicationForm>({
    name: new FormControl(undefined, Validators.required),
    description: new FormControl(),
    domain: new FormControl(),
    type: new FormControl(undefined, Validators.required),

    appType: new FormControl(),
    appClientId: new FormControl(),

    oauthGrantTypes: new FormControl(),
    oauthRedirectUris: new FormControl([]),
  });

  public applicationTypes$ = this.applicationTypesService.getEnabledApplicationTypes().pipe(
    map((types) =>
      types.map((type) => {
        const typeInfo = TYPES_INFOS[type.id.toUpperCase()];
        return {
          ...type,
          id: type.id.toUpperCase(),
          title: typeInfo.title,
          subtitle: typeInfo.subtitle,
          icon: typeInfo.icon,
        };
      }),
    ),
  );

  public applicationPayload: unknown;
  constructor(private readonly applicationTypesService: ApplicationTypesService) {}
  ngOnInit() {}

  onSubmit() {
    // TODO: save application
    this.applicationPayload = this.applicationFormGroup.value;
  }
}
