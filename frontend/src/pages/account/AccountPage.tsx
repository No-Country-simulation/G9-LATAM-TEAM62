import { useEffect, useState } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Modal } from '../../components/ui/Modal'
import { PasswordField } from '../../components/ui/PasswordField'
import { TextField } from '../../components/ui/TextField'
import { useAuth } from '../../context/useAuth'
import { useToast } from '../../context/useToast'
import { ApiError } from '../../lib/api/client'
import {
  getUserRequest,
  updateUserRequest,
  changePasswordRequest,
  deleteUserRequest,
  getProfileHistoryRequest,
  type ProfileHistoryEntry,
} from '../../lib/api/users'
import { financialProfileLabel, type ApiUser } from '../../lib/api/auth'
import {
  editProfileSchema,
  changePasswordSchema,
  savingFrequencyOptions,
  type EditProfileFormInput,
  type EditProfileFormValues,
  type ChangePasswordFormValues,
} from '../../lib/validation/accountSchemas'

const accountTabs = [
  { key: 'profile', label: 'Mi perfil' },
  { key: 'security', label: 'Seguridad' },
  { key: 'delete', label: 'Eliminar cuenta' },
] as const
type AccountTabKey = (typeof accountTabs)[number]['key']

function AccountPage() {
  const { user: authUser, updateUser } = useAuth()
  const [profile, setProfile] = useState<ApiUser | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isEditing, setIsEditing] = useState(false)
  const [profileHistory, setProfileHistory] = useState<ProfileHistoryEntry[] | null>(null)
  const [profileHistoryError, setProfileHistoryError] = useState<string | null>(null)
  const [activeTab, setActiveTab] = useState<AccountTabKey>('profile')
  const activeTabIndex = accountTabs.findIndex((t) => t.key === activeTab)

  // Depende del id, no del objeto authUser completo: updateUser(fresh) más abajo cambia la
  // referencia de authUser en cada respuesta (aunque los datos sean iguales), y si el efecto
  // dependiera de authUser entero eso lo volvería a disparar sin fin — un loop de requests que
  // agota el rate limit del backend en segundos.
  const authUserId = authUser?.id

  useEffect(() => {
    if (!authUserId) return
    getUserRequest(authUserId)
      .then((fresh) => {
        setProfile(fresh)
        updateUser(fresh)
      })
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : 'No se pudo cargar tu perfil.'))

    getProfileHistoryRequest(authUserId)
      .then((list) => setProfileHistory([...list].sort((a, b) => b.createdAt.localeCompare(a.createdAt))))
      .catch((err) =>
        setProfileHistoryError(err instanceof ApiError ? err.message : 'No se pudo cargar el historial de perfil.'),
      )
  }, [authUserId])

  if (!authUser) return null

  return (
    <div className="mx-auto w-full max-w-[560px] px-6 pt-6 pb-6 lg:max-w-[860px]">
      <PageHeader title="Mi perfil" />
      <p className="mt-2 text-[15px] text-ink-soft lg:text-base">Tus datos personales y preferencias de la cuenta.</p>

      {loadError && (
        <p className="mt-3 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{loadError}</p>
      )}

      <div className="relative mx-auto mt-5 flex rounded-full bg-surface-alt p-1 lg:max-w-[460px]">
        <div
          className="absolute top-1 bottom-1 left-1 rounded-full bg-surface shadow-sm transition-transform duration-300 ease-out"
          style={{ width: `calc((100% - 0.5rem) / ${accountTabs.length})`, transform: `translateX(${activeTabIndex * 100}%)` }}
          aria-hidden
        />
        {accountTabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            className={`relative z-10 flex-1 rounded-full py-2.5 text-center text-[13px] font-semibold transition-colors duration-300 ${
              activeTab === tab.key ? 'text-ink' : 'text-ink-soft hover:text-ink'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {activeTab === 'profile' && (
          <div className="lg:grid lg:grid-cols-2 lg:items-start lg:gap-6">
            {profile && <ProfileSummary profile={profile} onEdit={() => setIsEditing(true)} />}
            <ProfileHistorySection history={profileHistory} error={profileHistoryError} />
          </div>
        )}

        {activeTab === 'security' && (
          <div className="lg:mx-auto lg:max-w-[460px]">
            <h2 className="font-display text-lg font-bold text-ink">Cambiar contraseña</h2>
            <div className="mt-4 rounded-2xl border border-border p-5">
              <ChangePasswordForm />
            </div>
          </div>
        )}

        {activeTab === 'delete' && (
          <div className="lg:mx-auto lg:max-w-[460px]">
            <h2 className="font-display text-lg font-bold text-risk">Eliminar cuenta</h2>
            <p className="mt-1 text-[13px] text-ink-soft">
              Esta acción borra tu usuario, tus transacciones y tus recomendaciones de forma permanente.
            </p>
            <div className="mt-4">
              <DeleteAccountSection />
            </div>
          </div>
        )}
      </div>

      <Modal isOpen={isEditing} onClose={() => setIsEditing(false)} title="Editar datos">
        {profile && (
          <EditProfileForm
            profile={profile}
            onCancel={() => setIsEditing(false)}
            onSaved={(updated) => {
              setProfile(updated)
              updateUser(updated)
              setIsEditing(false)
            }}
          />
        )}
      </Modal>
    </div>
  )
}

function frequencyLabel(value: ApiUser['savingFrequency']) {
  return savingFrequencyOptions.find((o) => o.value === value)?.label ?? 'Sin definir'
}

function ProfileSummary({ profile, onEdit }: { profile: ApiUser; onEdit: () => void }) {
  return (
    <div className="rounded-2xl border border-border p-4">
      <SummaryRow label="Nombre" value={profile.name} />
      <SummaryRow label="Correo" value={profile.email} border />
      <SummaryRow
        label="Ingreso mensual"
        value={profile.monthlyIncome != null ? `$${profile.monthlyIncome.toLocaleString('es-AR')}` : 'Sin definir'}
        border
      />
      <SummaryRow label="Frecuencia de ahorro" value={frequencyLabel(profile.savingFrequency)} border />
      <SummaryRow label="Perfil financiero" value={financialProfileLabel(profile.financialProfile)} border />
      <SummaryRow
        label="Confianza del perfil"
        value={profile.profileAccuracy != null ? `${Math.round(profile.profileAccuracy * 100)}%` : 'Sin calcular todavía'}
        border
      />
      <div className="mt-3">
        <Button variant="ghost" fullWidth={false} onClick={onEdit}>
          Editar datos
        </Button>
      </div>
    </div>
  )
}

function ProfileHistorySection({
  history,
  error,
}: {
  history: ProfileHistoryEntry[] | null
  error: string | null
}) {
  return (
    <div className="mt-4 rounded-2xl border border-border p-4 lg:mt-0">
      <h2 className="font-display text-lg font-bold text-ink">Historial de perfil financiero</h2>

      {error && <p className="mt-2 text-sm font-medium text-risk">{error}</p>}

      {history == null && !error && <p className="mt-2 text-sm text-ink-faint">Cargando…</p>}

      {history && history.length === 0 && (
        <p className="mt-2 text-[13px] leading-relaxed text-ink-soft">
          Todavía no hay historial: se va a completar a medida que se calcule tu perfil financiero.
        </p>
      )}

      {history && history.length > 0 && (
        <div className="mt-2">
          {history.map((entry, i) => (
            <div key={entry.id} className={`flex items-center justify-between py-2.5 ${i > 0 ? 'border-t border-border' : ''}`}>
              <div>
                <p className="text-sm font-semibold text-ink">{financialProfileLabel(entry.financialProfile)}</p>
                <p className="text-[11px] text-ink-faint">
                  {new Date(entry.createdAt).toLocaleDateString('es-AR', { day: 'numeric', month: 'short', year: 'numeric' })}
                </p>
              </div>
              <span className="font-mono text-sm text-ink-soft">{Math.round(entry.profileAccuracy * 100)}%</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function SummaryRow({ label, value, border = false }: { label: string; value: string; border?: boolean }) {
  return (
    <div className={`flex items-center justify-between py-2 ${border ? 'border-t border-border' : ''}`}>
      <span className="text-[13px] text-ink-soft lg:text-sm">{label}</span>
      <span className="text-sm font-semibold text-ink lg:text-base">{value}</span>
    </div>
  )
}

function EditProfileForm({
  profile,
  onCancel,
  onSaved,
}: {
  profile: ApiUser
  onCancel: () => void
  onSaved: (user: ApiUser) => void
}) {
  const { user: authUser } = useAuth()
  const { showToast } = useToast()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<EditProfileFormInput, unknown, EditProfileFormValues>({
    resolver: zodResolver(editProfileSchema),
    defaultValues: {
      name: profile.name,
      email: profile.email,
      monthlyIncome: profile.monthlyIncome ?? 0,
      savingFrequency: profile.savingFrequency ?? 'NEVER',
      currentPassword: '',
    },
  })

  async function onSubmit(values: EditProfileFormValues) {
    if (!authUser) return
    setFormError(null)
    try {
      const updated = await updateUserRequest(authUser.id, {
        name: values.name,
        email: values.email,
        password: values.currentPassword,
        monthlyIncome: values.monthlyIncome,
        savingFrequency: values.savingFrequency,
      })
      onSaved(updated)
      showToast('Datos actualizados')
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudieron guardar los cambios.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      {formError && (
        <p className="mb-4 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{formError}</p>
      )}
      <TextField label="Nombre completo" error={errors.name?.message} {...register('name')} />
      <TextField label="Correo electrónico" type="email" error={errors.email?.message} {...register('email')} />
      <TextField
        label="Ingreso mensual (neto)"
        type="number"
        inputMode="decimal"
        error={errors.monthlyIncome?.message}
        {...register('monthlyIncome')}
      />
      <div className="mb-3">
        <label htmlFor="savingFrequency" className="mb-1 block text-[13px] font-semibold text-ink-soft">
          Frecuencia de ahorro
        </label>
        <select
          id="savingFrequency"
          className="w-full rounded-xl border-[1.5px] border-border px-4 py-3 text-[15px] text-ink outline-none transition focus:border-navy"
          {...register('savingFrequency')}
        >
          {savingFrequencyOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      <PasswordField
        label="Contraseña actual"
        error={errors.currentPassword?.message}
        {...register('currentPassword')}
      />
      <div className="flex gap-3">
        <Button type="button" variant="ghost" onClick={onCancel}>
          Cancelar
        </Button>
        <Button type="submit" isLoading={isSubmitting}>
          Guardar
        </Button>
      </div>
    </form>
  )
}

function ChangePasswordForm() {
  const { showToast } = useToast()
  const [formError, setFormError] = useState<string | null>(null)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordFormValues>({ resolver: zodResolver(changePasswordSchema) })

  async function onSubmit(values: ChangePasswordFormValues) {
    setFormError(null)
    try {
      const response = await changePasswordRequest(values.oldPassword, values.newPassword)
      showToast(response.message)
      reset()
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'No se pudo cambiar la contraseña.')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      {formError && (
        <p className="mb-4 rounded-lg bg-risk/10 px-3 py-2 text-sm font-medium text-risk">{formError}</p>
      )}
      <PasswordField label="Contraseña actual" error={errors.oldPassword?.message} {...register('oldPassword')} />
      <PasswordField
        label="Nueva contraseña"
        placeholder="Mínimo 8 caracteres"
        error={errors.newPassword?.message}
        {...register('newPassword')}
      />
      <PasswordField
        label="Confirmar nueva contraseña"
        error={errors.confirmPassword?.message}
        {...register('confirmPassword')}
      />
      <Button type="submit" fullWidth={false} isLoading={isSubmitting}>
        Cambiar contraseña
      </Button>
    </form>
  )
}

function DeleteAccountSection() {
  const { user: authUser, logout } = useAuth()
  const [confirming, setConfirming] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete() {
    if (!authUser) return
    setIsDeleting(true)
    setError(null)
    try {
      await deleteUserRequest(authUser.id)
      logout()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo eliminar la cuenta.')
      setIsDeleting(false)
    }
  }

  return (
    <>
      <Button variant="ghost" fullWidth={false} onClick={() => setConfirming(true)}>
        Eliminar cuenta
      </Button>

      <Modal isOpen={confirming} onClose={() => setConfirming(false)} title="Eliminar cuenta">
        {error && <p className="mb-3 text-sm font-medium text-risk">{error}</p>}
        <p className="text-sm text-ink-soft">¿Confirmás que querés eliminar tu cuenta? No se puede deshacer.</p>
        <div className="mt-5 flex justify-end gap-3">
          <Button variant="ghost" fullWidth={false} onClick={() => setConfirming(false)} disabled={isDeleting}>
            Cancelar
          </Button>
          <Button
            fullWidth={false}
            isLoading={isDeleting}
            onClick={handleDelete}
            className="bg-risk text-white hover:shadow-[0_10px_24px_-8px_rgba(225,72,77,.5)]"
          >
            Sí, eliminar mi cuenta
          </Button>
        </div>
      </Modal>
    </>
  )
}

export default AccountPage
