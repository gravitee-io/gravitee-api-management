/**
 * Dialog for bulk importing principals from a pasted/uploaded JSON array.
 * Parses, validates, shows a per-row preview, then batch-POSTs to the backend.
 */
import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    cn,
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import { Check, Download, FileText, Upload, X } from 'lucide-react';
import { useRef, useState } from 'react';
import { toBackend } from '../../../lib/entity-adapter';
import type { UseEntitiesResult } from '../../../lib/hooks/useEntities';
import { parsePrincipalJson } from './principal-import';

// ---------- Types -------------------------------------------------------------

interface PrincipalImportDialogProps {
    open: boolean;
    onOpenChange: (next: boolean) => void;
    create: UseEntitiesResult['create'];
    onImported?: (summary: { added: number; skipped: number; failed: number }) => void;
}

type ImportState = 'idle' | 'running' | 'done';

// ---------- Component ---------------------------------------------------------

export function PrincipalImportDialog({ open, onOpenChange, create, onImported }: PrincipalImportDialogProps) {
    const [jsonText, setJsonText] = useState('');
    const [parseResult, setParseResult] = useState<ReturnType<typeof parsePrincipalJson> | null>(null);
    const [importState, setImportState] = useState<ImportState>('idle');
    const [summary, setSummary] = useState<{ added: number; skipped: number; failed: number; failedItems: string[] } | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const handleParse = () => {
        const result = parsePrincipalJson(jsonText.trim());
        setParseResult(result);
        setSummary(null);
    };

    const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = ev => {
            const text = ev.target?.result as string;
            setJsonText(text);
            const result = parsePrincipalJson(text.trim());
            setParseResult(result);
            setSummary(null);
        };
        reader.readAsText(file);
    };

    const validItems = parseResult?.items.filter(item => item.entity && !parseResult.duplicateIndices.has(item.index)) ?? [];

    const handleImport = async () => {
        if (validItems.length === 0) return;
        setImportState('running');

        const results = await Promise.allSettled(validItems.map(item => create(toBackend(item.entity!))));

        let added = 0;
        let failed = 0;
        const failedItems: string[] = [];
        for (let i = 0; i < results.length; i++) {
            const r = results[i];
            if (r.status === 'fulfilled') {
                added++;
            } else {
                failed++;
                const entity = validItems[i].entity!;
                failedItems.push(
                    `${entity.uid.type}::${entity.uid.id}: ${r.reason instanceof Error ? r.reason.message : String(r.reason)}`,
                );
            }
        }
        const skipped = parseResult?.duplicateIndices.size ?? 0;

        const result = { added, skipped, failed, failedItems };
        setSummary(result);
        setImportState('done');
        onImported?.({ added, skipped, failed });
    };

    const handleClose = () => {
        setJsonText('');
        setParseResult(null);
        setSummary(null);
        setImportState('idle');
        onOpenChange(false);
    };

    const canImport = validItems.length > 0 && importState === 'idle';

    return (
        <Dialog
            open={open}
            onOpenChange={next => {
                if (!next) handleClose();
            }}
        >
            <DialogContent className="max-w-2xl">
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <Upload className="size-4 text-muted-foreground" />
                        Import principals from JSON
                    </DialogTitle>
                    <DialogDescription>
                        Paste a JSON array of principal objects or upload a <code className="font-mono text-[11px]">.json</code> file. Each
                        object must have a <code className="font-mono text-[11px]">uid</code> with{' '}
                        <code className="font-mono text-[11px]">type</code> (User / Group / ServiceAccount / AgentIdentity) and{' '}
                        <code className="font-mono text-[11px]">id</code>.
                    </DialogDescription>
                </DialogHeader>

                {importState !== 'done' ? (
                    <div className="space-y-3">
                        {/* File upload */}
                        <div className="flex items-center gap-2">
                            <Button variant="outline" size="sm" onClick={() => fileInputRef.current?.click()}>
                                <FileText className="mr-2 size-3.5" />
                                Upload .json file
                            </Button>
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept="application/json,.json"
                                className="hidden"
                                onChange={handleFileUpload}
                            />
                            <span className="text-xs text-muted-foreground">or paste below</span>
                        </div>

                        {/* Textarea */}
                        <textarea
                            value={jsonText}
                            onChange={e => {
                                setJsonText(e.target.value);
                                setParseResult(null);
                            }}
                            placeholder={`[\n  {\n    "uid": { "type": "User", "id": "alice" },\n    "attrs": { "name": "Alice", "email": "alice@example.com" },\n    "parents": []\n  }\n]`}
                            className="h-48 w-full rounded-md border border-input bg-background px-3 py-2 font-mono text-xs focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                            spellCheck={false}
                        />

                        <Button variant="outline" size="sm" onClick={handleParse} disabled={!jsonText.trim()}>
                            Validate JSON
                        </Button>

                        {/* Parse error */}
                        {parseResult && !parseResult.ok && (
                            <Alert variant="destructive">
                                <AlertTitle>Parse error</AlertTitle>
                                <AlertDescription className="font-mono text-xs">{parseResult.parseError}</AlertDescription>
                            </Alert>
                        )}

                        {/* Preview table */}
                        {parseResult?.ok && parseResult.items.length > 0 && (
                            <div className="space-y-2">
                                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                    <span>
                                        {validItems.length} valid · {parseResult.items.filter(i => i.error).length} invalid ·{' '}
                                        {parseResult.duplicateIndices.size} duplicate
                                    </span>
                                </div>
                                <div className="max-h-48 overflow-auto rounded-md border">
                                    <table className="w-full text-xs">
                                        <thead className="bg-muted/40">
                                            <tr>
                                                <th className="px-3 py-1.5 text-left font-medium">#</th>
                                                <th className="px-3 py-1.5 text-left font-medium">Type</th>
                                                <th className="px-3 py-1.5 text-left font-medium">ID</th>
                                                <th className="px-3 py-1.5 text-left font-medium">Status</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y">
                                            {parseResult.items.map(item => {
                                                const isDuplicate = parseResult.duplicateIndices.has(item.index);
                                                const isInvalid = !!item.error;
                                                return (
                                                    <tr
                                                        key={item.index}
                                                        className={cn(isInvalid || isDuplicate ? 'bg-red-50/50 dark:bg-red-950/20' : '')}
                                                    >
                                                        <td className="px-3 py-1 tabular-nums text-muted-foreground">{item.index + 1}</td>
                                                        <td className="px-3 py-1 font-mono">{item.entity?.uid.type ?? '—'}</td>
                                                        <td className="px-3 py-1 font-mono">{item.entity?.uid.id ?? '—'}</td>
                                                        <td className="px-3 py-1">
                                                            {isInvalid ? (
                                                                <span className="flex items-center gap-1 text-destructive">
                                                                    <X className="size-3" />
                                                                    <span className="line-clamp-1">{item.error}</span>
                                                                </span>
                                                            ) : isDuplicate ? (
                                                                <Badge variant="destructive" className="h-4 px-1 text-[10px]">
                                                                    duplicate
                                                                </Badge>
                                                            ) : (
                                                                <Badge variant="secondary" className="h-4 gap-1 px-1 text-[10px]">
                                                                    <Check className="size-3" />
                                                                    valid
                                                                </Badge>
                                                            )}
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        )}

                        {parseResult?.ok && parseResult.items.length === 0 && (
                            <div className="rounded-md border border-dashed p-4 text-center text-xs text-muted-foreground">
                                The array is empty — nothing to import.
                            </div>
                        )}
                    </div>
                ) : (
                    /* Summary after import */
                    summary && (
                        <div className="space-y-3">
                            <Alert variant={summary.failed > 0 ? 'destructive' : 'default'}>
                                <AlertTitle>Import complete</AlertTitle>
                                <AlertDescription>
                                    {summary.added} imported, {summary.skipped} skipped (duplicates), {summary.failed} failed.
                                </AlertDescription>
                            </Alert>
                            {summary.failedItems.length > 0 && (
                                <div className="space-y-1">
                                    <div className="text-xs font-medium text-muted-foreground">Failed items:</div>
                                    <div className="max-h-32 overflow-auto rounded-md border bg-muted/30 p-2">
                                        {summary.failedItems.map((msg, i) => (
                                            <div key={i} className="font-mono text-[11px] text-destructive">
                                                {msg}
                                            </div>
                                        ))}
                                    </div>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => {
                                            const blob = new Blob([summary.failedItems.join('\n')], { type: 'text/plain' });
                                            const url = URL.createObjectURL(blob);
                                            const a = document.createElement('a');
                                            a.href = url;
                                            a.download = 'import-failures.txt';
                                            a.click();
                                            URL.revokeObjectURL(url);
                                        }}
                                    >
                                        <Download className="mr-2 size-3.5" />
                                        Download failure list
                                    </Button>
                                </div>
                            )}
                        </div>
                    )
                )}

                <DialogFooter>
                    <Button variant="outline" onClick={handleClose}>
                        {importState === 'done' ? 'Close' : 'Cancel'}
                    </Button>
                    {importState !== 'done' && (
                        <Button onClick={() => void handleImport()} disabled={!canImport}>
                            <Upload className="mr-2 size-4" />
                            Import{' '}
                            {validItems.length > 0 ? `${validItems.length} ${validItems.length === 1 ? 'principal' : 'principals'}` : ''}
                        </Button>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
