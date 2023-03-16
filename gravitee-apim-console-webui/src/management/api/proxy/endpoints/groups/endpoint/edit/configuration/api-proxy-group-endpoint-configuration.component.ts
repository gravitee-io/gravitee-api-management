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
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'api-proxy-group-endpoint-configuration',
  template: require('./api-proxy-group-endpoint-configuration.component.html'),
  styles: [require('./api-proxy-group-endpoint-configuration.component.scss')],
})
export class ApiProxyGroupEndpointConfigurationComponent {
  @Input() configurationForm: FormGroup;
  @Input() configurationSchema: unknown;

  public onProxyConfigurationError(error: unknown) {
    // Set error at the end of js task. Otherwise it will be reset on value change
    setTimeout(() => {
      this.configurationForm.get('proxyConfiguration').setErrors(error ? { error: true } : null);
    }, 0);
  }
}
