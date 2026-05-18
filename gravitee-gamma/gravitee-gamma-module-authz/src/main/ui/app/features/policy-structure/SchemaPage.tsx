import {
    Alert,
    AlertDescription,
    AlertTitle,
    Badge,
    Button,
    Card,
    Empty,
    EmptyDescription,
    EmptyHeader,
    EmptyTitle,
    Spinner,
} from '@gravitee/graphene-core';
import { Download } from 'lucide-react';
import type * as monacoNs from 'monaco-editor';
import { useMemo, useRef, useState } from 'react';
import { MonacoEditor } from '../../../components/MonacoEditor';
import { parseGaplSchema } from '../../../lib/gapl-parser';
import { useSchema } from '../../../lib/hooks/useSchema';
import { useEnvironment } from '../../lib/env/EnvironmentContext';
import { SchemaOutline } from './SchemaOutline';
import { getEntityCategoryId } from './entity-types';

function KpiPill({ label, value }: { label: string; value: number }) {
    return (
        <Card style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', padding: '0.375rem 0.75rem' }}>
            <Badge
                variant="secondary"
                style={{ fontFamily: 'var(--font-mono, ui-monospace, monospace)', fontVariantNumeric: 'tabular-nums' }}
            >
                {value}
            </Badge>
            <span style={{ fontSize: '0.75rem', color: 'var(--muted-foreground, #6b7280)' }}>{label}</span>
        </Card>
    );
}

function downloadTextAsFile(text: string, filename: string): void {
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}

export function SchemaPage() {
    const environmentId = useEnvironment();
    const { schema, notFound, isLoading, error } = useSchema(environmentId);

    const [activeLine, setActiveLine] = useState<number | undefined>(undefined);
    const editorRef = useRef<monacoNs.editor.IStandaloneCodeEditor | null>(null);

    const schemaText = schema?.schemaText ?? '';
    const parsed = useMemo(() => parseGaplSchema(schemaText), [schemaText]);

    const kpiCounts = useMemo(() => {
        let principalKinds = 0;
        let resourceKinds = 0;
        for (const ent of parsed.entities) {
            const cat = getEntityCategoryId(ent.name);
            if (cat === 'principal') {
                principalKinds++;
            } else {
                resourceKinds++;
            }
        }
        return {
            entities: parsed.entities.length,
            actions: parsed.actions.length,
            principalKinds,
            resourceKinds,
        };
    }, [parsed]);

    const handleEditorMount = (editor: monacoNs.editor.IStandaloneCodeEditor) => {
        editorRef.current = editor;
    };

    const handleJump = (line: number) => {
        setActiveLine(line);
        const editor = editorRef.current;
        if (editor) {
            editor.revealLineInCenter(line);
            editor.setPosition({ lineNumber: line, column: 1 });
            editor.focus();
        }
        window.setTimeout(() => setActiveLine(undefined), 1500);
    };

    const onExport = () => {
        downloadTextAsFile(schemaText, 'schema.gapl');
    };

    if (isLoading) {
        return (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Spinner aria-hidden />
                <span>Loading schema…</span>
            </div>
        );
    }

    if (error !== undefined) {
        return (
            <Alert variant="destructive">
                <AlertTitle>Could not load schema</AlertTitle>
                <AlertDescription>{error}</AlertDescription>
            </Alert>
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }} data-testid="page-schema">
            <header style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem' }}>
                <div>
                    <h1>Schema</h1>
                    <p style={{ color: 'var(--muted-foreground, #6b7280)' }}>
                        The GAPL schema is generated automatically from your entities and policies. Add entities or write
                        policies to populate it — this view is read-only.
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                    <Button type="button" variant="outline" onClick={onExport} disabled={!schemaText}>
                        <Download aria-hidden style={{ width: '1rem', height: '1rem', marginRight: '0.375rem' }} />
                        Export
                    </Button>
                </div>
            </header>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                <KpiPill label="entities" value={kpiCounts.entities} />
                <KpiPill label="actions" value={kpiCounts.actions} />
                <KpiPill label="principal kinds" value={kpiCounts.principalKinds} />
                <KpiPill label="resource kinds" value={kpiCounts.resourceKinds} />
            </div>

            {notFound && (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>Schema not available</EmptyTitle>
                        <EmptyDescription>
                            No schema has been derived for this environment yet. Add entities or policies to populate it.
                        </EmptyDescription>
                    </EmptyHeader>
                </Empty>
            )}

            <div
                style={{
                    display: 'grid',
                    gridTemplateColumns: '280px minmax(0, 1fr)',
                    gap: '1rem',
                    alignItems: 'start',
                }}
            >
                <Card
                    style={{
                        overflow: 'hidden',
                        position: 'sticky',
                        top: '1rem',
                        maxHeight: 'calc(100vh - 280px)',
                        overflowY: 'auto',
                    }}
                >
                    <div style={{ borderBottom: '1px solid var(--border)', padding: '0.5rem 0.75rem' }}>
                        <p
                            style={{
                                fontSize: '0.75rem',
                                fontWeight: 600,
                                textTransform: 'uppercase',
                                letterSpacing: '0.05em',
                                color: 'var(--muted-foreground)',
                            }}
                        >
                            Outline
                        </p>
                    </div>
                    <SchemaOutline parsed={parsed} activeLine={activeLine} onJump={handleJump} />
                </Card>

                <div>
                    <MonacoEditor
                        value={schemaText}
                        readOnly
                        height="calc(100vh - 280px)"
                        ariaLabel="GAPL schema viewer"
                        onMount={handleEditorMount}
                    />
                </div>
            </div>
        </div>
    );
}
