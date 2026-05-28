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
      sidebar={null /* replace with your <AppSidebar /> */}
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

// 3. With LayoutSlots (module federation):
//    If using useLayoutConfig instead of direct props, push the sidebar from
//    inside your module component:
//
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
//    See Storybook "Patterns/Module Federation" for the full setup.
