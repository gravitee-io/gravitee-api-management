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
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';

import { ApiCardComponent } from './api-card.component';
import { ApiCardHarness } from './api-card.harness';

describe('CardComponent', () => {
  let component: ApiCardComponent;
  let fixture: ComponentFixture<ApiCardComponent>;
  let harness: ApiCardHarness;
  const api = {
    title: 'Test title',
    version: 'v.1',
    content:
      'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
    id: '1',
  };
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApiCardComponent, RouterModule.forRoot([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiCardComponent);
    component = fixture.componentInstance;
    component.apiId = api.id;
    component.title = api.title;
    component.version = api.version;
    component.content = api.content;
    fixture.componentRef.setInput('typeLabel', 'API');

    fixture.detectChanges();
    harness = await TestbedHarnessEnvironment.harnessForFixture(fixture, ApiCardHarness);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display data in card', async () => {
    expect(await harness.getTitle()).toEqual('Test title');
    expect(await harness.getDescription()).toContain(
      'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
    );
    expect(await harness.getType()).toBe('API');
    expect(await harness.isMcpServer()).toBe(false);
  });

  it('should emit the API id when selected', async () => {
    const selected = jest.fn();
    component.cardSelect.subscribe(selected);

    await harness.select();

    expect(selected).toHaveBeenCalledWith(api.id);
  });
});
