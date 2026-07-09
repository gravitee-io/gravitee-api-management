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
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatButtonHarness } from '@angular/material/button/testing';
import { MatFormFieldHarness } from '@angular/material/form-field/testing';
import { MatTooltipHarness } from '@angular/material/tooltip/testing';
import { GioFormTagsInputHarness } from '@gravitee/ui-particles-angular';
import { SpanHarness } from '@gravitee/ui-particles-angular/testing';

import { BrandedSendersComponent } from './branded-senders.component';

import { BrandedSender } from '../../../entities/brandedSender';
import { GioTestingModule } from '../../testing';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, BrandedSendersComponent],
  template: `
    <form [formGroup]="form">
      <branded-senders
        formControlName="brandedSenders"
        [defaultFrom]="defaultFrom"
        [defaultSubject]="defaultSubject"
        [inheritedFromOrg]="inheritedFromOrg"
        [canReset]="canReset"
        (reset)="resetEmitted = resetEmitted + 1"
      />
    </form>
  `,
})
class TestHostComponent {
  defaultFrom = '';
  defaultSubject = '';
  inheritedFromOrg = false;
  canReset = false;
  resetEmitted = 0;
  form = new FormGroup({
    brandedSenders: new FormControl<BrandedSender[]>([], { nonNullable: true }),
  });
}

describe('BrandedSendersComponent', () => {
  let component: TestHostComponent;
  let fixture: ComponentFixture<TestHostComponent>;
  let loader: HarnessLoader;

  const control = () => component.form.controls.brandedSenders;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, GioTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
  });

  describe('default section', () => {
    it('should display the default from and subject as read-only fields', async () => {
      component.defaultFrom = 'noreply@example.com';
      component.defaultSubject = '[Example] %s';
      fixture.detectChanges();

      const defaultFromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Default From' }));
      const defaultFromInput = (await defaultFromField.getControl(MatInputHarness))!;
      expect(await defaultFromInput.getValue()).toBe('noreply@example.com');
      expect(await defaultFromInput.isDisabled()).toBe(true);

      const defaultSubjectField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Default subject prefix' }));
      const defaultSubjectInput = (await defaultSubjectField.getControl(MatInputHarness))!;
      expect(await defaultSubjectInput.getValue()).toBe('[Example] %s');
      expect(await defaultSubjectInput.isDisabled()).toBe(true);
    });

    it('should explain, via each lock tooltip, that the default fields are a read-only preview', async () => {
      const tooltips = await loader.getAllHarnesses(MatTooltipHarness);
      const messages: string[] = [];
      for (const tooltip of tooltips) {
        await tooltip.show();
        messages.push(await tooltip.getTooltipText());
        await tooltip.hide();
      }

      expect(messages).toContain('Read-only preview of the Default From configured in the Email settings above');
      expect(messages).toContain('Read-only preview of the Default subject prefix configured in the Email settings above');
    });
  });

  describe('rendering the form control value', () => {
    it('should render a configuration provided through the form control', async () => {
      control().setValue([{ domains: ['example.com'], from: 'noreply@example.com', subject: '[Example] %s' }]);
      fixture.detectChanges();

      const domainsField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Recipient domains' }));
      const domainsInput = (await domainsField.getControl(GioFormTagsInputHarness))!;
      expect(await domainsInput.getTags()).toEqual(['example.com']);

      const fromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'From' }));
      expect(await (await fromField.getControl(MatInputHarness))!.getValue()).toBe('noreply@example.com');

      const subjectField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Subject prefix' }));
      expect(await (await subjectField.getControl(MatInputHarness))!.getValue()).toBe('[Example] %s');
    });

    it('should render no configurations when the form control value is null', async () => {
      control().setValue(null as unknown as BrandedSender[]);
      fixture.detectChanges();

      const deleteButtons = await loader.getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Delete configuration"]' }));
      expect(deleteButtons.length).toBe(0);
    });
  });

  describe('adding and removing configurations', () => {
    it('should add an empty configuration when "Add configuration" is clicked', async () => {
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      await addButton.click();

      expect(control().value).toEqual([{ domains: [], from: '', subject: '' }]);
    });

    it('should remove a configuration when its delete button is clicked', async () => {
      control().setValue([{ domains: ['example.com'], from: 'noreply@example.com', subject: '' }]);
      fixture.detectChanges();

      const deleteButton = await loader.getHarness(MatButtonHarness.with({ selector: '[aria-label="Delete configuration"]' }));
      await deleteButton.click();

      expect(control().value).toEqual([]);
    });

    it('should remove the correct configuration when one of several is deleted', async () => {
      control().setValue([
        { domains: ['a.com'], from: 'a@a.com', subject: 'A' },
        { domains: ['b.com'], from: 'b@b.com', subject: 'B' },
        { domains: ['c.com'], from: 'c@c.com', subject: 'C' },
      ]);
      fixture.detectChanges();

      const deleteButtons = await loader.getAllHarnesses(MatButtonHarness.with({ selector: '[aria-label="Delete configuration"]' }));
      await deleteButtons[1].click();

      expect(control().value).toEqual([
        { domains: ['a.com'], from: 'a@a.com', subject: 'A' },
        { domains: ['c.com'], from: 'c@c.com', subject: 'C' },
      ]);
    });
  });

  describe('propagating edits', () => {
    it('should propagate field edits back to the form control', async () => {
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      await addButton.click();

      const domainsField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Recipient domains' }));
      await (await domainsField.getControl(GioFormTagsInputHarness))!.addTag('example.com');

      const fromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'From' }));
      await (await fromField.getControl(MatInputHarness))!.setValue('noreply@example.com');

      const subjectField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Subject prefix' }));
      await (await subjectField.getControl(MatInputHarness))!.setValue('[Example] %s');

      expect(control().value).toEqual([{ domains: ['example.com'], from: 'noreply@example.com', subject: '[Example] %s' }]);
    });

    it('should normalize domains (trim + lowercase) and trim the from address when propagating', async () => {
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      await addButton.click();

      const domainsField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Recipient domains' }));
      await (await domainsField.getControl(GioFormTagsInputHarness))!.addTag('Example.COM');

      const fromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'From' }));
      await (await fromField.getControl(MatInputHarness))!.setValue('  noreply@example.com  ');

      expect(control().value).toEqual([{ domains: ['example.com'], from: 'noreply@example.com', subject: '' }]);
    });
  });

  describe('disabled state', () => {
    it('should disable every field when the form control is disabled', async () => {
      control().setValue([{ domains: ['example.com'], from: 'noreply@example.com', subject: '' }]);
      control().disable();
      fixture.detectChanges();

      const domainsField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Recipient domains' }));
      expect(await (await domainsField.getControl(GioFormTagsInputHarness))!.isDisabled()).toBe(true);

      const fromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'From' }));
      expect(await (await fromField.getControl(MatInputHarness))!.isDisabled()).toBe(true);

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addButton.isDisabled()).toBe(true);
    });

    it('should stay disabled when the value is rewritten while disabled (e.g. a save-bar Discard)', async () => {
      control().setValue([{ domains: ['example.com'], from: 'noreply@example.com', subject: '' }]);
      control().disable();
      fixture.detectChanges();

      // A later writeValue rebuilds the fields; without re-applying the disabled state they would silently re-enable.
      control().setValue([{ domains: ['example.org'], from: 'noreply@example.org', subject: '' }]);
      fixture.detectChanges();

      const fromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'From' }));
      expect(await (await fromField.getControl(MatInputHarness))!.isDisabled()).toBe(true);
    });

    it('should re-enable every field and the add button when the control is re-enabled', async () => {
      control().setValue([{ domains: ['example.com'], from: 'noreply@example.com', subject: '' }]);
      control().disable();
      fixture.detectChanges();

      control().enable();
      fixture.detectChanges();

      const fromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'From' }));
      expect(await (await fromField.getControl(MatInputHarness))!.isDisabled()).toBe(false);

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      expect(await addButton.isDisabled()).toBe(false);
    });
  });

  describe('validation', () => {
    it('should be valid with no configurations', () => {
      control().setValue([]);
      fixture.detectChanges();
      expect(control().valid).toBe(true);
    });

    it('should be valid with a well-formed configuration', () => {
      control().setValue([{ domains: ['example.com', 'example.org'], from: 'noreply@example.com', subject: '[Example] %s' }]);
      fixture.detectChanges();
      expect(control().valid).toBe(true);
    });

    it('should accept a from address that uses a display name', () => {
      control().setValue([{ domains: ['example.com'], from: 'Example Team <noreply@example.com>', subject: '' }]);
      fixture.detectChanges();
      expect(control().valid).toBe(true);
    });

    it('should be invalid when a configuration has no domains', () => {
      control().setValue([{ domains: [], from: 'noreply@example.com', subject: '' }]);
      fixture.detectChanges();
      expect(control().invalid).toBe(true);
    });

    it('should be invalid when a configuration has a malformed domain', () => {
      control().setValue([{ domains: ['not a domain'], from: 'noreply@example.com', subject: '' }]);
      fixture.detectChanges();
      expect(control().invalid).toBe(true);
    });

    it('should be invalid when the from address is empty', () => {
      control().setValue([{ domains: ['example.com'], from: '', subject: '' }]);
      fixture.detectChanges();
      expect(control().invalid).toBe(true);
    });

    it('should be invalid when the from address is malformed', () => {
      control().setValue([{ domains: ['example.com'], from: 'not-an-email', subject: '' }]);
      fixture.detectChanges();
      expect(control().invalid).toBe(true);
    });

    it('should be invalid when the subject prefix exceeds the maximum length', () => {
      control().setValue([{ domains: ['example.com'], from: 'noreply@example.com', subject: 'x'.repeat(256) }]);
      fixture.detectChanges();
      expect(control().invalid).toBe(true);
    });

    it('should accept domains regardless of case and surrounding whitespace', () => {
      control().setValue([{ domains: ['  Example.COM  '], from: 'noreply@example.com', subject: '' }]);
      fixture.detectChanges();
      expect(control().valid).toBe(true);
    });

    it('should accept an ACE/punycode IDN TLD', () => {
      control().setValue([{ domains: ['example.xn--p1ai'], from: 'noreply@example.xn--p1ai', subject: '' }]);
      fixture.detectChanges();
      expect(control().valid).toBe(true);
    });

    it('should accept a sender address with an uppercase ACE/punycode IDN TLD', () => {
      control().setValue([{ domains: ['example.xn--p1ai'], from: 'noreply@example.XN--P1AI', subject: '' }]);
      fixture.detectChanges();
      expect(control().valid).toBe(true);
    });

    it('should be invalid when the aggregate serialized size exceeds the backend cap', () => {
      const tooMany = Array.from({ length: 60 }, (_unused, index) => ({
        domains: [`example${index}.com`],
        from: `noreply@example${index}.com`,
        subject: `[Example ${index}] %s`,
      }));
      control().setValue(tooMany);
      fixture.detectChanges();

      expect(control().invalid).toBe(true);
      expect(fixture.nativeElement.querySelector('.branded-senders__error')?.textContent).toContain('too large to save');
    });

    it('should be invalid when the same domain is used across configurations', () => {
      control().setValue([
        { domains: ['example.com'], from: 'noreply@example.com', subject: '' },
        { domains: ['EXAMPLE.com', 'example.org'], from: 'noreply@example.org', subject: '' },
      ]);
      fixture.detectChanges();

      expect(control().invalid).toBe(true);
      expect(fixture.nativeElement.querySelector('.branded-senders__error')?.textContent).toContain('example.com');
    });
  });

  describe('inline error messages', () => {
    it('should show an inline error for an invalid domain', async () => {
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      await addButton.click();

      const domainsField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'Recipient domains' }));
      await (await domainsField.getControl(GioFormTagsInputHarness))!.addTag('invalid', 'blur');

      expect(await domainsField.getTextErrors()).toEqual(['Invalid domain(s): invalid']);
    });

    it('should show an inline error when the from address is malformed', async () => {
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      await addButton.click();

      const fromField = await loader.getHarness(MatFormFieldHarness.with({ floatingLabelText: 'From' }));
      const fromInput = (await fromField.getControl(MatInputHarness))!;
      await fromInput.setValue('not-an-email');
      await fromInput.blur();

      expect(await fromField.getTextErrors()).toEqual(['Enter a valid email address, optionally with a display name.']);
    });
  });

  describe('inherited from org badge', () => {
    const badges = () => loader.getAllHarnesses(SpanHarness.with({ selector: '[data-testid="branded-senders-inherited-badge"]' }));

    it('should show an "Inherited from Org" badge on each displayed configuration when inherited', async () => {
      component.inheritedFromOrg = true;
      control().setValue([
        { domains: ['a.com'], from: 'a@a.com', subject: 'A' },
        { domains: ['b.com'], from: 'b@b.com', subject: 'B' },
      ]);
      fixture.detectChanges();

      const shown = await badges();
      expect(shown.length).toBe(2);
      expect(await shown[0].getText()).toBe('Inherited from Org');
    });

    it('should show no badge when inherited but there are no configurations to display', async () => {
      component.inheritedFromOrg = true;
      control().setValue([]);
      fixture.detectChanges();

      expect((await badges()).length).toBe(0);
    });

    it('should show no badge when not inherited, even with configurations present', async () => {
      component.inheritedFromOrg = false;
      control().setValue([{ domains: ['a.com'], from: 'a@a.com', subject: 'A' }]);
      fixture.detectChanges();

      expect((await badges()).length).toBe(0);
    });

    it('should hide the badge once the user edits the list (creating an override)', async () => {
      component.inheritedFromOrg = true;
      control().setValue([{ domains: ['a.com'], from: 'a@a.com', subject: 'A' }]);
      fixture.detectChanges();
      expect((await badges()).length).toBe(1);

      // Adding a configuration diverges from the inherited value -> badge should disappear.
      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      await addButton.click();

      expect((await badges()).length).toBe(0);
    });

    it('should show the badge again after a fresh value is written (e.g. a save or reset reload)', async () => {
      component.inheritedFromOrg = true;
      control().setValue([{ domains: ['a.com'], from: 'a@a.com', subject: 'A' }]);
      fixture.detectChanges();

      const addButton = await loader.getHarness(MatButtonHarness.with({ selector: '.branded-senders__add' }));
      await addButton.click();
      expect((await badges()).length).toBe(0);

      // A reload writes a fresh (server) value, which resets the edited state.
      control().setValue([{ domains: ['org.com'], from: 'org@org.com', subject: 'O' }]);
      fixture.detectChanges();

      expect((await badges()).length).toBe(1);
    });
  });

  describe('reset action', () => {
    const resetButton = () => loader.getHarnessOrNull(MatButtonHarness.with({ selector: '[data-testid="reset-branded-senders"]' }));

    it('should not show the reset button by default', async () => {
      expect(await resetButton()).toBeNull();
    });

    it('should show the reset button when canReset is set', async () => {
      component.canReset = true;
      fixture.detectChanges();

      expect(await resetButton()).not.toBeNull();
    });

    it('should emit reset when the reset button is clicked', async () => {
      component.canReset = true;
      fixture.detectChanges();

      await (await resetButton())!.click();

      expect(component.resetEmitted).toBe(1);
    });
  });
});
