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
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';

import { ApplicationCardComponent } from './application-card.component';
import { fakeApplication } from '../../entities/application/application.fixture';
import { PictureComponent } from '../picture/picture.component';

describe('ApplicationCardComponent', () => {
  let component: ApplicationCardComponent;
  let fixture: ComponentFixture<ApplicationCardComponent>;

  const mockData = {
    application: {
      ...fakeApplication(),
      id: '1',
      _links: {
        ...fakeApplication()._links,
        picture: 'link1',
        background: 'background',
        members: 'members',
        notifications: 'notifications',
        self: 'self',
      },
    },
    routerLinkValue: ['.', '1'],
    buttonCapture: 'Open',
    missingContentMessage: 'Description for this application is missing.',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MatCardModule, MatButtonModule, RouterModule.forRoot([]), NoopAnimationsModule, HttpClientTestingModule, PictureComponent],
      declarations: [],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationCardComponent);
    component = fixture.componentInstance;

    component.application = mockData.application;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the correct title', () => {
    const titleElement = fixture.nativeElement.querySelector('.m3-title-medium');
    expect(titleElement.textContent).toContain(mockData.application.name);
  });

  it('should display the owner', () => {
    const versionElement = fixture.nativeElement.querySelector('.m3-body-medium');
    expect(versionElement.textContent).toContain(`Owner: ${mockData.application.owner?.display_name}`);
  });

  it('should display content if available', () => {
    const contentElement = fixture.nativeElement.querySelector('.app-card__description');
    expect(contentElement).not.toBeNull();
    if (contentElement) {
      expect(contentElement.textContent).toContain(mockData.application.description);
    }
  });

  it('should display missing content message if content is not available', () => {
    component.application.description = '';
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
    expect(pictureComponent.getAttribute('ng-reflect-picture')).toEqual(mockData.application._links?.picture);
    expect(pictureComponent.getAttribute('ng-reflect-hash-value')).toEqual(`${mockData.application.name}`);
  });
});
