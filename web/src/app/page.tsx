import dynamic from "next/dynamic";
import { Suspense } from "react";

const MainApp = dynamic(() => import("../components/MainApp"), { ssr: false });

export default function Page() {
  return (
    <Suspense fallback={
      <div className="flex h-screen items-center justify-center bg-slate-950 text-white font-sans">
        <div className="text-center space-y-4">
          <div className="w-12 h-12 border-4 border-purple-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
          <p className="text-xs font-bold uppercase tracking-widest text-purple-400">DreamPlay Pro 3.0 Loading...</p>
        </div>
      </div>
    }>
      <MainApp />
    </Suspense>
  );
}
