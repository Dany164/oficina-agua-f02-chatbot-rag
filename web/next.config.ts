import type { NextConfig } from 'next'

// Orígenes que pueden incrustar /embed en un iframe (widget), separados por comas.
// Por defecto solo este mismo sitio. Ejemplo: WIDGET_ALLOWED_ORIGINS=https://pagina-publica.example
const allowedOrigins = (process.env.WIDGET_ALLOWED_ORIGINS ?? '')
    .split(',')
    .map((origin) => origin.trim())
    .filter((origin) => /^https?:\/\/[^\s;]+$/.test(origin))

const frameAncestors = ["'self'", ...allowedOrigins].join(' ')

const nextConfig: NextConfig = {
    async headers() {
        return [
            {
                source: '/embed',
                headers: [{ key: 'Content-Security-Policy', value: `frame-ancestors ${frameAncestors}` }],
            },
        ]
    },
}

export default nextConfig