import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import type { ServerResponse } from 'http'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api/report-v2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true,
        timeout: 3600000,
        proxyTimeout: 3600000,
        selfHandleResponse: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Accept', 'text/event-stream')
            proxyReq.setHeader('Cache-Control', 'no-cache')
            proxyReq.setHeader('Connection', 'keep-alive')
            proxyReq.removeHeader('Accept-Encoding')
          })
          proxy.on('proxyRes', (proxyRes, _req, res) => {
            const serverRes = res as ServerResponse
            serverRes.setHeader('content-type', 'text/event-stream; charset=utf-8')
            serverRes.setHeader('cache-control', 'no-cache, no-transform')
            serverRes.setHeader('connection', 'keep-alive')
            serverRes.setHeader('x-accel-buffering', 'no')
            serverRes.statusCode = proxyRes.statusCode ?? 200
            serverRes.flushHeaders()
            serverRes.socket?.setNoDelay?.(true)
            proxyRes.pipe(serverRes)
          })
        },
      },
      '/api/report-v3': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true,
        timeout: 3600000,
        proxyTimeout: 3600000,
        selfHandleResponse: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Accept', 'text/event-stream')
            proxyReq.setHeader('Cache-Control', 'no-cache')
            proxyReq.setHeader('Connection', 'keep-alive')
            proxyReq.removeHeader('Accept-Encoding')
          })
          proxy.on('proxyRes', (proxyRes, _req, res) => {
            const serverRes = res as ServerResponse
            const headers: Record<string, string> = {
              'content-type': 'text/event-stream',
              'cache-control': 'no-cache, no-transform',
              connection: 'keep-alive',
              'x-accel-buffering': 'no',
            }
            serverRes.writeHead(proxyRes.statusCode ?? 200, headers)
            serverRes.flushHeaders()
            serverRes.socket?.setNoDelay?.(true)
            proxyRes.socket?.setNoDelay?.(true)
            proxyRes.on('data', (chunk: Buffer) => {
              serverRes.write(chunk)
            })
            proxyRes.on('end', () => {
              serverRes.end()
            })
          })
        },
      },
      '/api/shopping': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true,
        timeout: 3600000,
        proxyTimeout: 3600000,
        selfHandleResponse: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Accept', 'text/event-stream')
            proxyReq.setHeader('Cache-Control', 'no-cache')
            proxyReq.setHeader('Connection', 'keep-alive')
            proxyReq.removeHeader('Accept-Encoding')
          })
          proxy.on('proxyRes', (proxyRes, _req, res) => {
            const serverRes = res as ServerResponse
            const headers: Record<string, string> = {
              'content-type': 'text/event-stream',
              'cache-control': 'no-cache, no-transform',
              connection: 'keep-alive',
              'x-accel-buffering': 'no',
            }
            serverRes.writeHead(proxyRes.statusCode ?? 200, headers)
            serverRes.flushHeaders()
            serverRes.socket?.setNoDelay?.(true)
            proxyRes.socket?.setNoDelay?.(true)
            proxyRes.on('data', (chunk: Buffer) => {
              serverRes.write(chunk)
            })
            proxyRes.on('end', () => {
              serverRes.end()
            })
          })
        },
      },
      '/api/sse-test': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true,
        timeout: 3600000,
        proxyTimeout: 3600000,
        selfHandleResponse: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Accept', 'text/event-stream')
            proxyReq.setHeader('Cache-Control', 'no-cache')
            proxyReq.setHeader('Connection', 'keep-alive')
            proxyReq.removeHeader('Accept-Encoding')
          })
          proxy.on('proxyRes', (proxyRes, _req, res) => {
            const serverRes = res as ServerResponse
            const headers: Record<string, string> = {
              'content-type': 'text/event-stream',
              'cache-control': 'no-cache, no-transform',
              connection: 'keep-alive',
              'x-accel-buffering': 'no',
            }
            serverRes.writeHead(proxyRes.statusCode ?? 200, headers)
            serverRes.flushHeaders()
            serverRes.socket?.setNoDelay?.(true)
            proxyRes.socket?.setNoDelay?.(true)
            proxyRes.on('data', (chunk: Buffer) => {
              serverRes.write(chunk)
            })
            proxyRes.on('end', () => {
              serverRes.end()
            })
          })
        },
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        xfwd: true,
        timeout: 3600000,
        proxyTimeout: 3600000,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('Accept', 'text/event-stream')
            proxyReq.setHeader('Cache-Control', 'no-cache')
            proxyReq.setHeader('Connection', 'keep-alive')
          })
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache'
              proxyRes.headers['x-accel-buffering'] = 'no'
              proxyRes.headers['connection'] = 'keep-alive'
            }
          })
        },
      },
    },
  },
})
