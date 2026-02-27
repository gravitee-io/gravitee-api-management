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

import { ApplicationCardComponent } from './application-card.component';
import { AppTestingModule } from '../../testing/app-testing.module';

describe('ApplicationCardComponent', () => {
  let component: ApplicationCardComponent;
  let fixture: ComponentFixture<ApplicationCardComponent>;

  const mockData = {
    applicationId: '1',
    title: 'My App',
    description: 'A test application',
    missingContentMessage: 'Description for this application is missing.',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplicationCardComponent);
    component = fixture.componentInstance;

    component.applicationId = mockData.applicationId;
    component.title = mockData.title;
    component.description = mockData.description;

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the correct title', () => {
    const titleElement = fixture.nativeElement.querySelector('.m3-title-medium');
    expect(titleElement.textContent).toContain(mockData.title);
  });

  it('should display content if available', () => {
    const contentElement = fixture.nativeElement.querySelector('.app-card__description');
    expect(contentElement).not.toBeNull();
    if (contentElement) {
      expect(contentElement.textContent).toContain(mockData.description);
    }
  });

  it('should display missing content message if description is not available', () => {
    component.description = undefined;
    fixture.detectChanges();

    const missingContentElement = fixture.nativeElement.querySelector('.app-card__description span');
    expect(missingContentElement).not.toBeNull();
    if (missingContentElement) {
      expect(missingContentElement.textContent).toContain(mockData.missingContentMessage);
    }
  });

  it('should emit select event with applicationId on click', () => {
    const selectSpy = jest.fn();
    component.select.subscribe(selectSpy);

    fixture.nativeElement.querySelector('gmd-card').click();
    expect(selectSpy).toHaveBeenCalledWith(mockData.applicationId);
  });
});
