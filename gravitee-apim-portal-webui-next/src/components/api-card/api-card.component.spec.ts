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
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';

import { ApiCardComponent } from './api-card.component';

describe('CardComponent', () => {
  let component: ApiCardComponent;
  let fixture: ComponentFixture<ApiCardComponent>;
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
    component.title = api.title;
    component.version = api.version;
    component.content = api.content;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display data in card', () => {
    expect(fixture.nativeElement.querySelector('.m3-title-medium').textContent).toEqual('Test title');
    expect(fixture.nativeElement.querySelector('.api-card__description').innerHTML).toContain(
      'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
    );
  });
});
