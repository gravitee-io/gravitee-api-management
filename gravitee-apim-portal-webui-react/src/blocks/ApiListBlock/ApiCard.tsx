import { useNavigate } from 'react-router-dom';
import type { Api } from '../../entities/api';
import styles from './ApiCard.module.scss';

interface ApiCardProps {
  api: Api;
  clickable?: boolean;
}

export function ApiCard({ api, clickable = false }: ApiCardProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    if (clickable) navigate(`/api/${api.id}`);
  };

  return (
    <div className={`${styles.card} ${clickable ? styles.clickable : ''}`} onClick={handleClick}>
      <div className={styles.header}>
        <span className={styles.name}>{api.name}</span>
        {api.labels?.includes('MCP') && <span className={styles.mcpBadge}>MCP</span>}
      </div>
      <p className={styles.description}>
        {api.description || 'Description for this API is missing.'}
      </p>
      {api.labels && api.labels.filter(l => l !== 'MCP').length > 0 && (
        <div className={styles.labels}>
          {api.labels.filter(l => l !== 'MCP').map((label) => (
            <span key={label} className={styles.label}>{label}</span>
          ))}
        </div>
      )}
    </div>
  );
}
