import { StrictMode } from 'react';
import * as ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './styles.css';
import App from './app/app';

// Suppress benign ResizeObserver warning from Monaco/third-party (browser defers notifications to next frame; not actionable).
window.addEventListener('error', (event) => {
    if (event.message?.includes?.('ResizeObserver loop completed with undelivered notifications')) {
        event.preventDefault();
        event.stopImmediatePropagation();
        return true;
    }
});

const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement);
root.render(
    <StrictMode>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </StrictMode>,
);
