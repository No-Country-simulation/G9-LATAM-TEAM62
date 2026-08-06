import { useContext } from 'react'
import { AnalysisContext } from './analysis-context'

export function useAnalysis() {
  const ctx = useContext(AnalysisContext)
  if (!ctx) throw new Error('useAnalysis debe usarse dentro de <AnalysisProvider>')
  return ctx
}
