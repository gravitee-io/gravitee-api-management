/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AvatarPicker } from './AvatarPicker';

describe('AvatarPicker', () => {
    it('should reject a non-image file before encoding it', async () => {
        const onSelect = jest.fn();
        render(<AvatarPicker onSelect={onSelect} onUseDefault={() => undefined} />);

        const file = new File(['not an image'], 'notes.txt', { type: 'text/plain' });
        // accept="image/*" is only a picker hint; browsers can still deliver a non-image file.
        fireEvent.change(screen.getByLabelText('Choose avatar image'), { target: { files: [file] } });

        expect(await screen.findByText('Please choose an image file.')).toBeTruthy();
        expect(onSelect).not.toHaveBeenCalled();
    });

    it('should reject a file larger than 1MB', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();
        render(<AvatarPicker onSelect={onSelect} onUseDefault={() => undefined} />);

        const file = new File([new Uint8Array(1024 * 1024 + 1)], 'huge.png', { type: 'image/png' });
        await user.upload(screen.getByLabelText('Choose avatar image'), file);

        expect(await screen.findByText('Image exceeds the maximum authorized size (1MB)')).toBeTruthy();
        expect(onSelect).not.toHaveBeenCalled();
    });

    it('should accept an image under 1MB', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();
        render(<AvatarPicker onSelect={onSelect} onUseDefault={() => undefined} />);

        const file = new File(['png-bytes'], 'avatar.png', { type: 'image/png' });
        await user.upload(screen.getByLabelText('Choose avatar image'), file);

        await waitFor(() => expect(onSelect).toHaveBeenCalledTimes(1));
        expect(onSelect.mock.calls[0]?.[0]).toMatch(/^data:image\/png;base64,/);
    });

    it('should show an error when the file cannot be read', async () => {
        const onSelect = jest.fn();
        const readSpy = jest.spyOn(FileReader.prototype, 'readAsDataURL').mockImplementation(function (this: FileReader) {
            queueMicrotask(() => {
                this.onerror?.(new ProgressEvent('error') as ProgressEvent<FileReader>);
            });
        });
        render(<AvatarPicker onSelect={onSelect} onUseDefault={() => undefined} />);

        fireEvent.change(screen.getByLabelText('Choose avatar image'), {
            target: { files: [new File(['png-bytes'], 'avatar.png', { type: 'image/png' })] },
        });

        expect(await screen.findByText('Could not read that image. Try another file.')).toBeTruthy();
        expect(onSelect).not.toHaveBeenCalled();
        readSpy.mockRestore();
    });
});
