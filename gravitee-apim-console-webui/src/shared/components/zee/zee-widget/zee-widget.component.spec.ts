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
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { ZeeWidgetComponent, MAX_PROMPT_LENGTH } from './zee-widget.component';

import { ZeeModule } from '../zee.module';
import { ZeeService } from '../zee.service';
import { ZeeGenerateResponse, ZeeResourceAdapter, ZeeResourceType } from '../zee.model';
import { GioTestingModule } from '../../../testing';

/** JSDOM lacks DataTransfer — create a minimal FileList-shaped stub for unit tests. */
const createFileList = (...files: File[]): FileList => {
  const fileList = {
    length: files.length,
    item: (i: number) => files[i] ?? null,
    [Symbol.iterator]: function* () {
      yield* files;
    },
  };
  files.forEach((f, i) => {
    (fileList as unknown as Record<number, File>)[i] = f;
  });
  return fileList as unknown as FileList;
};

describe('ZeeWidgetComponent', () => {
  let fixture: ComponentFixture<ZeeWidgetComponent>;
  let component: ZeeWidgetComponent;
  let zeeService: ZeeService;
  let snackBar: MatSnackBar;

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
    snackBar = TestBed.inject(MatSnackBar);

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

    it('should not submit when prompt exceeds MAX_PROMPT_LENGTH', () => {
      const spy = jest.spyOn(zeeService, 'generate');
      component.prompt = 'a'.repeat(MAX_PROMPT_LENGTH + 1);
      component.onSubmit();
      expect(spy).not.toHaveBeenCalled();
      expect(component.state).toEqual('idle');
    });

    it('should submit when prompt is exactly MAX_PROMPT_LENGTH', () => {
      jest.spyOn(zeeService, 'generate').mockReturnValue(of(mockResponse));
      component.prompt = 'a'.repeat(MAX_PROMPT_LENGTH);
      component.onSubmit();
      expect(component.state).toEqual('preview');
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

  describe('429 rate limit handling', () => {
    it('should show snackbar and drop back to idle on 429', () => {
      const snackSpy = jest.spyOn(snackBar, 'open');
      jest.spyOn(zeeService, 'generate').mockReturnValue(throwError(() => ({ status: 429 })));

      component.prompt = 'Generate something';
      component.onSubmit();

      expect(snackSpy).toHaveBeenCalledWith(expect.stringContaining('cooling down'), 'Dismiss', expect.any(Object));
      expect(component.state).toEqual('idle');
      expect(component.errorMessage).toEqual('');
    });

    it('should NOT show snackbar on non-429 errors', () => {
      const snackSpy = jest.spyOn(snackBar, 'open');
      jest.spyOn(zeeService, 'generate').mockReturnValue(throwError(() => ({ status: 500, message: 'Server error' })));

      component.prompt = 'Generate something';
      component.onSubmit();

      expect(snackSpy).not.toHaveBeenCalled();
      expect(component.state).toEqual('error');
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
      const event = { target: { files: createFileList(file) } } as unknown as Event;
      component.onFileSelect(event);

      expect(component.files).toHaveLength(1);
      expect(component.files[0].name).toEqual('test.json');
    });

    it('should add files from drag and drop', () => {
      const file = new File(['content'], 'spec.yaml', { type: 'text/yaml' });
      const event = {
        preventDefault: jest.fn(),
        dataTransfer: { files: createFileList(file) },
      } as unknown as DragEvent;

      component.onFileDrop(event);

      expect(component.files).toHaveLength(1);
      expect(component.files[0].name).toEqual('spec.yaml')
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

  describe('file validation', () => {
    it('should reject files with disallowed extensions', () => {
      const badFile = new File(['binary'], 'malware.exe', { type: 'application/octet-stream' });
      component.addFiles([badFile]);

      expect(component.files).toHaveLength(0);
      expect(component.fileValidationErrors).toHaveLength(1);
      expect(component.fileValidationErrors[0]).toContain('"malware.exe"');
      expect(component.fileValidationErrors[0]).toContain('not an allowed type');
    });

    it('should accept all whitelisted extensions', () => {
      const allowed = ['a.json', 'b.yaml', 'c.yml', 'd.txt', 'e.md'].map((name) => new File(['content'], name));
      component.addFiles(allowed);

      expect(component.files).toHaveLength(5);
      expect(component.fileValidationErrors).toHaveLength(0);
    });

    it('should reject files exceeding maxFileSizeMb', () => {
      component.maxFileSizeMb = 5;
      // Create a file stub with a size property > 5MB
      const bigFile = new File(['x'.repeat(10)], 'huge.json');
      Object.defineProperty(bigFile, 'size', { value: 6 * 1024 * 1024 });

      component.addFiles([bigFile]);

      expect(component.files).toHaveLength(0);
      expect(component.fileValidationErrors[0]).toContain('huge.json');
      expect(component.fileValidationErrors[0]).toContain('5 MB limit');
    });

    it('should reject batch that would exceed MAX_FILES total', () => {
      // Pre-load 4 files
      component.files = Array.from({ length: 4 }, (_, i) => new File(['x'], `f${i}.json`));
      // Attempt to add 2 more (would hit 6 total)
      const incoming = [new File(['a'], 'extra1.json'), new File(['b'], 'extra2.json')];
      component.addFiles(incoming);

      // Only the first should have been accepted (slot limit = 1 remaining)
      expect(component.files).toHaveLength(5);
      expect(component.fileValidationErrors[0]).toContain('Too many files');
    });

    it('should clear count errors after removing a file', () => {
      component.files = Array.from({ length: 5 }, (_, i) => new File(['x'], `f${i}.json`));
      component.fileValidationErrors = ['Too many files. You can attach at most 5 files (currently have 5).'];

      component.removeFile(0);

      // Count error should be cleared
      expect(component.fileValidationErrors.filter((e) => e.startsWith('Too many files'))).toHaveLength(0);
    });

    it('should not set fileValidationErrors when all files are valid', () => {
      const file = new File(['{}'], 'valid.json');
      component.addFiles([file]);

      expect(component.fileValidationErrors).toHaveLength(0);
    });
  });

  describe('character counter guard', () => {
    it('isSubmitDisabled should be true when prompt is empty', () => {
      component.prompt = '';
      expect(component.isSubmitDisabled).toBe(true);
    });

    it('isSubmitDisabled should be true when prompt exceeds MAX_PROMPT_LENGTH', () => {
      component.prompt = 'a'.repeat(MAX_PROMPT_LENGTH + 1);
      expect(component.isSubmitDisabled).toBe(true);
    });

    it('isSubmitDisabled should be false when prompt is within limit', () => {
      component.prompt = 'Hello Zee!';
      expect(component.isSubmitDisabled).toBe(false);
    });
  });
});
