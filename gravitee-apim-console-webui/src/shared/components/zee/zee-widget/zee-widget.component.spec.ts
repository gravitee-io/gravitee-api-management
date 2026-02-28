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
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { of, throwError } from 'rxjs';

import { ZeeWidgetComponent } from './zee-widget.component';
import { ZeeModule } from '../zee.module';
import { ZeeService } from '../zee.service';
import { ZeeGenerateResponse, ZeeResourceAdapter, ZeeResourceType } from '../zee.model';

import { GioTestingModule } from '../../../testing';

describe('ZeeWidgetComponent', () => {
  let fixture: ComponentFixture<ZeeWidgetComponent>;
  let component: ZeeWidgetComponent;
  let zeeService: ZeeService;

  const mockAdapter: ZeeResourceAdapter = {
    previewLabel: 'Generated Flow',
    transform: jest.fn((generated: any) => ({ name: generated.name })),
  };

  const mockResponse: ZeeGenerateResponse = {
    resourceType: 'FLOW',
    generated: { name: 'Bot-blocker Flow', enabled: true },
    metadata: { model: 'gpt-4o', tokensUsed: 512 },
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZeeModule, GioTestingModule, NoopAnimationsModule, MatIconTestingModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ZeeWidgetComponent);
    component = fixture.componentInstance;
    zeeService = TestBed.inject(ZeeService);

    // Set required inputs
    component.resourceType = ZeeResourceType.FLOW;
    component.adapter = mockAdapter;
    component.contextData = { apiId: 'api-123' };

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should start in idle state', () => {
    expect(component.state).toEqual('idle');
  });

  describe('idle → loading → preview (success path)', () => {
    it('should transition to loading then preview on successful generate', () => {
      jest.spyOn(zeeService, 'generate').mockReturnValue(of(mockResponse));

      component.prompt = 'Build me a rate-limit flow';
      component.onSubmit();

      // Synchronous observable: should land in preview immediately
      expect(component.state).toEqual('preview');
      expect(component.generatedResource).toEqual(mockResponse.generated);
    });

    it('should not submit when prompt is empty', () => {
      const spy = jest.spyOn(zeeService, 'generate');
      component.prompt = '   ';
      component.onSubmit();
      expect(spy).not.toHaveBeenCalled();
      expect(component.state).toEqual('idle');
    });

    it('should pass resourceType and contextData to the service', () => {
      const spy = jest.spyOn(zeeService, 'generate').mockReturnValue(of(mockResponse));
      component.prompt = 'Generate a flow';
      component.onSubmit();

      expect(spy).toHaveBeenCalledWith(
        expect.objectContaining({
          resourceType: ZeeResourceType.FLOW,
          prompt: 'Generate a flow',
          contextData: { apiId: 'api-123' },
        }),
        [],
      );
    });
  });

  describe('idle → loading → error (failure path)', () => {
    it('should transition to error state on service failure', () => {
      jest.spyOn(zeeService, 'generate').mockReturnValue(throwError(() => ({ message: 'Network error' })));

      component.prompt = 'Generate something';
      component.onSubmit();

      expect(component.state).toEqual('error');
      expect(component.errorMessage).toContain('Network error');
    });

    it('should use fallback message if error has no message', () => {
      jest.spyOn(zeeService, 'generate').mockReturnValue(throwError(() => ({})));

      component.prompt = 'Generate something';
      component.onSubmit();

      expect(component.state).toEqual('error');
      expect(component.errorMessage).toEqual('Generation failed');
    });
  });

  describe('preview → accepted', () => {
    beforeEach(() => {
      jest.spyOn(zeeService, 'generate').mockReturnValue(of(mockResponse));
      component.prompt = 'Generate a flow';
      component.onSubmit();
      // Now in preview state
    });

    it('should emit accepted with transformed payload and reset to idle', () => {
      const emittedValues: any[] = [];
      component.accepted.subscribe((val) => emittedValues.push(val));

      component.onAccept();

      expect(mockAdapter.transform).toHaveBeenCalledWith(mockResponse.generated, { apiId: 'api-123' });
      expect(emittedValues).toHaveLength(1);
      expect(emittedValues[0]).toEqual({ name: 'Bot-blocker Flow' });
      expect(component.state).toEqual('idle');
      expect(component.generatedResource).toBeNull();
    });
  });

  describe('preview → rejected', () => {
    beforeEach(() => {
      jest.spyOn(zeeService, 'generate').mockReturnValue(of(mockResponse));
      component.prompt = 'Generate a flow';
      component.onSubmit();
    });

    it('should emit rejected and reset to idle', () => {
      let rejectedEmitted = false;
      component.rejected.subscribe(() => (rejectedEmitted = true));

      component.onReject();

      expect(rejectedEmitted).toBe(true);
      expect(component.state).toEqual('idle');
      expect(component.prompt).toEqual('');
    });
  });

  describe('error → reset', () => {
    beforeEach(() => {
      jest.spyOn(zeeService, 'generate').mockReturnValue(throwError(() => ({ message: 'Timeout' })));
      component.prompt = 'Generate something';
      component.onSubmit();
      // Now in error state
    });

    it('should reset to idle on reset()', () => {
      expect(component.state).toEqual('error');
      component.reset();
      expect(component.state).toEqual('idle');
      expect(component.errorMessage).toEqual('');
    });
  });

  describe('file handling', () => {
    it('should add files from file input', () => {
      const file = new File(['content'], 'test.json', { type: 'application/json' });
      // JSDOM doesn't have DataTransfer; build a FileList-shaped mock
      const mockFileList = {
        0: file,
        length: 1,
        item: (i: number) => (i === 0 ? file : null),
        [Symbol.iterator]: function* () { yield file; },
      } as unknown as FileList;

      const event = { target: { files: mockFileList } } as unknown as Event;
      component.onFileSelect(event);

      expect(component.files).toHaveLength(1);
      expect(component.files[0].name).toEqual('test.json');
    });

    it('should add files from drag and drop', () => {
      const file = new File(['content'], 'spec.yaml', { type: 'text/yaml' });
      const mockFileList = {
        0: file,
        length: 1,
        item: (i: number) => (i === 0 ? file : null),
        [Symbol.iterator]: function* () { yield file; },
      } as unknown as FileList;

      const event = {
        preventDefault: jest.fn(),
        dataTransfer: { files: mockFileList },
      } as unknown as DragEvent;

      component.onFileDrop(event);

      expect(component.files).toHaveLength(1);
      expect(component.files[0].name).toEqual('spec.yaml');
    });

    it('should remove a file by index', () => {
      const fileA = new File(['a'], 'a.json');
      const fileB = new File(['b'], 'b.json');
      component.files = [fileA, fileB];

      component.removeFile(0);

      expect(component.files).toHaveLength(1);
      expect(component.files[0].name).toEqual('b.json');
    });

    it('should pass accumulated files to the service', () => {
      const spy = jest.spyOn(zeeService, 'generate').mockReturnValue(of(mockResponse));
      const file = new File(['content'], 'context.json');
      component.files = [file];
      component.prompt = 'Generate something with context';

      component.onSubmit();

      expect(spy).toHaveBeenCalledWith(expect.any(Object), [file]);
    });
  });
});
