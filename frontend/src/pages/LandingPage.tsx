import { FinalCtaSection } from '../components/landing/FinalCtaSection'
import { FinancialProfilesSection } from '../components/landing/FinancialProfilesSection'
import { Footer } from '../components/landing/Footer'
import { HeroSection } from '../components/landing/HeroSection'
import { HowItWorksSection } from '../components/landing/HowItWorksSection'

function LandingPage() {
  return (
    <div className="min-h-screen bg-bg text-ink">
      <HeroSection />
      <HowItWorksSection />
      <FinancialProfilesSection />
      <FinalCtaSection />
      <Footer />
    </div>
  )
}

export default LandingPage
