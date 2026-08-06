import { FinalCtaSection } from '../components/landing/FinalCtaSection'
import { Footer } from '../components/landing/Footer'
import { HeroSection } from '../components/landing/HeroSection'
import { HowItWorksSection } from '../components/landing/HowItWorksSection'
import { ScoreRangesSection } from '../components/landing/ScoreRangesSection'

function LandingPage() {
  return (
    <div className="min-h-screen bg-bg text-ink">
      <HeroSection />
      <HowItWorksSection />
      <ScoreRangesSection />
      <FinalCtaSection />
      <Footer />
    </div>
  )
}

export default LandingPage
