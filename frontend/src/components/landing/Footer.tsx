import { Logo } from '../ui/Logo'

export function Footer() {
  const year = new Date().getFullYear()

  return (
    <footer className="bg-navy">
      <div className="mx-auto flex max-w-[1160px] flex-col items-center gap-3 border-t border-white/10 px-6 py-8 text-center text-xs text-white/45 md:flex-row md:justify-between md:text-left">
        <div className="flex items-center gap-2 font-semibold text-white/70">
          <Logo className="h-4 w-4" transparent />
          Finance AI
        </div>
        <p>© {year} Finance AI. Tus datos financieros son privados y solo se usan para calcular tu perfil.</p>
      </div>
    </footer>
  )
}
