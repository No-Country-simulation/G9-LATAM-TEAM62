const SCORE = 74

function App() {
  const size = 140
  const stroke = 11
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference * (1 - SCORE / 100)

  return (
    <div className="min-h-screen bg-[#F4F6F9] text-[#0E1526]">
      <div className="mx-auto flex max-w-[1160px] flex-col px-6 pt-8 md:pt-14">
        <nav className="flex items-center justify-between pb-8">
          <div className="flex items-center gap-2 font-semibold">
            <span className="h-2.5 w-2.5 rounded-full bg-[#16B892]" />
            Finance AI
          </div>
        </nav>

        <div className="grid flex-1 items-center gap-14 pb-16 md:grid-cols-[1.05fr_0.95fr]">
          <div>
            <span className="mb-4 inline-flex rounded-full bg-[#E1F7F0] px-3 py-1.5 text-xs font-semibold tracking-wide text-[#0A3F33] uppercase">
              Salud financiera con IA
            </span>
            <h1 className="text-4xl leading-tight font-bold tracking-tight md:text-6xl">
              El estado real de tus finanzas,{' '}
              <em className="not-italic bg-[linear-gradient(180deg,transparent_60%,#E1F7F0_60%)]">
                en un solo número.
              </em>
            </h1>
            <p className="mt-5 max-w-[44ch] text-base text-[#5B6472] md:text-lg">
              Contanos tus ingresos y tus gastos. Finance AI analiza tu
              situación y te dice, en criollo, qué ajustar para mejorar.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <button className="rounded-full bg-[#16B892] px-6 py-4 font-semibold text-[#04231C] transition hover:shadow-lg">
                Comenzar análisis
              </button>
              <button className="rounded-full border border-[#E3E7ED] px-6 py-4 font-semibold text-[#101B33] transition hover:border-[#101B33]">
                Iniciar sesión
              </button>
            </div>
          </div>

          <div className="flex items-center justify-center">
            <div className="w-full max-w-[300px] rounded-[26px] bg-[#101B33] p-9 text-center shadow-[0_20px_44px_-14px_rgba(16,27,51,.22)]">
              <div
                className="relative mx-auto"
                style={{ width: size, height: size }}
              >
                <svg
                  width={size}
                  height={size}
                  viewBox={`0 0 ${size} ${size}`}
                  className="-rotate-90"
                >
                  <circle
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    fill="none"
                    stroke="rgba(255,255,255,.14)"
                    strokeWidth={stroke}
                  />
                  <circle
                    cx={size / 2}
                    cy={size / 2}
                    r={radius}
                    fill="none"
                    stroke="#16B892"
                    strokeWidth={stroke}
                    strokeLinecap="round"
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                  />
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                  <span className="font-mono text-4xl font-semibold text-white">
                    {SCORE}
                  </span>
                  <span className="mt-1 text-[11px] font-semibold tracking-wide text-white/60 uppercase">
                    Puntaje de salud
                  </span>
                </div>
              </div>
              <p className="mt-4 text-xs leading-relaxed text-white/55">
                Puntaje calculado a partir de tus ingresos, deudas y hábitos de
                ahorro.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default App
