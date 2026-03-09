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

import { Pipe, PipeTransform } from '@angular/core';
import type { LocalizeFn } from '@angular/localize/init';

import { ApplicationType } from '../entities/application/application';

type ApplicationTypeField = 'name' | 'description';

declare const $localize: LocalizeFn;

function localizeApplicationType(typeId: string, field: ApplicationTypeField): string | null {
  switch (`${typeId}.${field}`) {
    case 'simple.name':
      return $localize`:@@applicationType.simple.name:Simple`;
    case 'simple.description':
      return $localize`:@@applicationType.simple.description:A standalone client where you manage your own client_id. No DCR involved.`;

    case 'browser.name':
      return $localize`:@@applicationType.browser.name:SPA`;
    case 'browser.description':
      return $localize`:@@applicationType.browser.description:Front-end JS apps (React, Angular, Vue) using DCR for authentication.`;

    case 'web.name':
      return $localize`:@@applicationType.web.name:Web`;
    case 'web.description':
      return $localize`:@@applicationType.web.description:Server-side web apps (e.g., .NET, Java) that authenticate users through DCR.`;

    case 'native.name':
      return $localize`:@@applicationType.native.name:Native`;
    case 'native.description':
      return $localize`:@@applicationType.native.description:Mobile and desktop apps (iOS, Android) that authenticate through DCR.`;

    case 'backend_to_backend.name':
      return $localize`:@@applicationType.backendToBackend.name:Backend to backend`;
    case 'backend_to_backend.description':
      return $localize`:@@applicationType.backendToBackend.description:Machine-to-machine apps (scripts, daemons, CLIs) using DCR for API access.`;

    default:
      return null;
  }
}

@Pipe({
  name: 'applicationTypeTranslate',
  standalone: true,
})
export class ApplicationTypeTranslatePipe implements PipeTransform {
  transform(type: ApplicationType | null | undefined, field: ApplicationTypeField): string {
    if (!type?.id) {
      return '';
    }

    const backendValue = field === 'name' ? type.name || type.id : type.description || '';

    if (typeof $localize === 'undefined') {
      return backendValue;
    }

    const translated = localizeApplicationType(type.id, field);
    return translated ?? backendValue;
  }
}
