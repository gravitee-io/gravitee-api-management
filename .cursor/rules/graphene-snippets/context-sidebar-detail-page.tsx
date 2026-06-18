// Snippet: Resource detail page with ContextSidebar
//
// Use when a user drills down from a list into a single resource (API, Agent,
// Application…) that has enough sub-sections to warrant its own navigation.
// Do NOT use on list pages or pages with fewer than 3 sub-sections (use Tabs).
//
// Replace {PLACEHOLDERS} with your actual values.
// See Storybook "Composed/ContextSidebar" for interactive examples.

import { useState } from 'react';
import { AppLayout, ContentHeader, ContextSidebar, ContextToggleButton } from '@gravitee/graphene-core';
import type { NavGroup } from '@gravitee/graphene-core';
import { FileTextIcon, SettingsIcon, UsersIcon } from '@gravitee/graphene-core/icons';

// 1. Define the sub-sections for your resource type.
const resourceNavGroups: NavGroup[] = [
  {
    label: '{GROUP_LABEL}',
    items: [
      { key: 'overview', title: 'Overview', icon: FileTextIcon },
      { key: 'settings', title: 'Settings', icon: SettingsIcon },
      { key: 'members', title: 'Members', icon: UsersIcon },
    ],
  },
];

// 2. Build your detail page component.
//    The host provides <AppContextBar /> in ContentHeader's appContext slot.
//    Do NOT include the application/module name as the first breadcrumb —
//    it is already shown by AppContextBar. Start at the page level.
export function ResourceDetailPage({
  resource,
  onBack,
}: {
  resource: { name: string; status: string };
  onBack: () => void;
}) {
  const [section, setSection] = useState('overview');
  const [contextExpanded, setContextExpanded] = useState(true);

  const sectionLabel = resourceNavGroups.flatMap((g) => g.items).find((i) => i.key === section)?.title ?? 'Overview';

  return (
    <AppLayout
      viewMode="context"
      contextExpanded={contextExpanded}
      sidebar={null /* replace with your <AppSidebar renderNavigation={...} /> */}
      contextSidebar={
        <ContextSidebar
          header={
            <>
              <p className="truncate text-sm font-semibold">{resource.name}</p>
              <p className="text-xs text-muted-foreground">{resource.status}</p>
            </>
          }
          groups={resourceNavGroups}
          activeItemKey={section}
          onItemSelect={setSection}
        />
      }
      subheader={
        <ContentHeader
          leading={<ContextToggleButton expanded={contextExpanded} onToggle={() => setContextExpanded((v) => !v)} />}
          appContext={null /* replace with <AppContextBar apps={...} activeAppKey={...} ... /> */}
          breadcrumbs={[
            { label: '{LIST_PAGE_LABEL}', onClick: onBack },
            { label: resource.name },
            { label: sectionLabel },
          ]}
          trailing={null /* replace with your trailing (search, user avatar, etc.) */}
        />
      }
    >
      {/* Page content for the active section */}
      <h1>{sectionLabel}</h1>
    </AppLayout>
  );
}

// 3. Content width tier:
//    Detail pages use the **default** content tier (80rem / 1280px). No
//    useLayoutConfig call is needed for width — AppLayout applies it automatically.
//    Do NOT set max-w-* classes on page wrappers.
//
//    Use contentVariant: 'wide' only for DataTable list pages (see data-table-list-page snippet).
//    Use contentVariant: 'full-bleed' only for tool layouts (Policy Studio) or
//    observability pages (dashboards, log/trace explorers).

// 4. With LayoutSlots (module federation):
//    If using useLayoutConfig instead of direct props, push the sidebar from
//    inside your module component. The host owns appContext in ContentHeader;
//    modules only set breadcrumbs, leading, and contextSidebar.
//
//    // appContext lives in the host shell's ContentHeader — modules do not set it.
//    useLayoutConfig(
//      {
//        viewMode: 'context',
//        contextExpanded,
//        contextSidebar: <ContextSidebar header={...} groups={...} ... />,
//        leading: <ContextToggleButton expanded={contextExpanded} onToggle={...} />,
//        breadcrumbs: [...],
//      },
//      [section, contextExpanded],
//    );
//
//    Child pages can call useLayoutConfig for non-overlapping keys without
//    overriding the parent's slots.  For example, a deeply nested page can set
//    contentVariant: 'full-bleed' while the parent owns viewMode and sidebar:
//
//    useLayoutConfig({ contentVariant: 'full-bleed' }, []);
//
//    Each hook only resets the keys it owns on unmount.
//
//    See Storybook "Patterns/Module Federation" for the full setup.
