import React from "react";

interface MobileFrameProps {
  children: React.ReactNode;
  activeTrackerContent?: string;
  isTrackerOpen: boolean;
  setIsTrackerOpen: (open: boolean) => void;
}

export default function MobileFrame({
  children,
  activeTrackerContent = "",
  isTrackerOpen,
  setIsTrackerOpen,
}: MobileFrameProps) {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center font-sans antialiased selection:bg-purple-500/30 selection:text-purple-200 p-0 md:p-6 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-slate-900 via-slate-950 to-black">
      
      {/* 
        Container Utama (Desktop vs Mobile):
        - Desktop: Tampilan dual-panel (Phone Frame + Tracker Panel) berdampingan
        - Mobile: Fullscreen Phone Frame saja (dengan sliding drawer)
      */}
      <div className="flex flex-col md:flex-row items-center justify-center gap-6 max-w-6xl w-full">
        
        {/* Mockup Bingkai Ponsel */}
        <div className="w-full h-screen md:h-[860px] md:max-w-[420px] md:rounded-[44px] md:border-8 md:border-slate-800 md:shadow-[0_0_80px_rgba(0,0,0,0.8),_0_20px_40px_rgba(168,85,247,0.1)] relative flex flex-col overflow-hidden bg-slate-900 md:ring-1 md:ring-slate-700/50 transition-all duration-500">
          
          {/* Punch Hole Kamera Ponsel (Desktop Only) */}
          <div className="hidden md:flex absolute top-3 left-1/2 -translate-x-1/2 w-28 h-5 bg-slate-800 rounded-full z-50 items-center justify-center">
            <div className="w-2.5 h-2.5 bg-slate-900 rounded-full mr-2 border border-slate-700"></div>
            <div className="w-10 h-1 bg-slate-900 rounded-full"></div>
          </div>

          {/* Area Konten */}
          <div className="flex-1 flex flex-col h-full overflow-hidden pt-0 md:pt-6">
            {children}
          </div>
        </div>

        {/* Panel Tracker Samping (Desktop Only) */}
        <div className="hidden lg:flex flex-col w-[380px] h-[860px] bg-slate-900/80 border border-slate-800 rounded-[32px] p-6 shadow-2xl backdrop-blur-md relative overflow-hidden transition-all duration-300">
          <div className="absolute top-0 left-0 right-0 h-[2px] bg-gradient-to-r from-purple-500 to-pink-500"></div>
          
          <div className="flex items-center space-x-2 mb-4">
            <span className="text-xl">📖</span>
            <h2 className="font-bold text-lg text-slate-100 tracking-wide">Story State Tracker</h2>
          </div>
          
          <div className="flex-1 overflow-y-auto pr-1 text-sm leading-relaxed text-slate-300 whitespace-pre-wrap font-mono bg-slate-950/50 rounded-2xl p-4 border border-slate-800/60 scrollbar-thin">
            {activeTrackerContent || "Belum ada status cerita aktif. Mulai percakapan untuk memperbarui tracker."}
          </div>
          
          <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between text-xs text-slate-500">
            <span>Dual-Component UX Active</span>
            <span className="animate-pulse text-green-500">● Sync</span>
          </div>
        </div>
      </div>

      {/* Drawer Tracker untuk Layar Mobile / Tablet Kecil */}
      {isTrackerOpen && (
        <div className="lg:hidden fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-sm animate-fadeIn">
          <div className="w-[85%] max-w-sm h-full bg-slate-900 border-l border-slate-800 p-6 flex flex-col shadow-2xl relative">
            <button
              onClick={() => setIsTrackerOpen(false)}
              className="absolute top-4 right-4 w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 flex items-center justify-center text-slate-300 hover:text-white transition-all"
            >
              ✕
            </button>
            
            <div className="flex items-center space-x-2 mb-6 mt-2">
              <span className="text-xl">📖</span>
              <h2 className="font-bold text-lg text-slate-100 tracking-wide">Story State Tracker</h2>
            </div>
            
            <div className="flex-1 overflow-y-auto text-sm leading-relaxed text-slate-300 whitespace-pre-wrap font-mono bg-slate-950/50 rounded-2xl p-4 border border-slate-800/60">
              {activeTrackerContent || "Belum ada status cerita aktif."}
            </div>
            
            <div className="mt-6 text-xs text-slate-500 text-center">
              DreamPlay Roleplay Pro Engine 3.0
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
