import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { useAnalysis } from '../../context/useAnalysis'
import { useAuth } from '../../context/useAuth'
import { ApiError } from '../../lib/api/client'
import {
  categoryLabel,
  paymentMethodLabel,
  uploadStatementRequest,
  type StatementIngestionResult,
} from '../../lib/api/transactions'

function UploadStatementPage() {
  const { user } = useAuth()
  const { generateForTransactions } = useAnalysis()
  const navigate = useNavigate()
  const [file, setFile] = useState<File | null>(null)
  const [defaultYear, setDefaultYear] = useState('')
  const [country, setCountry] = useState('CL')
  const [isUploading, setIsUploading] = useState(false)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [analysisError, setAnalysisError] = useState<string | null>(null)
  const [result, setResult] = useState<StatementIngestionResult | null>(null)
  const [analysisReady, setAnalysisReady] = useState(false)

  if (!user) return null

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!file || !user) return
    setIsUploading(true)
    setError(null)
    try {
      const res = await uploadStatementRequest(file, user.id, {
        defaultYear: defaultYear ? Number(defaultYear) : undefined,
        country: country || undefined,
      })
      setResult(res)
      setIsUploading(false)

      if (res.createdTransactions.length > 0) {
        setIsAnalyzing(true)
        try {
          await generateForTransactions(res.createdTransactions)
          setAnalysisReady(true)
        } catch (err) {
          setAnalysisError(
            err instanceof ApiError ? err.message : 'La cartola se cargó, pero no pudimos generar tu análisis.',
          )
        } finally {
          setIsAnalyzing(false)
        }
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo procesar la cartola.')
      setIsUploading(false)
    }
  }

  return (
    <div className="mx-auto w-full max-w-[560px] px-6 pt-6 pb-28 lg:max-w-[880px] lg:pt-8 lg:pb-16">
      <PageHeader title="Subir cartola bancaria" />

      <p className="mt-4 text-[15px] text-ink-soft">
        Subí el archivo de tu banco (Excel, CSV o PDF) y lo procesamos automáticamente: clasificamos cada
        movimiento y lo cargamos como transacción.
      </p>

      {!result && (
        <form onSubmit={handleSubmit} className="mt-6 lg:mx-auto lg:max-w-[560px]">
          {error && (
            <p className="mb-4 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{error}</p>
          )}

          <div className="mb-3">
            <label htmlFor="file" className="mb-1 block text-[13px] font-semibold text-ink-soft">
              Archivo de la cartola
            </label>
            <input
              id="file"
              type="file"
              accept=".csv,.xlsx,.xls,.pdf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              className="w-full rounded-xl border-[1.5px] border-border bg-surface px-4 py-3 text-[14px] text-ink outline-none transition file:mr-3 file:rounded-full file:border-0 file:bg-surface-alt file:px-3 file:py-1.5 file:text-[13px] file:font-semibold focus:border-navy"
            />
          </div>

          <div className="mb-3 flex gap-3">
            <div className="flex-1">
              <label htmlFor="defaultYear" className="mb-1 block text-[13px] font-semibold text-ink-soft">
                Año por defecto (opcional)
              </label>
              <input
                id="defaultYear"
                type="number"
                inputMode="numeric"
                placeholder="Ej: 2026"
                value={defaultYear}
                onChange={(e) => setDefaultYear(e.target.value)}
                className="w-full rounded-xl border-[1.5px] border-border px-4 py-3 text-[15px] text-ink outline-none transition focus:border-navy"
              />
            </div>
            <div className="flex-1">
              <label htmlFor="country" className="mb-1 block text-[13px] font-semibold text-ink-soft">
                País (opcional)
              </label>
              <input
                id="country"
                type="text"
                placeholder="Ej: CL"
                value={country}
                onChange={(e) => setCountry(e.target.value)}
                className="w-full rounded-xl border-[1.5px] border-border px-4 py-3 text-[15px] text-ink outline-none transition focus:border-navy"
              />
            </div>
          </div>

          <Button type="submit" isLoading={isUploading} disabled={!file}>
            Subir y procesar
          </Button>
        </form>
      )}

      {result && (
        <div className="mt-8">
          <div className="lg:grid lg:grid-cols-[300px_1fr] lg:items-start lg:gap-0">
            <div className="lg:pr-10">
              <p className="text-xs font-bold tracking-wide text-ink-faint uppercase">Resumen</p>
              <div className="mt-3">
                <SummaryRow label="Archivo" value={result.fileName} />
                <SummaryRow label="País / Año" value={`${result.country} · ${result.year}`} border />
                <SummaryRow label="Filas leídas" value={`${result.rawRowsCount}`} border />
                <SummaryRow label="Transacciones válidas" value={`${result.validRowsCount}`} border />
                <SummaryRow label="Descartadas" value={`${result.discardedRowsCount}`} border />
              </div>

              {result.warnings.length > 0 && (
                <div className="mt-5 rounded-2xl border border-warning/25 bg-warning-soft p-4">
                  <p className="text-xs font-bold tracking-wide text-[#7A4B0C] uppercase">Avisos</p>
                  {result.warnings.map((w, i) => (
                    <p key={i} className="mt-1.5 text-[13px] text-[#7A4B0C]">
                      {w}
                    </p>
                  ))}
                </div>
              )}
            </div>

            {result.createdTransactions.length > 0 && (
              <div className="mt-8 border-t border-border pt-6 lg:mt-0 lg:border-t-0 lg:border-l lg:border-border lg:pt-0 lg:pl-10">
                <p className="text-xs font-bold tracking-wide text-ink-faint uppercase">Transacciones creadas</p>
                <div className="mt-3 rounded-2xl border border-border">
                  {result.createdTransactions.map((t, i) => (
                    <div
                      key={t.id}
                      className={`flex items-center justify-between gap-3 px-5 py-3 ${i > 0 ? 'border-t border-border' : ''}`}
                    >
                      <div className="min-w-0">
                        <p className="truncate text-[13.5px] font-medium text-ink">
                          {t.description || 'Sin descripción'}
                        </p>
                        <p className="mt-0.5 text-[11px] text-ink-faint">
                          {categoryLabel(t.category)} · {paymentMethodLabel(t.paymentMethod)} ·{' '}
                          {new Date(t.date).toLocaleDateString('es-AR', { day: 'numeric', month: 'short' })}
                        </p>
                      </div>
                      <span className="shrink-0 font-mono text-sm font-semibold text-ink">
                        ${t.amount.toLocaleString('es-AR')}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {isAnalyzing && (
            <p className="mt-6 text-sm text-ink-soft">Generando tu análisis y recomendaciones…</p>
          )}
          {analysisError && (
            <p className="mt-6 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{analysisError}</p>
          )}

          <div className="mt-8 flex gap-3">
            <Button variant="ghost" fullWidth={false} onClick={() => navigate('/dashboard')}>
              Volver al panel
            </Button>
            <Button fullWidth={false} onClick={() => navigate('/transactions')}>
              Ver mis transacciones
            </Button>
            {analysisReady && (
              <Button fullWidth={false} onClick={() => navigate('/analysis/results')}>
                Ver mi análisis
              </Button>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function SummaryRow({ label, value, border = false }: { label: string; value: string; border?: boolean }) {
  return (
    <div className={`flex items-center justify-between py-2.5 ${border ? 'border-t border-border' : ''}`}>
      <span className="text-[13px] text-ink-soft">{label}</span>
      <span className="font-mono text-sm font-semibold text-ink">{value}</span>
    </div>
  )
}

export default UploadStatementPage
