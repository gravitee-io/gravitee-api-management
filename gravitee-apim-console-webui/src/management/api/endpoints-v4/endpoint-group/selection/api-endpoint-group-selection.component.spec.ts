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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { FormControl, FormGroup, FormsModule } from '@angular/forms';
import { LICENSE_CONFIGURATION_TESTING } from '@gravitee/ui-particles-angular';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Observable, of } from 'rxjs';

import { ApiEndpointGroupSelectionHarness } from './api-endpoint-group-selection.harness';
import { ApiEndpointGroupSelectionComponent } from './api-endpoint-group-selection.component';

import { GioLicenseBannerModule } from '../../../../../shared/components/gio-license-banner/gio-license-banner.module';
import { ApiEndpointGroupModule } from '../api-endpoint-group.module';
import { GioTestingModule } from '../../../../../shared/testing';
import { ApiType, ConnectorPlugin } from '../../../../../entities/management-api-v2';
import { ConnectorPluginsV2Service } from '../../../../../services-ngx/connector-plugins-v2.service';

const ENDPOINT_LIST: ConnectorPlugin[] = [
  {
    id: 'mock',
    name: 'Mock',
    description: 'Use a Mock backend to emulate the behaviour of a typical HTTP server and test processes',
    icon: 'mock-icon',
    deployed: true,
    supportedApiType: 'MESSAGE',
    supportedQos: ['AT_MOST_ONCE', 'NONE', 'AT_LEAST_ONCE', 'AUTO'],
  },
  {
    id: 'kafka',
    name: 'Kafka',
    description: 'Publish and subscribe to messages from one or more Kafka topics',
    icon: 'kafka-icon',
    deployed: false,
    supportedApiType: 'MESSAGE',
    supportedQos: ['AT_MOST_ONCE', 'NONE', 'AT_LEAST_ONCE', 'AUTO'],
  },
  {
    id: 'a2a-proxy',
    name: 'A2A Proxy',
    description: 'should be filtered out',
    icon: 'agent-icon',
    deployed: true,
    supportedApiType: 'A2A_PROXY',
    supportedQos: ['NONE'],
  },
];

describe('ApiEndpointGroupSelectionComponent', () => {
  let fixture: ComponentFixture<ApiEndpointGroupSelectionComponent>;
  let harness: ApiEndpointGroupSelectionHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiEndpointGroupModule, NoopAnimationsModule, GioTestingModule, MatIconTestingModule, FormsModule, GioLicenseBannerModule],
      declarations: [ApiEndpointGroupSelectionComponent],
      providers: [
        {
          provide: 'LicenseConfiguration',
          useValue: LICENSE_CONFIGURATION_TESTING,
        },
        {
          provide: ConnectorPluginsV2Service,
          useValue: {
            listEndpointPluginsByApiType(_: ApiType): Observable<ConnectorPlugin[]> {
              return of(ENDPOINT_LIST);
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiEndpointGroupSelectionComponent);
    fixture.componentRef.setInput(
      'endpointGroupTypeForm',
      new FormGroup({
        endpointGroupType: new FormControl(),
      }),
    );
    fixture.componentRef.setInput('requiredQos', ['AUTO']);
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiEndpointGroupSelectionHarness);
    fixture.detectChanges();
  });

  /* We do not show Add endpoint group in case of agent to agent.
     In case of normal MESSAGE api, we hide the agent-to-agent endpoint. */
  it('should not render the AGENT_TO_AGENT endpoint at all', async () => {
    const values = await harness.getAllEndpointIds();
    expect(values).not.toContain('a2a-proxy');
  });

  it('should show license banner when endpoint is not deployed', async () => {
    await harness.selectEndpoint('kafka');
    const banner = await harness.getLicenseBanner();
    expect(banner).not.toBeNull();
  });

  it('should not show license banner when endpoint is deployed', async () => {
    await harness.selectEndpoint('mock');
    const banner = await harness.getLicenseBanner();
    expect(banner).toBeNull();
  });
});
