import '@blocknote/mantine/style.css';
import { useState, useEffect } from 'react';
import { BlockNoteView } from '@blocknote/mantine';
import { useCreateBlockNote } from '@blocknote/react';
import { locales as multiColumnLocales } from '@blocknote/xl-multi-column';
import { en as coreEn } from '@blocknote/core/locales';
import { schema } from '../blocks/schema';
import { loadDocument } from '../utils/storage';
import { type PageWidth, PAGE_WIDTH_VALUES } from './Editor';
import styles from './Viewer.module.scss';

type PartialBlockType = typeof schema.PartialBlock;

function ViewerInner({ content, pageWidth }: { content: PartialBlockType[]; pageWidth: PageWidth }) {
  const editor = useCreateBlockNote({
    schema,
    initialContent: content,
    dictionary: {
      ...coreEn,
      multi_column: multiColumnLocales.en,
    },
  });

  return (
    <div
      className={styles.container}
      style={{ '--page-width': PAGE_WIDTH_VALUES[pageWidth] } as React.CSSProperties}
    >
      <BlockNoteView editor={editor} editable={false} />
    </div>
  );
}

export function Viewer({ pageWidth = 'narrow' }: { pageWidth?: PageWidth }) {
  const [content, setContent] = useState<PartialBlockType[] | undefined | 'loading'>('loading');

  useEffect(() => {
    loadDocument<PartialBlockType[]>().then((doc) => setContent(doc));
  }, []);

  if (content === 'loading') {
    return <div className={styles.container}>Loading...</div>;
  }

  if (!content) {
    return (
      <div className={styles.empty}>
        <p>No published content yet.</p>
        <a href="/edit" className={styles.editLink}>
          Open editor
        </a>
      </div>
    );
  }

  return <ViewerInner content={content} pageWidth={pageWidth} />;
}
