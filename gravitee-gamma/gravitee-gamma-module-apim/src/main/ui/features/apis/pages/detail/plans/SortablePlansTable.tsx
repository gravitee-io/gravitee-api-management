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
import {
    DndContext,
    KeyboardSensor,
    PointerSensor,
    closestCenter,
    useSensor,
    useSensors,
    type Announcements,
    type DragEndEvent,
} from '@dnd-kit/core';
import { restrictToVerticalAxis } from '@dnd-kit/modifiers';
import { SortableContext, arrayMove, sortableKeyboardCoordinates, useSortable, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Button, DataTablePagination, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@gravitee/graphene-core';
import { GripVerticalIcon } from '@gravitee/graphene-core/icons';
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef, type Row } from '@tanstack/react-table';
import { useState, type ReactNode } from 'react';

interface SortablePlan {
    id: string;
    name: string;
}

interface SortablePlansTableProps<T extends SortablePlan> {
    columns: ColumnDef<T, unknown>[];
    /** The current page of published plans, in the server's order. */
    plans: T[];
    totalCount: number;
    page: number;
    perPage: number;
    emptyMessage: ReactNode;
    onPage: (page: number) => void;
    onPerPage: (perPage: number) => void;
    /** Locks every handle, e.g. while a previous move is still being written. */
    disabled: boolean;
    /** `order` is the plan's new 1-based position among all published plans; `onError` rolls the list back. */
    onReorder: (plan: T, order: number, options: { onError: () => void }) => void;
}

/**
 * The published plans with a drag handle on each row.
 *
 * Built on Graphene's table primitives rather than `DataTable`, which renders its rows itself and gives no way
 * to attach the sortable ref and transform a dragged row needs.
 */
export function SortablePlansTable<T extends SortablePlan>({
    columns,
    plans,
    totalCount,
    page,
    perPage,
    emptyMessage,
    onPage,
    onPerPage,
    disabled,
    onReorder,
}: Readonly<SortablePlansTableProps<T>>) {
    // Tied to the page it was computed from: once the refetched page arrives, the server's order wins.
    const [moved, setMoved] = useState<{ basis: T[]; rows: T[] } | null>(null);
    const rows = moved?.basis === plans ? moved.rows : plans;

    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
    );

    const table = useReactTable({
        data: rows,
        columns,
        getCoreRowModel: getCoreRowModel(),
        getRowId: plan => plan.id,
        manualPagination: true,
    });

    const nameOf = (id: string | number) => rows.find(plan => plan.id === id)?.name ?? '';
    const positionOf = (id: string | number) => (page - 1) * perPage + rows.findIndex(plan => plan.id === id) + 1;
    const announcements: Announcements = {
        onDragStart: ({ active }) => `Picked up ${nameOf(active.id)}, at position ${positionOf(active.id)}.`,
        onDragOver: ({ active, over }) => (over ? `${nameOf(active.id)} is over position ${positionOf(over.id)}.` : undefined),
        onDragEnd: ({ active, over }) => (over ? `${nameOf(active.id)} dropped at position ${positionOf(over.id)}.` : undefined),
        onDragCancel: ({ active }) => `Moving ${nameOf(active.id)} was cancelled.`,
    };

    const handleDragEnd = ({ active, over }: DragEndEvent) => {
        if (!over || active.id === over.id) {
            return;
        }
        const from = rows.findIndex(plan => plan.id === active.id);
        const to = rows.findIndex(plan => plan.id === over.id);
        const plan = rows[from];
        setMoved({ basis: plans, rows: arrayMove(rows, from, to) });
        onReorder(plan, (page - 1) * perPage + to + 1, { onError: () => setMoved(null) });
    };

    return (
        <div className="space-y-2" role="region" aria-label="Plans">
            <div className="overflow-hidden rounded-lg border">
                <DndContext
                    sensors={sensors}
                    collisionDetection={closestCenter}
                    modifiers={[restrictToVerticalAxis]}
                    accessibility={{ announcements }}
                    onDragEnd={handleDragEnd}
                >
                    <Table>
                        <TableHeader>
                            {table.getHeaderGroups().map(group => (
                                <TableRow key={group.id}>
                                    <TableHead className="w-12">
                                        <span className="sr-only">Reorder</span>
                                    </TableHead>
                                    {group.headers.map(header => (
                                        <TableHead key={header.id}>
                                            {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                                        </TableHead>
                                    ))}
                                </TableRow>
                            ))}
                        </TableHeader>
                        <TableBody>
                            {rows.length === 0 ? (
                                <TableRow>
                                    <TableCell colSpan={columns.length + 1} className="h-24 text-center">
                                        {emptyMessage}
                                    </TableCell>
                                </TableRow>
                            ) : (
                                <SortableContext items={rows.map(plan => plan.id)} strategy={verticalListSortingStrategy}>
                                    {table.getRowModel().rows.map(row => (
                                        <SortableRow key={row.id} row={row} disabled={disabled} />
                                    ))}
                                </SortableContext>
                            )}
                        </TableBody>
                    </Table>
                </DndContext>
            </div>
            <DataTablePagination
                page={page}
                pageSize={perPage}
                totalCount={totalCount}
                pageSizeOptions={[10, 25, 50, 100]}
                onPageChange={onPage}
                onPageSizeChange={onPerPage}
            />
        </div>
    );
}

function SortableRow<T extends SortablePlan>({ row, disabled }: Readonly<{ row: Row<T>; disabled: boolean }>) {
    const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
        id: row.original.id,
        disabled,
    });

    return (
        <TableRow
            ref={setNodeRef}
            data-dragging={isDragging ? 'true' : undefined}
            style={{ transform: CSS.Translate.toString(transform), transition, position: 'relative', zIndex: isDragging ? 1 : undefined }}
        >
            <TableCell>
                <Button
                    ref={setActivatorNodeRef}
                    variant="ghost"
                    size="icon"
                    className="size-8 cursor-grab touch-none"
                    disabled={disabled}
                    aria-label={`Reorder ${row.original.name}`}
                    {...attributes}
                    {...listeners}
                >
                    <GripVerticalIcon aria-hidden="true" className="size-4" />
                </Button>
            </TableCell>
            {row.getVisibleCells().map(cell => (
                <TableCell key={cell.id} className="min-w-0">
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                </TableCell>
            ))}
        </TableRow>
    );
}
