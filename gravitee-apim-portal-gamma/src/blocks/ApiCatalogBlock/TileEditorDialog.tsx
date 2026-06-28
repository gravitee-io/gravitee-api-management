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
import {
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import { useCallback, useEffect, useMemo, useState } from 'react';

import type { Api } from '../../features/editor/entities/api';
import type { BlockNoteDocument } from '../../features/portals/types';

import { TileEditor } from './TileEditor';
import { TileRenderer } from './TileRenderer';
import { MAX_TILE_BLOCKS, serializeTileTemplate } from './tile-template';
import styles from './TileEditorDialog.module.scss';

interface TileEditorDialogProps {
    readonly open: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly tileTemplate: BlockNoteDocument;
    readonly previewApi: Api | null;
    readonly onSave: (document: BlockNoteDocument) => void | Promise<void>;
}

export function TileEditorDialog({
    open,
    onOpenChange,
    tileTemplate,
    previewApi,
    onSave,
}: TileEditorDialogProps) {
    const [draft, setDraft] = useState<BlockNoteDocument>(tileTemplate);

    useEffect(() => {
        if (open) {
            setDraft(tileTemplate);
        }
    }, [open, tileTemplate]);

    const handleSave = useCallback(async () => {
        await onSave(draft);
        onOpenChange(false);
    }, [draft, onOpenChange, onSave]);

    const previewDocument = useMemo(() => draft, [draft]);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className={styles.dialogContent}>
                <DialogHeader>
                    <DialogTitle>Customize API tiles</DialogTitle>
                    <DialogDescription>
                        Design a shared tile layout for all APIs in this catalog. Up to {MAX_TILE_BLOCKS} blocks per
                        tile.
                    </DialogDescription>
                </DialogHeader>

                <div className={styles.tileCanvas}>
                    <TileEditor
                        key={serializeTileTemplate(tileTemplate)}
                        document={tileTemplate}
                        onChange={setDraft}
                    />
                </div>

                {previewApi ? (
                    <div className={styles.previewTile}>
                        <p className={styles.previewLabel}>Preview</p>
                        <div className={styles.previewTileInner}>
                            <TileRenderer api={previewApi} tileTemplate={previewDocument} />
                        </div>
                    </div>
                ) : null}

                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" onClick={handleSave}>
                        Save tile layout
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
