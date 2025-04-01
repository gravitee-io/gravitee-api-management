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
import { Component, Input } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';

import { EndpointV2 } from '../../../../../../../entities/management-api-v2';
import { Tenant } from '../../../../../../../entities/tenant/tenant';

export type EndpointGeneralData = Pick<EndpointV2, 'name' | 'type' | 'target' | 'weight' | 'tenants' | 'backup'>;

@Component({
  selector: 'api-proxy-group-endpoint-edit-general',
  templateUrl: './api-proxy-group-endpoint-edit-general.component.html',
  styleUrls: ['./api-proxy-group-endpoint-edit-general.component.scss'],
  standalone: false,
})
export class ApiProxyGroupEndpointEditGeneralComponent {
  @Input() generalForm: UntypedFormGroup;
  @Input() endpoint: EndpointV2;
  @Input() supportedTypes: string[];
  @Input() tenants: Tenant[];
}
