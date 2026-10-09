import { sendChatMessage } from '@/lib/chatClient';
import { createChatHandler } from '@/lib/chatHandler';
import { loadConfig } from '@/lib/config';
import { RateLimiter } from '@/lib/rateLimiter';

// Se crean una vez por proceso: el limitador necesita conservar su estado entre peticiones.
const config = loadConfig();
const rateLimiter = new RateLimiter(config.rateLimitMaxRequests, config.rateLimitWindowSeconds * 1000);

export const POST = createChatHandler({
    config,
    rateLimiter,
    sendMessage: (message, clientIp) => sendChatMessage(message, clientIp, config),
});