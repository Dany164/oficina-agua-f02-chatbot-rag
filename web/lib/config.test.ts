import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { loadConfig } from '@/lib/config';

const VARS = [
    'CHATBOT_API_URL',
    'CHATBOT_TIMEOUT_MS',
    'BFF_RATE_LIMIT_MAX_REQUESTS',
    'BFF_RATE_LIMIT_WINDOW_SECONDS',
    'BFF_TRUST_PROXY_HEADERS',
];

function clearVars() {
    for (const name of VARS) {
        delete process.env[name];
    }
}

describe('loadConfig', () => {
    beforeEach(clearVars);
    afterEach(clearVars);

    it('usa los valores por defecto cuando no hay variables', () => {
        expect(loadConfig()).toEqual({
            chatApiUrl: 'http://localhost:8080',
            requestTimeoutMs: 30_000,
            rateLimitMaxRequests: 10,
            rateLimitWindowSeconds: 60,
            maxMessageLength: 500,
            trustProxyHeaders: false,
        });
    });

    it('lee las variables de entorno', () => {
        process.env.CHATBOT_API_URL = 'http://backend:8081';
        process.env.CHATBOT_TIMEOUT_MS = '5000';
        process.env.BFF_RATE_LIMIT_MAX_REQUESTS = '3';
        process.env.BFF_RATE_LIMIT_WINDOW_SECONDS = '30';
        process.env.BFF_TRUST_PROXY_HEADERS = 'true';


        expect(loadConfig()).toMatchObject({
            chatApiUrl: 'http://backend:8081',
            requestTimeoutMs: 5000,
            rateLimitMaxRequests: 3,
            rateLimitWindowSeconds: 30,
            trustProxyHeaders: true,

        });
    });

    it('quita las barras finales de la URL del backend', () => {
        process.env.CHATBOT_API_URL = 'http://backend:8081///';

        expect(loadConfig().chatApiUrl).toBe('http://backend:8081');
    });

    it('rechaza valores que no son enteros positivos', () => {
        process.env.BFF_RATE_LIMIT_MAX_REQUESTS = 'abc';
        expect(() => loadConfig()).toThrow('BFF_RATE_LIMIT_MAX_REQUESTS');

        process.env.BFF_RATE_LIMIT_MAX_REQUESTS = '0';
        expect(() => loadConfig()).toThrow('BFF_RATE_LIMIT_MAX_REQUESTS');
    });

    it('solo activa trustProxyHeaders con el valor "true"', () => {
        process.env.BFF_TRUST_PROXY_HEADERS = 'yes';

        expect(loadConfig().trustProxyHeaders).toBe(false);
    });
});