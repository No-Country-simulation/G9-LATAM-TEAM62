import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { StepHeader } from '../../components/analysis/StepHeader'
import { Button } from '../../components/ui/Button'
import { Chip } from '../../components/ui/Chip'
import { TextField } from '../../components/ui/TextField'
import { debtLevels, savingsFrequencies, type DebtLevel, type SavingsFrequency } from '../../context/analysis-context'
import { useAnalysis } from '../../context/useAnalysis'
import { profileSchema, type ProfileFormInput, type ProfileFormValues } from '../../lib/validation/analysisSchemas'

function ProfilePage() {
  const { profile, setProfile } = useAnalysis()
  const navigate = useNavigate()

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isValid },
  } = useForm<ProfileFormInput, unknown, ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    mode: 'onChange',
    defaultValues: {
      income: profile.income ?? undefined,
      debt: profile.debt ?? '',
      savings: profile.savings ?? '',
    },
  })

  const selectedDebt = watch('debt')

  function onSubmit(values: ProfileFormValues) {
    setProfile({
      income: values.income,
      debt: values.debt as DebtLevel,
      savings: values.savings as SavingsFrequency,
    })
    navigate('/analysis/transactions')
  }

  return (
    <>
      <StepHeader title="Nuevo análisis" onBack={() => navigate('/dashboard')} step={1} totalSteps={2} />

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="px-6 lg:px-0">
        <h2 className="mt-6 font-display text-2xl font-bold tracking-tight text-ink">Contanos lo básico</h2>
        <p className="mt-2 text-[15px] text-ink-soft">
          Esto le da a la IA una base antes de mirar tus transacciones.
        </p>

        <div className="mt-6">
          <TextField
            label="Ingreso mensual (neto)"
            type="number"
            inputMode="decimal"
            placeholder="Ej: 350000"
            error={errors.income?.message}
            {...register('income')}
          />

          <div className="mb-4">
            <label className="mb-1.5 block text-[13px] font-semibold text-ink-soft">Nivel de deuda actual</label>
            <div className="flex flex-wrap gap-2">
              {debtLevels.map((level) => (
                <Chip
                  key={level}
                  selected={selectedDebt === level}
                  onClick={() => setValue('debt', level, { shouldValidate: true })}
                >
                  {level}
                </Chip>
              ))}
            </div>
            {errors.debt && <p className="mt-1.5 text-xs font-medium text-risk">{errors.debt.message}</p>}
          </div>

          <div className="mb-3">
            <label htmlFor="savings" className="mb-1 block text-[13px] font-semibold text-ink-soft">
              ¿Con qué frecuencia ahorrás?
            </label>
            <select
              id="savings"
              className={`w-full rounded-xl border-[1.5px] px-4 py-3 text-[15px] text-ink outline-none transition focus:border-navy ${
                errors.savings ? 'border-risk' : 'border-border'
              }`}
              {...register('savings')}
            >
              <option value="">Elegí una frecuencia</option>
              {savingsFrequencies.map((freq) => (
                <option key={freq} value={freq}>
                  {freq}
                </option>
              ))}
            </select>
            {errors.savings && <p className="mt-1.5 text-xs font-medium text-risk">{errors.savings.message}</p>}
          </div>
        </div>

        <div className="mt-8 max-lg:fixed max-lg:inset-x-0 max-lg:bottom-0 max-lg:z-10 max-lg:mt-0 max-lg:border-t max-lg:border-border max-lg:bg-surface max-lg:px-6 max-lg:py-4">
          <Button type="submit" disabled={!isValid}>
            Continuar
          </Button>
        </div>
      </form>
    </>
  )
}

export default ProfilePage
