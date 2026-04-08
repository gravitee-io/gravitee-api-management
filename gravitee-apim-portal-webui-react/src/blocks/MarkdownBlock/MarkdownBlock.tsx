import { useState } from 'react';
import { createReactBlockSpec } from '@blocknote/react';
import { MarkdownRenderer } from '../../components/MarkdownRenderer';
import styles from './MarkdownBlock.module.scss';

type Tab = 'source' | 'preview';

const DEFAULT_MD = `## Getting Started

Welcome to the **Developer Portal**. Here you'll find everything you need to integrate our APIs.

### Quick Links

- Browse the \`API Catalog\` to discover available endpoints
- Read the **Authentication Guide** for setup instructions
- Check out code samples in your favorite language`;

export const MarkdownBlock = createReactBlockSpec(
  {
    type: 'graviteeMarkdown' as const,
    propSchema: {
      markdown: { default: DEFAULT_MD },
    },
    content: 'none',
  },
  {
    render: ({ block, editor }) => {
      const { markdown } = block.props;
      const isEditable = editor.isEditable;
      const [activeTab, setActiveTab] = useState<Tab>('preview');

      if (!isEditable) {
        return (
          <div className={styles.preview}>
            <MarkdownRenderer content={markdown} />
          </div>
        );
      }

      const tabs: { key: Tab; label: string }[] = [
        { key: 'preview', label: 'Preview' },
        { key: 'source', label: 'Markdown' },
      ];

      return (
        <div className={styles.block} style={{ width: '100%' }}>
          <div className={styles.tabs}>
            {tabs.map((tab) => (
              <button
                key={tab.key}
                className={`${styles.tab} ${activeTab === tab.key ? styles.active : ''}`}
                onClick={() => setActiveTab(tab.key)}
                type="button"
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className={styles.body}>
            {activeTab === 'source' && (
              <textarea
                className={styles.editor}
                value={markdown}
                onChange={(e) => editor.updateBlock(block, { props: { markdown: e.target.value } })}
                spellCheck={false}
                placeholder="# Your markdown here..."
              />
            )}
            {activeTab === 'preview' && (
              <div className={styles.preview}>
                <MarkdownRenderer content={markdown} />
              </div>
            )}
          </div>
        </div>
      );
    },
  },
);
