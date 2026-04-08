import { createReactBlockSpec } from '@blocknote/react';
import { ApiListView, type ViewMode } from './ApiListView';
import styles from './ApiListBlock.module.scss';

export const ApiListBlock = createReactBlockSpec(
  {
    type: 'graviteeApiList' as const,
    propSchema: {
      title: { default: 'Top APIs' },
      limit: { default: '5' },
      category: { default: '' },
      viewMode: { default: 'cards' as ViewMode },
    },
    content: 'none',
  },
  {
    render: ({ block, editor }) => {
      const { title, limit, category, viewMode } = block.props;
      const isEditable = editor.isEditable;

      const setViewMode = (mode: ViewMode) => {
        editor.updateBlock(block, { props: { viewMode: mode } });
      };

      return (
        <div className={`${styles.wrapper} ${isEditable ? styles.editable : ''}`}>
          {isEditable && (
            <div className={styles.floatingToolbar}>
              <button
                className={`${styles.iconBtn} ${viewMode === 'cards' ? styles.active : ''}`}
                onClick={() => setViewMode('cards')}
                title="Cards"
                type="button"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <rect x="3" y="3" width="7" height="7" rx="1" />
                  <rect x="14" y="3" width="7" height="7" rx="1" />
                  <rect x="3" y="14" width="7" height="7" rx="1" />
                  <rect x="14" y="14" width="7" height="7" rx="1" />
                </svg>
              </button>
              <button
                className={`${styles.iconBtn} ${viewMode === 'list' ? styles.active : ''}`}
                onClick={() => setViewMode('list')}
                title="List"
                type="button"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <line x1="3" y1="6" x2="21" y2="6" />
                  <line x1="3" y1="12" x2="21" y2="12" />
                  <line x1="3" y1="18" x2="21" y2="18" />
                </svg>
              </button>
            </div>
          )}

          <ApiListView
            title={title}
            limit={Number(limit)}
            category={category}
            viewMode={viewMode as ViewMode}
            clickable={!isEditable}
          />
        </div>
      );
    },
  },
);
