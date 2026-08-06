import { Outlet } from 'react-router-dom'

function AnalysisLayout() {
  return (
    <div className="min-h-screen bg-surface lg:flex lg:items-start lg:justify-center lg:bg-bg lg:py-10">
      <div className="mx-auto w-full max-w-[560px] pb-28 lg:rounded-3xl lg:border lg:border-border lg:bg-surface lg:px-8 lg:py-8 lg:pb-8 lg:shadow-[0_20px_44px_-14px_rgba(16,27,51,.12)]">
        <Outlet />
      </div>
    </div>
  )
}

export default AnalysisLayout
