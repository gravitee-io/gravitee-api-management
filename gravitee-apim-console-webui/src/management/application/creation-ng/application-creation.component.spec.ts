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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';

import { ApplicationCreationComponent } from './application-creation.component';
import { ApplicationCreationHarness } from './application-creation.harness';

describe('ApplicationCreationComponent', () => {
  let component: ApplicationCreationComponent;
  let fixture: ComponentFixture<ApplicationCreationComponent>;
  let componentHarness: ApplicationCreationHarness;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationCreationComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ApplicationCreationComponent);
    component = fixture.componentInstance;
    componentHarness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApplicationCreationHarness);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
