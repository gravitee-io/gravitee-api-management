const env = process.env.BACKEND_ENV;
const target = `${env ? `https://${env}` : 'http://localhost:8083'}`
export default {
  '/management': {
    target,
    secure: false,
    changeOrigin: true,
    onProxyReq: function (proxyReq, req, res) {
      proxyReq.setHeader('origin', target);
    },
  },
};
