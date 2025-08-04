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

import { ClusterConfigurationComponent } from './cluster-configuration.component';
import { ClusterConfigurationHarness } from './cluster-configuration.harness';

import { GioTestingModule } from '../../../../shared/testing';
import { GioTestingPermissionProvider } from '../../../../shared/components/gio-permission/gio-permission.service';

describe('ClusterConfigurationComponent', () => {
  const CLUSTER_ID = 'clusterId';

  let fixture: ComponentFixture<ClusterConfigurationComponent>;
  let clusterConfigurationHarness: ClusterConfigurationHarness;

  let permissions: string[];

  beforeEach(async () => {
    permissions = ['cluster-definition-u'];

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

    fixture.detectChanges();
    clusterConfigurationHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ClusterConfigurationHarness);
    fixture.detectChanges();
  });

  it('should initialize the form with cluster data', async () => {
    expect(await clusterConfigurationHarness.getBootstrapServersValue()).toBe('kafka-prod.example.com:9092');
    expect(await clusterConfigurationHarness.getSecurityTypeValue()).toBe('SSL');
  });

  it('should validate the form correctly', async () => {
    // Clear the bootstrap servers field (which is required)
    await clusterConfigurationHarness.setBootstrapServersValue('');

    expect(await clusterConfigurationHarness.isFormValid()).toBe(false);

    await clusterConfigurationHarness.setBootstrapServersValue('new-server:9092');

    expect(await clusterConfigurationHarness.isFormValid()).toBe(true);
  });

  it('should submit the form and update the cluster', async () => {
    await clusterConfigurationHarness.setBootstrapServersValue('updated-server:9092');
    await clusterConfigurationHarness.setSecurityTypeValue('PLAINTEXT');

    await clusterConfigurationHarness.submitForm();
    fixture.detectChanges();

    expect(await clusterConfigurationHarness.getBootstrapServersValue()).toBe('updated-server:9092');
    expect(await clusterConfigurationHarness.getSecurityTypeValue()).toBe('PLAINTEXT');
  });
});
