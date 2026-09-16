/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { render, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { FileDropzone, RULESET_FILE_ACCEPT } from './FileDropzone';

describe('FileDropzone', () => {
    afterEach(() => {
        jest.restoreAllMocks();
    });
    it('reads a dropped file and reports its name and content', async () => {
        const user = userEvent.setup();
        const onFile = jest.fn();
        const onEmptyFile = jest.fn();
        render(<FileDropzone accept={RULESET_FILE_ACCEPT} formatsHint="yml, yaml, json" onFile={onFile} onEmptyFile={onEmptyFile} />);

        const input = document.querySelector('input[type="file"]') as HTMLInputElement;
        await user.upload(input, new File(['rules: []'], 'style.yaml', { type: 'text/yaml' }));

        await waitFor(() => expect(onFile).toHaveBeenCalledWith({ name: 'style.yaml', content: 'rules: []' }));
        expect(onEmptyFile).not.toHaveBeenCalled();
    });

    it('reports an empty file instead of importing it', async () => {
        const user = userEvent.setup();
        const onFile = jest.fn();
        const onEmptyFile = jest.fn();
        render(<FileDropzone accept={RULESET_FILE_ACCEPT} formatsHint="yml, yaml, json" onFile={onFile} onEmptyFile={onEmptyFile} />);

        const input = document.querySelector('input[type="file"]') as HTMLInputElement;
        await user.upload(input, new File([], 'empty.yaml', { type: 'text/yaml' }));

        await waitFor(() => expect(onEmptyFile).toHaveBeenCalled());
        expect(onFile).not.toHaveBeenCalled();
    });

    it('reports a FileReader failure instead of leaving an unhandled rejection', async () => {
        const user = userEvent.setup();
        const onFile = jest.fn();
        const onEmptyFile = jest.fn();
        const onReadError = jest.fn();
        const readError = new Error('Failed to read file');
        jest.spyOn(FileReader.prototype, 'readAsText').mockImplementation(function (this: FileReader) {
            Object.defineProperty(this, 'error', { configurable: true, value: readError });
            this.onerror?.(new ProgressEvent('error'));
        });
        render(
            <FileDropzone
                accept={RULESET_FILE_ACCEPT}
                formatsHint="yml, yaml, json"
                onFile={onFile}
                onEmptyFile={onEmptyFile}
                onReadError={onReadError}
            />,
        );

        const input = document.querySelector('input[type="file"]') as HTMLInputElement;
        await user.upload(input, new File(['rules: []'], 'style.yaml', { type: 'text/yaml' }));

        await waitFor(() => expect(onReadError).toHaveBeenCalled());
        expect(onFile).not.toHaveBeenCalled();
        expect(onEmptyFile).not.toHaveBeenCalled();
    });
});
