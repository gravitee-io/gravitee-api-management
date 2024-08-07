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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCardHarness } from '@angular/material/card/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';

import { ApiCardComponent } from './api-card.component';
import { PictureComponent } from '../picture/picture.component';
import { TileCardComponent } from '../tile-card/tile-card.component';

describe('ApiCardComponent', () => {
  let component: ApiCardComponent;
  let fixture: ComponentFixture<ApiCardComponent>;
  let harnessLoader: HarnessLoader;

  const mockData = {
    title: 'Test title',
    version: 'v.1',
    id: '1',
    content:
      'Get real-time weather updates, forecasts, and historical data to enhance your applications with accurate weather information.',
    picture: 'path/to/picture.png',
    routerLinkValue: ['.', 'api', '1'],
    buttonCapture: 'Learn More',
    missingContentMessage: 'Description for this API is missing.',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatCardModule,
        MatButtonModule,
        RouterModule.forRoot([]),
        NoopAnimationsModule,
        HttpClientTestingModule,
        ApiCardComponent,
        TileCardComponent,
        PictureComponent,
      ],
      declarations: [],
    }).compileComponents();

    fixture = TestBed.createComponent(ApiCardComponent);
    component = fixture.componentInstance;
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    component.title = mockData.title;
    component.version = mockData.version;
    component.content = mockData.content;
    component.id = mockData.id;
    component.picture = mockData.picture;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the correct title', () => {
    const titleElement = fixture.nativeElement.querySelector('.m3-title-medium');
    expect(titleElement.textContent).toContain(mockData.title);
  });

  it('should display the version', () => {
    const versionElement = fixture.nativeElement.querySelector('.m3-body-medium');
    expect(versionElement.textContent).toContain(`Version: ${mockData.version}`);
  });

  it('should display content if available', () => {
    const contentElement = fixture.nativeElement.querySelector('.app-card__description');
    expect(contentElement).not.toBeNull();
    if (contentElement) {
      expect(contentElement.textContent).toContain(mockData.content);
    }
  });

  it('should display missing content message if content is not available', () => {
    component.content = '';
    fixture.detectChanges();

    const missingContentElement = fixture.nativeElement.querySelector('.app-card__description span');
    expect(missingContentElement).not.toBeNull();
    if (missingContentElement) {
      expect(missingContentElement.textContent).toContain(mockData.missingContentMessage);
    }
  });

  it('should display the correct button text', () => {
    const buttonElement = fixture.nativeElement.querySelector('button');
    expect(buttonElement.textContent).toContain(mockData.buttonCapture);
  });

  it('should have the correct router link value', () => {
    const buttonElement = fixture.nativeElement.querySelector('button');
    const routerLinkValue = buttonElement.getAttribute('ng-reflect-router-link');
    expect(routerLinkValue).toEqual(mockData.routerLinkValue.join(','));
  });

  it('should display the picture component with the correct inputs', () => {
    const pictureComponent = fixture.nativeElement.querySelector('app-picture');
    expect(pictureComponent.getAttribute('ng-reflect-picture')).toEqual(mockData.picture);
    expect(pictureComponent.getAttribute('ng-reflect-hash-value')).toEqual(`${mockData.title} ${mockData.version}`);
  });

  it('should show card list', async () => {
    const cardHarnesses = await harnessLoader.getAllHarnesses(MatCardHarness);
    expect(cardHarnesses.length).toBe(1);
  });

  it('should display card component correctly after scrolled event', async () => {
    const initialCardHarnesses = await harnessLoader.getAllHarnesses(MatCardHarness);
    expect(initialCardHarnesses.length).toBe(1);

    document.querySelector('mat-card')?.dispatchEvent(new Event('scrolled'));
    fixture.detectChanges();

    const updatedCardHarnesses = await harnessLoader.getAllHarnesses(MatCardHarness);
    expect(updatedCardHarnesses.length).toBe(1);
  });

  it('should display card component correctly with search query', async () => {
    fixture.componentInstance.title = 'sample query';
    fixture.detectChanges();

    const cardHarnesses = await harnessLoader.getAllHarnesses(MatCardHarness);
    expect(cardHarnesses.length).toBe(1);
  });
});
