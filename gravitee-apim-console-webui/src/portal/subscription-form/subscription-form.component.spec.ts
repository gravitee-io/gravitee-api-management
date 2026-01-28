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

import { SubscriptionFormComponent } from './subscription-form.component';

import { GioTestingModule } from '../../shared/testing';
import { GioPermissionService } from '../../shared/components/gio-permission/gio-permission.service';

describe('SubscriptionFormComponent', () => {
  let fixture: ComponentFixture<SubscriptionFormComponent>;
  let component: SubscriptionFormComponent;

  const init = async (canUpdate: boolean) => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, GioTestingModule, SubscriptionFormComponent],
      providers: [
        {
          provide: GioPermissionService,
          useValue: {
            hasAnyMatching: jest.fn().mockReturnValue(canUpdate),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionFormComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  };

  it('should create component', async () => {
    await init(true);
    expect(component).toBeTruthy();
  });

  it('should load default boilerplate content', async () => {
    await init(true);

    const contentValue = component.contentControl.value;

    expect(contentValue).toContain('Subscription Form Builder');
    expect(contentValue).toContain('Example: Complete Subscription Request Form');
    expect(contentValue).toContain('Applicant Information');
    expect(contentValue).toContain('Usage Details');
    expect(contentValue).toContain('Simplified Quick Access Form');
  });

  it('should disable editor when user has no update permission', async () => {
    await init(false);

    expect(component.contentControl.disabled).toBe(true);
  });

  it('should enable editor when user has update permission', async () => {
    await init(true);

    expect(component.contentControl.disabled).toBe(false);
  });

  it('should allow updating content control value', async () => {
    await init(true);

    component.contentControl.setValue('Custom subscription form content');

    expect(component.contentControl.value).toBe('Custom subscription form content');
  });
});
