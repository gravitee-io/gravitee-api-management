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

import {
  EndpointHttpConfigComponent,
  EndpointHttpConfigValue,
} from '../../../components/endpoint-http-config/endpoint-http-config.component';
import { EndpointGroupV2 } from '../../../../../../entities/management-api-v2';

@Component({
  selector: 'api-proxy-group-configuration',
  templateUrl: './api-proxy-group-configuration.component.html',
  styleUrls: ['./api-proxy-group-configuration.component.scss'],
  standalone: false,
})
export class ApiProxyGroupConfigurationComponent {
  public static getGroupConfigurationFormGroup(endpointGroup: EndpointGroupV2, isReadonly: boolean): UntypedFormGroup {
    return EndpointHttpConfigComponent.getHttpConfigFormGroup(endpointGroup, isReadonly);
  }

  public static getGroupConfigurationFormValue(groupConfigurationFormGroup: UntypedFormGroup): EndpointHttpConfigValue {
    return EndpointHttpConfigComponent.getHttpConfigValue(groupConfigurationFormGroup);
  }

  @Input() groupConfigurationFormGroup: UntypedFormGroup;
}
