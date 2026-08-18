import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, SESSION_KEY, api, getToken, setToken } from './client'

function mockFetch(status: number, body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  })
}

function setLocation(pathname: string) {
  Object.defineProperty(window, 'location', {
    value: { pathname, href: '' },
    writable: true,
    configurable: true,
  })
}

describe('api client', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setLocation('/dashboard')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('adjunta el Authorization header cuando hay token guardado', async () => {
    setToken('abc123')
    const fetchMock = mockFetch(200, { ok: true })
    vi.stubGlobal('fetch', fetchMock)

    await api.get('/api/whatever')

    const [, options] = fetchMock.mock.calls[0]
    expect(options.headers.Authorization).toBe('Bearer abc123')
  })

  it('no adjunta Authorization cuando no hay token', async () => {
    const fetchMock = mockFetch(200, { ok: true })
    vi.stubGlobal('fetch', fetchMock)

    await api.post('/api/auth/login', { email: 'a@a.com', password: 'x' })

    const [, options] = fetchMock.mock.calls[0]
    expect(options.headers.Authorization).toBeUndefined()
  })

  it('no fija Content-Type cuando el body es FormData (multipart)', async () => {
    const fetchMock = mockFetch(200, {})
    vi.stubGlobal('fetch', fetchMock)

    await api.postForm('/api/transactions/upload-statement', new FormData())

    const [, options] = fetchMock.mock.calls[0]
    expect(options.headers['Content-Type']).toBeUndefined()
  })

  it('lanza ApiError con el mensaje del backend cuando la respuesta falla', async () => {
    const fetchMock = mockFetch(400, { error: 'Datos inválidos' })
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.get('/api/x')).rejects.toMatchObject({ message: 'Datos inválidos', status: 400 })
  })

  it('en un 401 con token adjunto, limpia la sesión y redirige a /login', async () => {
    setToken('expired-token')
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({ id: 1 }))
    const fetchMock = mockFetch(401, { error: 'Unauthorized' })
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.get('/api/transactions')).rejects.toBeInstanceOf(ApiError)

    expect(getToken()).toBeNull()
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
    expect(window.location.href).toBe('/login')
  })

  it('en un 401 sin token (credenciales de login inválidas) no fuerza logout/redirect', async () => {
    const fetchMock = mockFetch(401, { error: 'Credenciales inválidas' })
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      api.post('/api/auth/login', { email: 'a@a.com', password: 'bad' }),
    ).rejects.toMatchObject({ message: 'Credenciales inválidas' })

    expect(window.location.href).toBe('')
  })

  it('en un 403 con token adjunto (JWT vencido/inválido), limpia la sesión y redirige a /login', async () => {
    setToken('expired-token')
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({ id: 1 }))
    const fetchMock = mockFetch(403, { error: 'Forbidden' })
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.get('/api/transactions')).rejects.toBeInstanceOf(ApiError)

    expect(getToken()).toBeNull()
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull()
    expect(window.location.href).toBe('/login')
  })

  it('no vuelve a redirigir si ya está en /login', async () => {
    setToken('expired-token')
    setLocation('/login')
    const fetchMock = mockFetch(401, { error: 'Unauthorized' })
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.get('/api/users/1')).rejects.toBeInstanceOf(ApiError)

    expect(window.location.href).toBe('')
  })
})
