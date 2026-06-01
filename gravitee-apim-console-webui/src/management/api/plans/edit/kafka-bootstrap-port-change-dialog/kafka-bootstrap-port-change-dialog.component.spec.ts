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
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatCheckboxHarness } from '@angular/material/checkbox/testing';
import { MatIconTestingModule } from '@angular/material/icon/testing';

import { KafkaBootstrapPortChangeDialogComponent } from './kafka-bootstrap-port-change-dialog.component';

describe('KafkaBootstrapPortChangeDialogComponent', () => {
  let fixture: ComponentFixture<KafkaBootstrapPortChangeDialogComponent>;
  let component: KafkaBootstrapPortChangeDialogComponent;
  let loader: HarnessLoader;
  let dialogRefClose: jest.Mock;

  const createComponent = (data: { host?: string; oldPort?: number; newPort?: number }) => {
    dialogRefClose = jest.fn();

    TestBed.configureTestingModule({
      imports: [KafkaBootstrapPortChangeDialogComponent, NoopAnimationsModule, MatIconTestingModule],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatDialogRef, useValue: { close: dialogRefClose } },
      ],
    });

    fixture = TestBed.createComponent(KafkaBootstrapPortChangeDialogComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  };

  describe('with host and ports', () => {
    beforeEach(() => {
      createComponent({ host: 'kafka.example.com', oldPort: 9092, newPort: 9999 });
    });

    it('should render the warning banner', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Warning: changing the bootstrap port will break all consumers of this plan.');
    });

    it('should display the full address change text', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('kafka.example.com:9999');
      expect(compiled.textContent).toContain('kafka.example.com:9092');
    });

    it('should have the Confirm button disabled initially', async () => {
      const confirmButton = await loader.getHarness(MatButtonHarness.with({ text: 'Confirm and save' }));
      expect(await confirmButton.isDisabled()).toBe(true);
    });

    it('should enable the Confirm button after checking the checkbox', async () => {
      const checkbox = await loader.getHarness(MatCheckboxHarness);
      await checkbox.check();

      const confirmButton = await loader.getHarness(MatButtonHarness.with({ text: 'Confirm and save' }));
      expect(await confirmButton.isDisabled()).toBe(false);
    });

    it('should set isConfirmed to true after checking the checkbox', async () => {
      expect(component.isConfirmed).toBe(false);

      const checkbox = await loader.getHarness(MatCheckboxHarness);
      await checkbox.check();

      expect(component.isConfirmed).toBe(true);
    });

    it('should have the Cancel button in the DOM', async () => {
      const cancelButton = await loader.getHarness(MatButtonHarness.with({ text: 'Cancel' }));
      expect(cancelButton).not.toBeNull();
    });

    it('should keep the Confirm button disabled after unchecking the checkbox', async () => {
      const checkbox = await loader.getHarness(MatCheckboxHarness);
      await checkbox.check();
      await checkbox.uncheck();

      const confirmButton = await loader.getHarness(MatButtonHarness.with({ text: 'Confirm and save' }));
      expect(await confirmButton.isDisabled()).toBe(true);
    });
  });

  describe('without host (port-only variant)', () => {
    beforeEach(() => {
      createComponent({ oldPort: 9092, newPort: 9999 });
    });

    it('should display port-only change text', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('9092');
      expect(compiled.textContent).toContain('9999');
    });

    it('should not display host-based address text', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).not.toContain(':9092');
    });
  });

  describe('removing the port (no newPort)', () => {
    beforeEach(() => {
      createComponent({ host: 'kafka.example.com', oldPort: 9092 });
    });

    it('should render the removal title', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('Remove bootstrap port');
    });

    it('should warn that port-based routing will be disabled', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.textContent).toContain('disable port-based routing');
      expect(compiled.textContent).toContain('revert to host-based routing');
      expect(compiled.textContent).toContain('kafka.example.com:9092');
    });
  });
});
