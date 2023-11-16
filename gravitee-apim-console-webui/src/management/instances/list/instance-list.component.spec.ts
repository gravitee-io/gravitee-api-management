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
import { HarnessLoader } from '@angular/cdk/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { InstanceListComponent } from './instance-list.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { GioHttpTestingModule } from '../../../shared/testing';
import { UIRouterStateParams } from '../../../ajs-upgraded-providers';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { InstanceDetailsModule } from '../instance-details/instance-details.module';

describe('InstanceListComponent', () => {
  let fixture: ComponentFixture<InstanceListComponent>;
  let loader: HarnessLoader;
  let rootLoader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioHttpTestingModule, InstanceDetailsModule],
      providers: [{ provide: UIRouterStateParams, useValue: {} }],
    });
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InstanceListComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should create the component', () => {
    expect(fixture).toBeTruthy();
  });
});
