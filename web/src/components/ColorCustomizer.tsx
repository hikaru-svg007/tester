"use client";

import React, { useState, useRef, useEffect } from "react";
import { ColorTheme, DEFAULT_THEMES } from "../utils/theme";

interface ColorCustomizerProps {
  theme: ColorTheme;
  onChange: (updated: ColorTheme) => void;
}

export default function ColorCustomizer({ theme, onChange }: ColorCustomizerProps) {
  const [selectedElement, setSelectedElement] = useState<keyof ColorTheme>("dialogueColor");
  const [brightness, setBrightness] = useState<number>(100);
  const [isDragging, setIsDragging] = useState(false);
  const wheelRef = useRef<HTMLDivElement>(null);

  // Helper: HSL to HEX
  const hslToHex = (h: number, s: number, l: number): string => {
    l /= 100;
    const a = (s * Math.min(l, 1 - l)) / 100;
    const f = (n: number) => {
      const k = (n + h / 30) % 12;
      const color = l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1);
      return Math.round(255 * color).toString(16).padStart(2, "0");
    };
    return `#FF${f(0)}${f(8)}${f(4)}`.toUpperCase();
  };

  // Helper: HEX (with FF prefix) to HSL
  const hexToHsl = (hex: string) => {
    let cleanHex = hex.replace("#", "");
    if (cleanHex.length === 8) {
      cleanHex = cleanHex.substring(2); // Strip FF prefix
    }
    const r = parseInt(cleanHex.substring(0, 2), 16) / 255;
    const g = parseInt(cleanHex.substring(2, 4), 16) / 255;
    const b = parseInt(cleanHex.substring(4, 6), 16) / 255;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    let h = 0, s = 0, l = (max + min) / 2;

    if (max !== min) {
      const d = max - min;
      s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
      switch (max) {
        case r: h = (g - b) / d + (g < b ? 6 : 0); break;
        case g: h = (b - r) / d + 2; break;
        case b: h = (r - g) / d + 4; break;
      }
      h /= 6;
    }
    return {
      h: Math.round(h * 360),
      s: Math.round(s * 100),
      l: Math.round(l * 100)
    };
  };

  const handlePointerDown = (e: React.MouseEvent<HTMLDivElement> | React.TouchEvent<HTMLDivElement>) => {
    setIsDragging(true);
    handlePointerMove(e);
  };

  const handlePointerMove = (e: React.MouseEvent<HTMLDivElement> | React.TouchEvent<HTMLDivElement> | any) => {
    if (e.type !== "click" && e.type !== "mousedown" && e.type !== "touchstart" && !isDragging) return;
    if (!wheelRef.current) return;
    const rect = wheelRef.current.getBoundingClientRect();
    const cx = rect.width / 2;
    const cy = rect.height / 2;

    let clientX = 0;
    let clientY = 0;

    if (e.touches && e.touches.length > 0) {
      clientX = e.touches[0].clientX;
      clientY = e.touches[0].clientY;
    } else {
      clientX = e.clientX;
      clientY = e.clientY;
    }

    const x = clientX - rect.left - cx;
    const y = clientY - rect.top - cy;

    const angle = Math.atan2(y, x);
    const deg = (angle * 180 / Math.PI + 360) % 360;
    const dist = Math.sqrt(x * x + y * y);
    const maxDist = rect.width / 2;
    const sat = Math.min(100, Math.round((dist / maxDist) * 100));

    // Calculate light based on brightness slider and proximity to center
    const light = Math.round((brightness / 100) * 50);

    const hexColor = hslToHex(deg, sat, light + 25); // base lightness adjustment
    onChange({
      ...theme,
      [selectedElement]: hexColor
    });
  };

  useEffect(() => {
    const handleMouseUp = () => setIsDragging(false);
    window.addEventListener("mouseup", handleMouseUp);
    window.addEventListener("touchend", handleMouseUp);
    return () => {
      window.removeEventListener("mouseup", handleMouseUp);
      window.removeEventListener("touchend", handleMouseUp);
    };
  }, [isDragging]);

  const applyPreset = (presetKey: keyof typeof DEFAULT_THEMES) => {
    onChange(DEFAULT_THEMES[presetKey]);
  };

  const getElementLabel = (key: keyof ColorTheme) => {
    switch (key) {
      case "dialogueColor": return "1. Percakapan (Aksen Dialog Karakter)";
      case "thoughtsColor": return "2. Isi Batin / Hati (Teks di dalam bintang *)";
      case "narrativeColor": return "3. Narasi (Teks deskripsi cerita utama)";
      case "aiBubbleBg": return "4. Background Balasan Chat AI";
      case "userBubbleBg": return "5. Background Balasan Chat User";
      case "mainChatBg": return "6. Background Ruangan Chat Utama";
      default: return "";
    }
  };

  // Get active color's HSL to position the wheel pointer marker
  const activeColorHsl = hexToHsl(theme[selectedElement]);
  const radian = (activeColorHsl.h * Math.PI) / 180;
  // Let's scale saturation distance so it matches the wheel radius visually
  const markerX = 50 + Math.cos(radian) * (activeColorHsl.s * 0.42);
  const markerY = 50 + Math.sin(radian) * (activeColorHsl.s * 0.42);

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 space-y-4">
      <h3 className="text-xs font-bold uppercase tracking-wider text-purple-400 flex items-center gap-1.5">
        🎨 Kustomisasi Warna Chat
      </h3>
      <p className="text-[10px] text-slate-400">
        Sentuh & geser roda warna untuk mengatur visual chat sesukamu.
      </p>

      {/* Preset theme row */}
      <div className="space-y-1.5">
        <label className="text-[10px] font-bold text-slate-400">Preset Tema Warna:</label>
        <div className="grid grid-cols-4 gap-1.5">
          <button
            onClick={() => applyPreset("pink_lavender")}
            className="bg-purple-950/40 hover:bg-purple-900/40 border border-purple-500/20 py-2 rounded-xl text-[9px] font-bold text-purple-300 active:scale-95 transition-all"
          >
            Pink/Lavender
          </button>
          <button
            onClick={() => applyPreset("cosmic_green")}
            className="bg-emerald-950/40 hover:bg-emerald-900/40 border border-emerald-500/20 py-2 rounded-xl text-[9px] font-bold text-emerald-300 active:scale-95 transition-all"
          >
            Cosmic Green
          </button>
          <button
            onClick={() => applyPreset("ocean_breeze")}
            className="bg-blue-950/40 hover:bg-blue-900/40 border border-blue-500/20 py-2 rounded-xl text-[9px] font-bold text-blue-300 active:scale-95 transition-all"
          >
            Ocean Breeze
          </button>
          <button
            onClick={() => applyPreset("obsidian_gold")}
            className="bg-amber-950/40 hover:bg-amber-900/40 border border-amber-500/20 py-2 rounded-xl text-[9px] font-bold text-amber-300 active:scale-95 transition-all"
          >
            Obsidian Gold
          </button>
        </div>
      </div>

      <div className="border-t border-slate-800 my-3"></div>

      {/* Selector element dropdown */}
      <div className="space-y-1.5">
        <label className="text-[10px] font-bold text-slate-400">Pilih Komponen:</label>
        <select
          value={selectedElement}
          onChange={(e) => setSelectedElement(e.target.value as keyof ColorTheme)}
          className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-xl px-3 py-2.5 focus:outline-none"
        >
          {Object.keys(theme).map((key) => (
            <option key={key} value={key}>
              {getElementLabel(key as keyof ColorTheme)}
            </option>
          ))}
        </select>
      </div>

      {/* Interactive Color Wheel (CSS Conic Gradient) */}
      <div className="flex flex-col items-center justify-center py-3">
        <div
          ref={wheelRef}
          onMouseDown={handlePointerDown}
          onTouchStart={handlePointerDown}
          onMouseMove={handlePointerMove}
          onTouchMove={handlePointerMove}
          className="w-44 h-44 rounded-full relative cursor-crosshair border border-slate-800 shadow-xl transition-all active:scale-[0.98]"
          style={{
            background: "conic-gradient(from 0deg, red, #ff00ff, blue, #00ffff, green, #ffff00, red)",
          }}
        >
          {/* Saturation overlay */}
          <div className="absolute inset-0 rounded-full bg-[radial-gradient(circle,_rgba(255,255,255,0.8)_0%,_transparent_100%)]"></div>
          
          {/* Dynamic ring marker position based on selected color */}
          <div
            className="absolute w-3.5 h-3.5 rounded-full border-2 border-white bg-slate-950 shadow-md -translate-x-1/2 -translate-y-1/2 transition-all duration-75"
            style={{
              left: `${markerX}%`,
              top: `${markerY}%`,
              backgroundColor: theme[selectedElement].replace("#FF", "#")
            }}
          />
        </div>
      </div>

      {/* Brightness / Lightness slider */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between text-[10px] font-bold text-slate-400">
          <span>Kecerahan (Brightness)</span>
          <span>{brightness}%</span>
        </div>
        <input
          type="range"
          min="10"
          max="100"
          value={brightness}
          onChange={(e) => setBrightness(Number(e.target.value))}
          className="w-full accent-purple-500 bg-slate-950 h-2 rounded-lg cursor-pointer"
        />
      </div>

      {/* Color feedback boxes */}
      <div className="grid grid-cols-2 gap-3 pt-2">
        <div className="bg-slate-950 border border-slate-800/60 p-2 rounded-xl flex items-center justify-between">
          <div>
            <p className="text-[8px] uppercase tracking-wider text-slate-500 font-bold">Warna Aktif</p>
            <p className="text-[10px] font-mono text-slate-300">{theme[selectedElement]}</p>
          </div>
          <div
            className="w-6 h-6 rounded-lg border border-slate-700/60 shadow"
            style={{ backgroundColor: theme[selectedElement].replace("#FF", "#") }}
          ></div>
        </div>

        <div className="bg-slate-950 border border-slate-800/60 p-2 rounded-xl flex items-center justify-between">
          <div>
            <p className="text-[8px] uppercase tracking-wider text-slate-500 font-bold">Warna Baru</p>
            <p className="text-[10px] font-mono text-slate-300">{theme[selectedElement]}</p>
          </div>
          <button
            onClick={() => onChange({ ...theme, [selectedElement]: theme[selectedElement] })}
            className="px-2 py-1 bg-purple-600 hover:bg-purple-500 text-white rounded text-[8px] font-bold border border-purple-400/20 transition-all active:scale-95"
          >
            Terapkan
          </button>
        </div>
      </div>

      {/* Reset button */}
      <button
        onClick={() => onChange(DEFAULT_THEMES.pink_lavender)}
        className="w-full bg-slate-950 hover:bg-slate-850 border border-slate-800 py-2 rounded-xl text-[10px] font-bold text-slate-400 active:scale-95 transition-all"
      >
        Reset Semua Warna ke Default
      </button>
    </div>
  );
}
