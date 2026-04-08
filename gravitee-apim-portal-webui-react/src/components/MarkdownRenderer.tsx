import { useMemo, type ReactNode } from 'react';
import { CodeBlock } from './CodeBlock';
import styles from './MarkdownRenderer.module.scss';

interface MarkdownRendererProps {
  content: string;
}

interface ParsedNode {
  type: 'text' | 'code';
  content: string;
  language?: string;
}

function parseMarkdown(md: string): ParsedNode[] {
  const nodes: ParsedNode[] = [];
  const codeBlockRegex = /```(\w*)\n([\s\S]*?)```/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = codeBlockRegex.exec(md)) !== null) {
    if (match.index > lastIndex) {
      nodes.push({ type: 'text', content: md.slice(lastIndex, match.index) });
    }
    nodes.push({ type: 'code', content: match[2].trimEnd(), language: match[1] || undefined });
    lastIndex = match.index + match[0].length;
  }

  if (lastIndex < md.length) {
    nodes.push({ type: 'text', content: md.slice(lastIndex) });
  }

  return nodes;
}

function renderTextBlock(text: string): ReactNode[] {
  return text.split('\n').reduce<ReactNode[]>((acc, line, i) => {
    const trimmed = line.trim();

    if (!trimmed) {
      if (i > 0) acc.push(<br key={`br-${i}`} />);
      return acc;
    }

    if (trimmed.startsWith('# ')) {
      acc.push(<h1 key={i}>{renderInline(trimmed.slice(2))}</h1>);
    } else if (trimmed.startsWith('## ')) {
      acc.push(<h2 key={i}>{renderInline(trimmed.slice(3))}</h2>);
    } else if (trimmed.startsWith('### ')) {
      acc.push(<h3 key={i}>{renderInline(trimmed.slice(4))}</h3>);
    } else if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
      acc.push(<li key={i}>{renderInline(trimmed.slice(2))}</li>);
    } else {
      acc.push(<p key={i}>{renderInline(trimmed)}</p>);
    }

    return acc;
  }, []);
}

function renderInline(text: string): ReactNode {
  const parts: ReactNode[] = [];
  const inlineRegex = /(\*\*(.+?)\*\*)|(`(.+?)`)/g;
  let lastIdx = 0;
  let m: RegExpExecArray | null;
  let key = 0;

  while ((m = inlineRegex.exec(text)) !== null) {
    if (m.index > lastIdx) {
      parts.push(text.slice(lastIdx, m.index));
    }
    if (m[2]) {
      parts.push(<strong key={key++}>{m[2]}</strong>);
    } else if (m[4]) {
      parts.push(<code key={key++} className={styles.inlineCode}>{m[4]}</code>);
    }
    lastIdx = m.index + m[0].length;
  }

  if (lastIdx < text.length) {
    parts.push(text.slice(lastIdx));
  }

  return parts.length === 1 ? parts[0] : <>{parts}</>;
}

export function MarkdownRenderer({ content }: MarkdownRendererProps) {
  const nodes = useMemo(() => parseMarkdown(content), [content]);

  return (
    <div className={styles.markdown}>
      {nodes.map((node, i) =>
        node.type === 'code' ? (
          <CodeBlock key={i} language={node.language}>{node.content}</CodeBlock>
        ) : (
          <div key={i}>{renderTextBlock(node.content)}</div>
        ),
      )}
    </div>
  );
}
