import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const configPath = path.join(root, 'deployment', 'nginx', 'example.com.conf.example')
const config = fs.readFileSync(configPath, 'utf8')

const required = [
  ['canonical domains', /server_name\s+example\.com\s+www\.example\.com;/],
  ['frontend release directory', /root\s+\/home\/blog\/blog\/frontend;/],
  ['current certificate directory', /\/etc\/letsencrypt\/live\/example\.com\//],
  ['HSTS', /add_header\s+Strict-Transport-Security\b[^\r\n]+always;/],
  ['content security policy', /add_header\s+Content-Security-Policy\b[^\r\n]+always;/],
  ['permissions policy', /add_header\s+Permissions-Policy\b[^\r\n]+always;/],
  ['uncached HTML', /location\s*=\s*\/index\.html\s*{[^}]*expires\s+-1;/s],
  ['uncached theme bootstrap', /location\s*=\s*\/theme-bootstrap\.js\s*{[^}]*expires\s+-1;/s],
  ['immutable Vite assets', /location\s+~\*\s+\^\/assets\/[^}]*expires\s+365d;/s],
  ['local API upstream', /proxy_pass\s+http:\/\/127\.0\.0\.1:8080;/],
  ['single security-header owner', /proxy_hide_header\s+X-Frame-Options;/]
]

const forbidden = [
  ['retired subdomain', /\bblog\.example\.com\b/],
  ['retired dist directory', /\/home\/blog\/blog\/dist\b/]
]

const errors = []
for (const [label, pattern] of required) {
  if (!pattern.test(config)) errors.push(`missing ${label}`)
}
for (const [label, pattern] of forbidden) {
  if (pattern.test(config)) errors.push(`contains ${label}`)
}

if (errors.length > 0) {
  console.error(`Deployment config regression failed:\n- ${errors.join('\n- ')}`)
  process.exit(1)
}

console.log('PASS: deployment domain, paths, security headers, and cache policy')
