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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ActivatedRoute } from '@angular/router';
import { InteractivityChecker } from '@angular/cdk/a11y';
import { HttpTestingController } from '@angular/common/http/testing';

import { ClusterConfigurationComponent } from './cluster-configuration.component';
import { ClusterConfigurationHarness } from './cluster-configuration.harness';

import { GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';
import {
  expectGetClusterRequest,
  expectGetConfigurationSchemaRequest,
  expectUpdateClusterRequest,
} from '../../../../services-ngx/cluster.service.spec';
import { fakeCluster, fakeUpdateCluster } from '../../../../entities/management-api-v2';

const securityJsonSchema = {
  type: 'object',
  properties: {
    bootstrapServers: {
      type: 'string',
    },
    security: {
      type: 'object',
      properties: {
        protocol: {
          title: 'Security protocol',
          type: 'string',
          enum: ['PLAINTEXT', 'SASL_PLAINTEXT', 'SASL_SSL', 'SSL'],
        },
      },
      required: ['protocol'],
    },
  },
  required: ['bootstrapServers'],
};

describe('ClusterConfigurationComponent', () => {
  const CLUSTER_ID = 'clusterId';

  let fixture: ComponentFixture<ClusterConfigurationComponent>;
  let clusterConfigurationHarness: ClusterConfigurationHarness;
  let httpTestingController: HttpTestingController;

  let permissions: string[];

  beforeEach(async () => {
    permissions = ['cluster-configuration-u'];

    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, ClusterConfigurationComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { clusterId: CLUSTER_ID } } } },
        { provide: GioTestingPermissionProvider, useFactory: () => permissions },
      ],
    })
      .overrideProvider(InteractivityChecker, {
        useValue: {
          isFocusable: () => true,
          isTabbable: () => true,
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ClusterConfigurationComponent);
    httpTestingController = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    clusterConfigurationHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ClusterConfigurationHarness);
    expectGetClusterRequest(
      httpTestingController,
      fakeCluster({
        id: CLUSTER_ID,
      }),
    );
    expectGetConfigurationSchemaRequest(httpTestingController, securityJsonSchema);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should display the json schema form', async () => {
    expect(await clusterConfigurationHarness.isJsonSchemaFormPresent()).toBe(true);
  });

  it('should initialize the form with cluster data', async () => {
    expect(await clusterConfigurationHarness.getBootstrapServersValue()).toBe('kafka.example.com:9092');
    expect(await clusterConfigurationHarness.getSecurityTypeValue()).toBe('PLAINTEXT');
  });

  it('should submit the form and update the cluster', async () => {
    await clusterConfigurationHarness.setBootstrapServersValue('updated-server:9092');
    await clusterConfigurationHarness.setSecurityTypeValue('SASL_PLAINTEXT');

    await clusterConfigurationHarness.submitForm();

    expectUpdateClusterRequest(
      httpTestingController,
      CLUSTER_ID,
      fakeUpdateCluster({
        name: fakeCluster().name,
        description: fakeCluster().description,
        configuration: {
          bootstrapServers: 'updated-server:9092',
          security: {
            protocol: 'SASL_PLAINTEXT',
          },
        },
      }),
    );

    // Trigger new NgOnInit to refresh the data
    expectGetClusterRequest(
      httpTestingController,
      fakeCluster({
        id: CLUSTER_ID,
      }),
    );
    expectGetConfigurationSchemaRequest(httpTestingController, securityJsonSchema);
  });
});
