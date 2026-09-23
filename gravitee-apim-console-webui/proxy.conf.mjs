const env = process.env.BACKEND_ENV;
const target = `${env ? `https://${env}` : 'http://localhost:8083'}`;
export default {
  '/management': {
    target,
    secure: false,
    changeOrigin: true,
    onProxyReq: function (proxyReq, req, res) {
      proxyReq.setHeader('origin', target);
      // Docker nginx on :8084 forwards here with X-Forwarded-Proto but no host/port.
      // Management API then advertises http://localhost/management (port 80) and the console fails to boot.
      const forwardedHost = req.headers['x-forwarded-host'] || req.headers.host;
      if (forwardedHost) {
        proxyReq.setHeader('x-forwarded-host', forwardedHost);
      }
    },
  },
};
