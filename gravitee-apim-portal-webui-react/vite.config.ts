/// <reference types='vitest' />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { nxViteTsPaths } from '@nx/vite/plugins/nx-tsconfig-paths.plugin';
import { nxCopyAssetsPlugin } from '@nx/vite/plugins/nx-copy-assets.plugin';

const backendEnv = process.env['BACKEND_ENV'];
const backendTarget = backendEnv ? `https://${backendEnv}` : 'http://localhost:8083';
const portalOrigin = backendTarget.replace('-api', '-portal');

export default defineConfig(() => ({
    root: __dirname,
    cacheDir: '../node_modules/.vite/gravitee-apim-portal-webui-react',
    server: {
        port: 4102,
        host: 'localhost',
        proxy: {
            '/portal/ui/bootstrap': {
                target: backendTarget,
                secure: false,
                changeOrigin: true,
                selfHandleResponse: true,
                configure: (proxy) => {
                    proxy.on('proxyRes', (proxyRes, _req, res) => {
                        let body = Buffer.from('');
                        proxyRes.on('data', (data: Buffer) => {
                            body = Buffer.concat([body, data]);
                        });
                        proxyRes.on('end', () => {
                            res.writeHead(proxyRes.statusCode ?? 200, proxyRes.headers);
                            res.end(body.toString().replace(`${backendTarget}/`, ''));
                        });
                    });
                },
            },
            '/portal': {
                target: backendTarget,
                secure: false,
                changeOrigin: true,
                configure: (proxy) => {
                    proxy.on('proxyReq', (proxyReq) => {
                        proxyReq.setHeader('origin', portalOrigin);
                    });
                },
            },
        },
    },
    preview: {
        port: 4102,
        host: 'localhost',
    },
    plugins: [react(), nxViteTsPaths(), nxCopyAssetsPlugin(['*.md'])],
    build: {
        outDir: '../dist/gravitee-apim-portal-webui-react',
        emptyOutDir: true,
        reportCompressedSize: true,
        commonjsOptions: {
            transformMixedEsModules: true,
        },
    },
}));
