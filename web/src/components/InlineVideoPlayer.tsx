"use client";

import React, { useState, useEffect, useRef } from "react";
import { resolveDirectVideoUrl } from "../utils/videoExtractor";

interface InlineVideoPlayerProps {
  videoUrl: string;
}

export default function InlineVideoPlayer({ videoUrl }: InlineVideoPlayerProps) {
  const [resolved, setResolved] = useState<string>("");
  const [isResolving, setIsResolving] = useState(true);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [hasError, setHasError] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    setIsResolving(true);
    setHasError(false);
    try {
      const direct = resolveDirectVideoUrl(videoUrl);
      setResolved(direct);
    } catch (err) {
      setHasError(true);
    } finally {
      setIsResolving(false);
    }
  }, [videoUrl]);

  const handlePlayPause = () => {
    if (!videoRef.current) return;
    if (isPlaying) {
      videoRef.current.pause();
      setIsPlaying(false);
    } else {
      videoRef.current.play().then(() => {
        setIsPlaying(true);
      }).catch(() => {
        setHasError(true);
      });
    }
  };

  const handleMuteToggle = () => {
    if (!videoRef.current) return;
    videoRef.current.muted = !isMuted;
    setIsMuted(!isMuted);
  };

  if (isResolving) {
    return (
      <div className="w-full h-48 bg-slate-950 border border-slate-800 rounded-2xl flex flex-col items-center justify-center space-y-2 p-4 animate-pulse">
        <div className="w-8 h-8 border-3 border-purple-500 border-t-transparent rounded-full animate-spin"></div>
        <p className="text-[10px] text-purple-400 font-bold tracking-widest uppercase">Mengekstraksi Link Video...</p>
      </div>
    );
  }

  if (hasError || !resolved) {
    return (
      <div className="w-full bg-red-950/20 border border-red-900/30 rounded-2xl p-4 flex flex-col items-center justify-center text-center space-y-2">
        <span className="text-xl">⚠️</span>
        <p className="text-[11px] font-bold text-red-300">Gagal memutar aliran video fiktif.</p>
        <a
          href={videoUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="bg-purple-600 hover:bg-purple-500 text-white font-extrabold text-[10px] px-3 py-1.5 rounded-lg border border-purple-400/20 transition-all flex items-center gap-1 mt-1 uppercase tracking-wider"
        >
          Buka di Tab Baru ↗
        </a>
      </div>
    );
  }

  return (
    <div className="w-full bg-slate-950 rounded-2xl border border-slate-800 overflow-hidden relative group/video shadow-xl">
      <video
        ref={videoRef}
        src={resolved}
        className="w-full max-h-[260px] object-contain bg-black"
        preload="metadata"
        onClick={handlePlayPause}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onError={() => setHasError(true)}
        loop
      />

      {/* Overlay custom player controls */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-black/40 opacity-0 group-hover/video:opacity-100 transition-all duration-300 flex flex-col justify-between p-3 pointer-events-none">
        
        {/* Top details */}
        <div className="flex items-center justify-between pointer-events-auto">
          <span className="text-[9px] font-bold uppercase tracking-widest text-purple-400 bg-purple-950/60 px-2 py-0.5 rounded-md border border-purple-500/20 max-w-[70%] truncate">
            {videoUrl.split("/").pop() || "Aliran Video"}
          </span>
          <a
            href={resolved}
            download
            className="text-slate-300 hover:text-white text-xs bg-slate-900/80 hover:bg-slate-800 w-6 h-6 rounded-lg flex items-center justify-center border border-slate-800 transition-all"
            title="Download Video"
          >
            📥
          </a>
        </div>

        {/* Bottom actions */}
        <div className="flex items-center justify-between pointer-events-auto mt-auto">
          <div className="flex items-center gap-2">
            <button
              onClick={handlePlayPause}
              className="bg-purple-600 hover:bg-purple-500 hover:scale-105 active:scale-95 text-white w-8 h-8 rounded-full flex items-center justify-center border border-purple-400/20 transition-all shadow-md"
            >
              {isPlaying ? "⏸" : "▶"}
            </button>
            <button
              onClick={handleMuteToggle}
              className="bg-slate-900/80 hover:bg-slate-800 text-slate-300 hover:text-white w-8 h-8 rounded-full flex items-center justify-center border border-slate-800 transition-all"
            >
              {isMuted ? "🔇" : "🔊"}
            </button>
          </div>

          <a
            href={videoUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="bg-slate-900/80 hover:bg-slate-800 text-slate-300 hover:text-white text-[10px] font-bold px-3 py-1.5 rounded-xl border border-slate-800 transition-all uppercase tracking-wider flex items-center gap-1"
          >
            Layar Penuh ↗
          </a>
        </div>
      </div>
    </div>
  );
}
