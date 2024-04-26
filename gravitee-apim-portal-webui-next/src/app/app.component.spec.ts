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
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppComponent } from './app.component';
import { CompanyTitleHarness } from '../components/company-title/company-title.harness';
import { AppTestingModule } from '../testing/app-testing.module';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;
  let harnessLoader: HarnessLoader;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, AppTestingModule],
    }).compileComponents();
    fixture = TestBed.createComponent(AppComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);
  });

  it('should create the app', () => {
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have the 'Developer Portal' title`, async () => {
    const companyTitleComponent = await harnessLoader.getHarness(CompanyTitleHarness);
    expect(companyTitleComponent).toBeTruthy();

    expect(await companyTitleComponent.getTitle()).toEqual('Developer Portal');
  });
});
