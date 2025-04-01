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
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { startWith, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { EndpointV2 } from '../../../../../../../entities/management-api-v2';
import { EndpointHttpConfigComponent } from '../../../../components/endpoint-http-config/endpoint-http-config.component';

export type EndpointConfigurationData = {
  inherit: boolean;
  configuration?: Pick<EndpointV2, 'headers' | 'httpClientOptions' | 'httpClientSslOptions' | 'httpProxy'>;
};

@Component({
  selector: 'api-proxy-group-endpoint-configuration',
  templateUrl: './api-proxy-group-endpoint-configuration.component.html',
  styleUrls: ['./api-proxy-group-endpoint-configuration.component.scss'],
  standalone: false,
})
export class ApiProxyGroupEndpointConfigurationComponent {
  public static getConfigurationFormGroup(endpoint: EndpointV2, isReadonly: boolean, unsubscribe$: Subject<boolean>): UntypedFormGroup {
    const inherit = new UntypedFormControl({ value: endpoint.inherit ?? false, disabled: isReadonly });
    const configuration = EndpointHttpConfigComponent.getHttpConfigFormGroup(endpoint, isReadonly);

    inherit.valueChanges.pipe(startWith(inherit.value), takeUntil(unsubscribe$)).subscribe((inheritValue) => {
      if (!isReadonly && !inheritValue) {
        configuration.enable({ emitEvent: false });
        return;
      }
      configuration.disable({ emitEvent: false });
    });

    return new UntypedFormGroup({
      inherit,
      configuration,
    });
  }

  public static getConfigurationFormValue(configurationFormGroup: UntypedFormGroup): EndpointConfigurationData {
    return {
      inherit: configurationFormGroup.get('inherit').value,
      configuration: EndpointHttpConfigComponent.getHttpConfigValue(configurationFormGroup.get('configuration') as UntypedFormGroup),
    };
  }

  @Input() configurationFormGroup: UntypedFormGroup;
  protected readonly FormGroup = UntypedFormGroup;
}
