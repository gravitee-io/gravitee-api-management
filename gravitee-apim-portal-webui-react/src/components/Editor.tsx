import '@blocknote/mantine/style.css';
import { BlockNoteView } from '@blocknote/mantine';
import {
  SuggestionMenuController,
  getDefaultReactSlashMenuItems,
  useCreateBlockNote,
} from '@blocknote/react';
import {
  filterSuggestionItems,
  insertOrUpdateBlockForSlashMenu,
} from '@blocknote/core/extensions';
import {
  multiColumnDropCursor,
  getMultiColumnSlashMenuItems,
  locales as multiColumnLocales,
} from '@blocknote/xl-multi-column';
import { en as coreEn } from '@blocknote/core/locales';
import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import { schema } from '../blocks/schema';
import { saveDocument, loadDocument } from '../utils/storage';
import { uploadFile } from '../utils/upload';
import styles from './Editor.module.scss';

type EditorType = typeof schema.BlockNoteEditor;
type PartialBlockType = typeof schema.PartialBlock;

export interface EditorHandle {
  save: () => Promise<void>;
}

const topApisSlashItem = (editor: EditorType) => ({
  title: 'Top 5 APIs',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeApiList' as const,
      props: { title: 'Top 5 APIs', limit: '5', viewMode: 'cards' as const },
    }),
  aliases: ['top', 'top5', 'popular'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  ),
  subtext: 'Display the 5 most popular APIs',
});

const catalogSlashItem = (editor: EditorType) => ({
  title: 'API Catalog',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeApiList' as const,
      props: { title: 'Catalog', limit: '50', viewMode: 'cards' as const },
    }),
  aliases: ['catalog', 'catalogue', 'all', 'apis'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </svg>
  ),
  subtext: 'Display the full API catalog',
});

const bannerSlashItem = (editor: EditorType) => ({
  title: 'Banner',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeBanner' as const,
    }),
  aliases: ['banner', 'hero', 'header', 'welcome'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="2" y="4" width="20" height="16" rx="2" />
      <line x1="8" y1="10" x2="16" y2="10" />
      <line x1="10" y1="14" x2="14" y2="14" />
    </svg>
  ),
  subtext: 'Hero section with title, subtitle, and CTA button',
});

const cardSlashItem = (editor: EditorType) => ({
  title: 'Card',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeCard' as const,
    }),
  aliases: ['card', 'feature', 'info'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="3" y="3" width="18" height="18" rx="3" />
      <line x1="7" y1="9" x2="17" y2="9" />
      <line x1="7" y1="13" x2="13" y2="13" />
    </svg>
  ),
  subtext: 'Card with icon, title, and description',
});

const buttonSlashItem = (editor: EditorType) => ({
  title: 'Button',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeButton' as const,
    }),
  aliases: ['button', 'cta', 'link', 'action'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <rect x="3" y="8" width="18" height="8" rx="4" />
      <line x1="9" y1="12" x2="15" y2="12" />
    </svg>
  ),
  subtext: 'Action button with configurable link',
});

const htmlSlashItem = (editor: EditorType) => ({
  title: 'HTML',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeHtml' as const,
    }),
  aliases: ['html', 'code', 'custom', 'embed', 'web'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="16 18 22 12 16 6" />
      <polyline points="8 6 2 12 8 18" />
    </svg>
  ),
  subtext: 'Custom HTML/CSS block with preview',
});

const markdownSlashItem = (editor: EditorType) => ({
  title: 'Markdown',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeMarkdown' as const,
    }),
  aliases: ['markdown', 'md', 'text', 'rich'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 4h16v16H4z" />
      <path d="M7 15V9l3 3 3-3v6" />
      <path d="M17 12l-2 3h4l-2-3z" />
    </svg>
  ),
  subtext: 'Markdown block with preview',
});

const featuresSlashItem = (editor: EditorType) => ({
  title: 'Features',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeSection' as const,
    }),
  aliases: ['features', 'grid', 'showcase', 'capabilities'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="3" width="20" height="18" rx="2" />
      <line x1="2" y1="9" x2="22" y2="9" />
    </svg>
  ),
  subtext: 'Feature grid with icons, titles, and background',
});

const sectionSlashItem = (editor: EditorType) => ({
  title: 'Section',
  onItemClick: () =>
    insertOrUpdateBlockForSlashMenu(editor, {
      type: 'graviteeContainer' as const,
      children: [
        {
          type: 'paragraph' as any,
        },
      ],
    }),
  aliases: ['section', 'container', 'block', 'area', 'wrapper', 'background'],
  group: 'Gravitee',
  icon: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="4" width="20" height="16" rx="2" />
    </svg>
  ),
  subtext: 'Content section with customizable background',
});

function getCustomSlashMenuItems(editor: EditorType) {
  return [
    ...getDefaultReactSlashMenuItems(editor),
    ...getMultiColumnSlashMenuItems(editor),
    bannerSlashItem(editor),
    sectionSlashItem(editor),
    featuresSlashItem(editor),
    topApisSlashItem(editor),
    catalogSlashItem(editor),
    cardSlashItem(editor),
    buttonSlashItem(editor),
    htmlSlashItem(editor),
    markdownSlashItem(editor),
  ];
}

export type PageWidth = 'narrow' | 'medium' | 'wide';

export const PAGE_WIDTH_VALUES: Record<PageWidth, string> = {
  narrow: '760px',
  medium: '1080px',
  wide: '1400px',
};

interface EditorInnerProps {
  initialContent?: PartialBlockType[];
  pageWidth: PageWidth;
}

const EditorInner = forwardRef<EditorHandle, EditorInnerProps>(
  ({ initialContent, pageWidth }, ref) => {
    const editor = useCreateBlockNote({
      schema,
      initialContent,
      placeholders: {
        default: "Type '/' to insert a block...",
      },
      uploadFile,
      dropCursor: multiColumnDropCursor,
      dictionary: {
        ...coreEn,
        multi_column: multiColumnLocales.en,
      },
    });

    useImperativeHandle(ref, () => ({
      save: () => saveDocument(editor.document),
    }));

    return (
      <div
        className={styles.editorWrapper}
        style={{ '--page-width': PAGE_WIDTH_VALUES[pageWidth] } as React.CSSProperties}
      >
        <BlockNoteView editor={editor} slashMenu={false}>
          <SuggestionMenuController
            triggerCharacter="/"
            getItems={async (query) =>
              filterSuggestionItems(getCustomSlashMenuItems(editor), query)
            }
          />
        </BlockNoteView>
      </div>
    );
  },
);

interface EditorProps {
  pageWidth?: PageWidth;
}

export const Editor = forwardRef<EditorHandle, EditorProps>(({ pageWidth = 'narrow' }, ref) => {
  const [initialContent, setInitialContent] = useState<PartialBlockType[] | undefined | 'loading'>('loading');

  useEffect(() => {
    loadDocument<PartialBlockType[]>().then((doc) => setInitialContent(doc));
  }, []);

  if (initialContent === 'loading') {
    return <div className={styles.editorWrapper}>Loading...</div>;
  }

  return <EditorInner ref={ref} initialContent={initialContent} pageWidth={pageWidth} />;
});
