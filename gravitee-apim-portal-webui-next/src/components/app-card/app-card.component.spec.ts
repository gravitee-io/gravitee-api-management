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

import { AppCardComponent } from './app-card.component';
import { PictureComponent } from '../picture/picture.component';

describe('CardComponent', () => {
  let fixture: ComponentFixture<AppCardComponent>;
  let harnessLoader: HarnessLoader;

  const mockData = {
    title: 'Sample Title',
    version: 'v1.0.0',
    owner: 'John Doe',
    content: 'This is a sample content.',
    missingContentMessage: 'Content is missing.',
    picture: 'path/to/picture.png',
    pictureValue: 'hash123',
    routerLinkValue: ['sample-route'],
    buttonCapture: 'Click Me',
    isApi: true,
    id: '123',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MatCardModule,
        MatButtonModule,
        RouterModule.forRoot([]),
        NoopAnimationsModule,
        HttpClientTestingModule,
        AppCardComponent,
        PictureComponent,
      ],
      declarations: [],
    }).compileComponents();

    fixture = TestBed.createComponent(AppCardComponent);
    harnessLoader = TestbedHarnessEnvironment.loader(fixture);

    const component = fixture.componentInstance;
    component.title = mockData.title;
    component.version = mockData.version;
    component.owner = mockData.owner;
    component.content = mockData.content;
    component.missingContentMessage = mockData.missingContentMessage;
    component.picture = mockData.picture;
    component.pictureValue = mockData.pictureValue;
    component.routerLinkValue = mockData.routerLinkValue;
    component.buttonCapture = mockData.buttonCapture;
    component.isApi = mockData.isApi;
    component.id = mockData.id;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should display the correct title', () => {
    const titleElement = fixture.nativeElement.querySelector('.m3-title-medium');
    expect(titleElement.textContent).toContain(mockData.title);
  });

  it('should display the version if isApi is true', () => {
    const versionElement = fixture.nativeElement.querySelector('.m3-body-medium');
    expect(versionElement.textContent).toContain(`Version: ${mockData.version}`);
  });

  it('should display the owner if isApi is false', () => {
    const component = fixture.componentInstance;
    component.isApi = false;
    fixture.detectChanges();

    const ownerElement = fixture.nativeElement.querySelector('.m3-body-medium');
    expect(ownerElement.textContent).toContain(`Owner: ${mockData.owner}`);
  });

  it('should display content if available', () => {
    const contentElement = fixture.nativeElement.querySelector('.app-card__description');
    expect(contentElement).not.toBeNull();
    if (contentElement) {
      expect(contentElement.textContent).toContain(mockData.content);
    }
  });

  it('should display missing content message if content is not available', () => {
    const component = fixture.componentInstance;
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
    expect(routerLinkValue).toEqual(mockData.routerLinkValue[0]);
  });

  it('should display the picture component with the correct inputs', () => {
    const pictureComponent = fixture.nativeElement.querySelector('app-picture');
    expect(pictureComponent.getAttribute('ng-reflect-picture')).toEqual(mockData.picture);
    expect(pictureComponent.getAttribute('ng-reflect-hash-value')).toEqual(mockData.pictureValue);
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
