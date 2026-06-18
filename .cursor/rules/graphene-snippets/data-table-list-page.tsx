// Snippet: Entity List page with DataTable
//
// Use when building a list page for server-paginated entity data (APIs, Applications,
// Policies, Agents, Plans, etc.) — any CRUD list where users manage entities.
//
// Content width: DataTable list pages use the **wide** content tier (100rem / 1600px).
// Call `useLayoutConfig({ contentVariant: 'wide' }, [])` at the top of the component.
// Do NOT set max-w-* classes on page wrappers — the design system handles it.
//
// Do NOT use for:
// - Static key-value detail panels (use raw Table)
// - Time-series/triage explorers (logs, traces) — see Record Explorer pattern (deferred)
// - Tables with fewer than 5 fixed rows
//
// Empty state pattern (page-level decision):
// - totalCount === 0 && !hasFilters → DataTableEmptyState variant="first-use" (INSTEAD of DataTable)
// - filteredCount === 0 && hasFilters → DataTableEmptyState variant="no-results" (INSIDE DataTable)
//
// Header button coordination:
// - First-use → header hides "Add" button; empty state owns the primary CTA
// - Has data  → header shows "Add" button (primary); table is main content
// - Both buttons trigger the same creation flow
//
// See Storybook "Composed/DataTableEmptyState → Integration" for interactive example.
//
// Column ordering (left to right):
// 1. Name (link, always first, font-medium hover:underline)
// 2. Status badge (lifecycle state)
// 3. Category badges (type, version, security)
// 4. Dates (relative format + full tooltip)
// 5. Owner/actor (truncated text)
// 6. Actions (always last, sr-only header)
//
// Minimum 3 data columns (excluding actions). Fewer → use Item list or card grid.
// Target 4-7 visible columns. More than 7 → enable column visibility.
// Do NOT show raw entity IDs as visible columns.
//
// Actions column: ALWAYS use MoreVerticalIcon (⋮) + DropdownMenu.
// Never render a row of inline icon buttons. Single-action exception: exactly 1
// action may be a lone icon button without a dropdown.
//
// See Storybook "Patterns/Data Table → ApiList" for interactive example.

import type { ColumnDef, SortingState } from '@tanstack/react-table';
import { useEffect, useReducer, useRef, useState } from 'react';
import {
  Badge,
  Button,
  DataTable,
  DataTableColumnHeader,
  DataTableEmptyState,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  FacetedFilter,
  Input,
  useLayoutConfig,
} from '@gravitee/graphene-core';
import { DateCell } from '@gravitee/graphene-core/composed/DataTable';
import {
  GlobeIcon,
  MoreVerticalIcon,
  PencilIcon,
  PlusIcon,
  SearchIcon,
  Trash2Icon,
} from '@gravitee/graphene-core/icons';

// Replace with your entity type
interface Entity {
  id: string;
  name: string;
  status: 'active' | 'inactive' | 'draft';
  createdAt: string;
  owner: string;
}

// Replace with your API fetch function
declare function fetchEntities(params: {
  page: number;
  perPage: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  search?: string;
  status?: string[];
}): Promise<{ data: Entity[]; totalCount: number }>;

const STATUS_VARIANTS = {
  active: 'success',
  inactive: 'secondary',
  draft: 'outline',
} as const;

const statusOptions = [
  { value: 'active', label: 'Active' },
  { value: 'inactive', label: 'Inactive' },
  { value: 'draft', label: 'Draft' },
];

const columns: ColumnDef<Entity, unknown>[] = [
  {
    accessorKey: 'name',
    header: ({ column }) => <DataTableColumnHeader column={column} title="Name" />,
    cell: ({ row }) => (
      <a href={`#/entities/${row.original.id}`} className="font-medium hover:underline">
        {row.original.name}
      </a>
    ),
  },
  {
    accessorKey: 'status',
    header: ({ column }) => <DataTableColumnHeader column={column} title="Status" />,
    cell: ({ row }) => <Badge variant={STATUS_VARIANTS[row.original.status]}>{row.original.status}</Badge>,
  },
  {
    accessorKey: 'createdAt',
    header: ({ column }) => <DataTableColumnHeader column={column} title="Created" />,
    cell: ({ row }) => <DateCell value={row.original.createdAt} />,
  },
  {
    accessorKey: 'owner',
    header: 'Owner',
    enableSorting: false,
    cell: ({ row }) => <span className="max-w-[150px] truncate text-muted-foreground">{row.original.owner}</span>,
  },
  {
    id: 'actions',
    header: () => <span className="sr-only">Actions</span>,
    enableSorting: false,
    enableHiding: false,
    cell: () => (
      <div className="flex justify-end">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon-xs">
              <MoreVerticalIcon className="size-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem>
              <PencilIcon className="size-3.5" />
              Edit
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem className="text-destructive">
              <Trash2Icon className="size-3.5" />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    ),
  },
];

interface FetchState {
  data: Entity[];
  totalCount: number;
  loading: boolean;
}

type FetchAction = { type: 'loading' } | { type: 'success'; data: Entity[]; totalCount: number };

function fetchReducer(state: FetchState, action: FetchAction): FetchState {
  if (action.type === 'loading') return { ...state, loading: true };
  return { data: action.data, totalCount: action.totalCount, loading: false };
}

export function EntityListPage() {
  useLayoutConfig({ contentVariant: 'wide' }, []);

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [sorting, setSorting] = useState<SortingState>([]);
  const [statusFilter, setStatusFilter] = useState<string[]>([]);
  const [state, dispatch] = useReducer(fetchReducer, { data: [], totalCount: 0, loading: true });

  const debounceRef = useRef<ReturnType<typeof setTimeout>>();
  useEffect(() => {
    debounceRef.current = setTimeout(() => {
      setDebouncedSearch(search);
      setPage(1);
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [search]);

  useEffect(() => {
    let active = true;
    dispatch({ type: 'loading' });
    fetchEntities({
      page,
      perPage: pageSize,
      sortBy: sorting[0]?.id,
      sortOrder: sorting[0]?.desc ? 'desc' : 'asc',
      search: debouncedSearch || undefined,
      status: statusFilter.length ? statusFilter : undefined,
    }).then((res) => {
      if (active) dispatch({ type: 'success', data: res.data, totalCount: res.totalCount });
    });
    return () => {
      active = false;
    };
  }, [page, pageSize, sorting, debouncedSearch, statusFilter]);

  const handleSortingChange = (updater: SortingState | ((prev: SortingState) => SortingState)) => {
    setSorting(typeof updater === 'function' ? updater(sorting) : updater);
    setPage(1);
  };

  const handleStatusChange = (values: string[]) => {
    setStatusFilter(values);
    setPage(1);
  };

  const { data, totalCount, loading } = state;
  const hasFilters = search || statusFilter.length > 0;
  const isFirstUse = totalCount === 0 && !hasFilters && !loading;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Entities</h2>
          <p className="text-sm text-muted-foreground">Manage your entity catalog.</p>
        </div>
        {!isFirstUse && (
          <Button size="sm">
            <PlusIcon />
            Add entity
          </Button>
        )}
      </div>

      {isFirstUse ? (
        <div className="rounded-lg border">
          <DataTableEmptyState
            variant="first-use"
            icon={<GlobeIcon />}
            title="No entities yet"
            description="Get started by creating your first entity."
            primaryAction={
              <Button size="sm">
                <PlusIcon />
                Create entity
              </Button>
            }
          />
        </div>
      ) : (
        <DataTable
          columns={columns}
          data={data}
          loading={loading}
          skeletonCount={pageSize}
          sorting={sorting}
          onSortingChange={handleSortingChange}
          enableColumnVisibility
          serverSide
          pagination={{
            page,
            pageSize,
            totalCount,
            pageSizeOptions: [10, 25, 50, 100],
            onPageChange: setPage,
            onPageSizeChange: (size) => {
              setPageSize(size);
              setPage(1);
            },
          }}
          emptyMessage={
            <DataTableEmptyState
              variant="no-results"
              icon={<SearchIcon />}
              title="No entities match your search"
              description="Try adjusting your search terms or clearing filters."
              action={
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    setSearch('');
                    setStatusFilter([]);
                    setPage(1);
                  }}
                >
                  Clear filters
                </Button>
              }
            />
          }
          toolbar={
            <>
              <Input
                placeholder="Search..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="h-8 w-64"
              />
              <FacetedFilter
                title="Status"
                options={statusOptions}
                selected={statusFilter}
                onChange={handleStatusChange}
              />
              {hasFilters && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setSearch('');
                    setStatusFilter([]);
                    setPage(1);
                  }}
                >
                  Reset
                </Button>
              )}
            </>
          }
        />
      )}
    </div>
  );
}
