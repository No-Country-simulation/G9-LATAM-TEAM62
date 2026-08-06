import { Link } from 'react-router-dom'
import { Reveal } from '../ui/Reveal'

export function FinalCtaSection() {
  return (
    <section className="bg-navy">
      <div className="mx-auto max-w-[1160px] px-6 py-16 text-center md:py-24">
        <Reveal className="mx-auto flex max-w-[46ch] flex-col items-center">
          <h2 className="font-display text-3xl font-bold tracking-tight text-white md:text-4xl">
            Tu puntaje te espera. Sacalo en minutos.
          </h2>
          <p className="mt-4 text-base text-white/65 md:text-lg">
            Solo tus números y una lectura clara de dónde estás parado.
          </p>
          <Link
            to="/register"
            className="mt-8 rounded-full bg-accent px-8 py-4 font-semibold text-[#04231C] transition hover:shadow-lg"
          >
            Comenzar análisis
          </Link>
        </Reveal>
      </div>
    </section>
  )
}
