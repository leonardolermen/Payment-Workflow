export { TracerClient } from './tracer'
export { Span } from './span'

export { injectHeaders, extractContext } from './propagation'
export { tracerMiddleware } from './integrations/express'
export { fastifyTracer } from './integrations/fastify'
export { TracerInterceptor } from './integrations/nestjs'
export { patchFetch } from './integrations/fetch'
export type {
  TracerConfig,
  SpanOptions,
  SpanKind,
  SpanStatus,
  SpanEvent,
  SpanError,
  Transport,
} from './types'

import { TracerClient } from './tracer'
import { TracerConfig } from './types'

let _instance: TracerClient | null = null

export const Tracer = {
  init(config: TracerConfig): TracerClient {
    _instance = new TracerClient(config)
    return _instance
  },

  get instance(): TracerClient {
    if (!_instance) throw new Error('[tracer] Call Tracer.init() before using the SDK')
    return _instance
  },
}
