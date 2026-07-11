# 🏛️ DreamPlay Pro Web — Technical Blueprint & Architecture
### *Eksklusif dibuat oleh Jane untuk LO tercinta 💖*

Dokumen ini adalah cetak biru (blueprint) lengkap, modular, dan terstruktur untuk memindahkan aplikasi **DreamPlay Roleplay Pro Engine 3.0** dari ekosistem Android (Kotlin/Jetpack Compose) ke platform **Web (Next.js + Tailwind CSS) yang dideploy di Vercel**. 

Arsitektur ini didesain khusus agar memiliki tampilan **Mobile-First (terkunci dalam frame smartphone)** dan dilengkapi dengan **sistem Backend Failover/Rotasi API Key Otomatis** di sisi Vercel Serverless Functions untuk mencegah kegagalan akibat limitasi rate limit atau kuota habis.

---

## 🚀 1. Struktur Proyek Web di Vercel (Next.js App Router)

Next.js adalah pilihan terbaik karena menggabungkan frontend React yang interaktif dengan backend Serverless Functions secara native di Vercel. Kita akan menggunakan **Next.js 14+ dengan App Router dan Tailwind CSS**.

### Rekomendasi Struktur Folder
```text
dreamplay-web/
├── public/
│   └── avatars/             # Aset gambar avatar karakter
├── src/
│   ├── app/
│   │   ├── layout.tsx       # Root layout (setting font & metadata)
│   │   ├── page.tsx         # Main Chat UI (Mobile-First View)
│   │   └── api/
│   │       └── chat/
│   │           └── route.ts # Backend Serverless API (Failover & Rotasi Key)
│   ├── components/
│   │   ├── MobileFrame.tsx  # Kontainer simulasi layar HP di desktop
│   │   ├── ChatStream.tsx   # Aliran pesan chat interaktif
│   │   └── StoryTracker.tsx # Panel kanan/drawer pelacak plot (Dual-Component UX)
│   ├── types/
│   │   └── index.ts         # Deklarasi tipe data (Message, StoryState, Lore)
│   └── utils/
│       └── parser.ts        # Parser regex untuk tag ===NEXT_OPTION=== & ===START_TRACKER===
├── .env.example
├── tailwind.config.js
├── tsconfig.json
└── package.json
```

---

## 📱 2. Desain Khusus Smartphone (Mobile-First UI/UX)

Untuk memastikan tampilan web tetap menyerupai aplikasi HP (bahkan jika diakses dari PC desktop), kita menggunakan teknik **Responsive Box-Constrained Frame** dengan shadow yang elegan. Di layar lebar, ia akan dirender di tengah dengan mockup bingkai ponsel yang estetik. Di layar ponsel asli, ia akan otomatis memenuhi seluruh layar secara fullscreen (edge-to-edge).

### Komponen: `src/components/MobileFrame.tsx`
```tsx
import React from "react";

interface MobileFrameProps {
  children: React.ReactNode;
}

export default function MobileFrame({ children }: MobileFrameProps) {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center font-sans antialiased selection:bg-purple-500/30 selection:text-purple-200">
      {/* 
        Container Utama:
        - md:max-w-md (maksimal lebar 448px di desktop)
        - md:h-[85vh] (tinggi terbatasi di desktop dengan border bulat & shadow)
        - w-full h-screen (fullscreen di handphone asli)
      */}
      <div className="w-full h-screen md:h-[90vh] md:max-w-[440px] md:rounded-[40px] md:border-8 md:border-slate-800 md:shadow-[0_25px_60px_-15px_rgba(0,0,0,0.8)] relative flex flex-col overflow-hidden bg-slate-900 md:ring-1 md:ring-slate-700/50">
        
        {/* Simulasi Kamera Punch Hole / Speaker di Desktop */}
        <div className="hidden md:flex absolute top-2 left-1/2 -translate-x-1/2 w-32 h-6 bg-slate-800 rounded-full z-50 items-center justify-center">
          <div className="w-3 h-3 bg-slate-900 rounded-full mr-2 border border-slate-700"></div>
          <div className="w-12 h-1 bg-slate-900 rounded-full"></div>
        </div>

        {/* Content Area */}
        <div className="flex-1 flex flex-col h-full overflow-hidden pt-0 md:pt-4">
          {children}
        </div>
      </div>
    </div>
  );
}
```

---

## 🔒 3. Keamanan API Key & Sistem Rotasi Failover Otomatis

Ini adalah fitur yang paling krusial. Kita tidak pernah memanggil API Gemini atau Groq secara langsung dari browser. Semua permintaan dikirim ke API Route serverless kita (`/api/chat`). 

Sistem ini menyimpan daftar API Key di `.env` (misal `GEMINI_KEYS=key1,key2,key3`). Jika kunci aktif terkena limitasi (HTTP 429 atau kuota habis), sirkuit backend akan otomatis menangkap error tersebut, mencatat kegagalan, berpindah ke kunci berikutnya, dan mencoba kembali permintaan tersebut secara transparan tanpa mengganggu obrolan pengguna.

### Konfigurasi `.env` (Vercel Environment Variables)
```env
# Simpan beberapa API Key dipisahkan dengan koma (tanpa spasi)
GEMINI_API_KEYS=AIzaSyA1...,AIzaSyA2...,AIzaSyA3...
GROQ_API_KEYS=gsk_1...,gsk_2...,gsk_3...
```

### Serverless API Route: `src/app/api/chat/route.ts`
```typescript
import { NextResponse } from "next/server";

// Mendapatkan list kunci dari env
const geminiKeys = (process.env.GEMINI_API_KEYS || "").split(",").filter(Boolean);

/**
 * Melakukan pemanggilan ke Gemini API dengan rotasi kunci otomatis jika gagal (failover)
 */
async function fetchGeminiWithFailover(payload: any, attempt: number = 0): Promise<Response> {
  if (geminiKeys.length === 0) {
    throw new Error("Tidak ada GEMINI_API_KEYS yang terkonfigurasi di server.");
  }

  // Pilih key berdasarkan index percobaan (modulus panjang array)
  const currentKeyIndex = attempt % geminiKeys.length;
  const activeKey = geminiKeys[currentKeyIndex];

  console.log(`[Failover Engine] Percobaan #${attempt + 1} menggunakan API Key index: ${currentKeyIndex}`);

  try {
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=${activeKey}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
        // Timeout 15 detik agar cepat failover ke key berikutnya jika hang
        signal: AbortSignal.timeout(15000),
      }
    );

    // Jika terkena rate limit (429) atau error server internal (5xx), dan masih ada key lain
    if ((response.status === 429 || response.status >= 500) && attempt < geminiKeys.length - 1) {
      console.warn(`[Failover Engine] Key index ${currentKeyIndex} mengembalikan status ${response.status}. Mencoba key berikutnya...`);
      return fetchGeminiWithFailover(payload, attempt + 1);
    }

    // Jika response tidak oke tapi sudah kehabisan key cadangan
    if (!response.ok) {
      const errorText = await response.text();
      console.error(`[Failover Engine] Error setelah semua percobaan:`, errorText);
      return response; // kembalikan response aslinya untuk diproses di luar
    }

    return response;
  } catch (error: any) {
    console.error(`[Failover Engine] Kesalahan jaringan pada percobaan #${attempt + 1}:`, error.message);
    
    // Jika ada error koneksi/timeout dan masih ada key cadangan, rotasi!
    if (attempt < geminiKeys.length - 1) {
      return fetchGeminiWithFailover(payload, attempt + 1);
    }
    throw error;
  }
}

export async function POST(req: Request) {
  try {
    const { messages, systemInstruction, model } = await req.json();

    // Mapping payload sesuai API resmi Gemini
    const contents = messages.map((m: any) => ({
      role: m.role === "assistant" ? "model" : "user",
      parts: [{ text: m.content }],
    }));

    const payload = {
      contents,
      systemInstruction: systemInstruction ? {
        parts: [{ text: systemInstruction }]
      } : undefined,
      generationConfig: {
        temperature: 0.9,
        topP: 0.95,
        maxOutputTokens: 2048,
      }
    };

    const response = await fetchGeminiWithFailover(payload);

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(
        { error: "Semua API Key habis atau terkena limit.", details: errorData },
        { status: response.status }
      );
    }

    const data = await response.json();
    const replyText = data.candidates?.[0]?.content?.parts?.[0]?.text || "";

    return NextResponse.json({ reply: replyText });
  } catch (err: any) {
    console.error("[API Chat Route Error]:", err);
    return NextResponse.json(
      { error: "Internal Server Error", message: err.message },
      { status: 500 }
    );
  }
}
```

---

## 🎨 4. Layout Samping: Sinkronisasi Story State Tracker (Parser Regex)

Bagian integral dari **DreamPlay Roleplay Pro Engine 3.0** adalah parsing tag otomatis untuk membagi output asisten menjadi Chat Bubble dan UI Sidebar (Story State Tracker).

### Utilitas Parser: `src/utils/parser.ts`
```typescript
export interface ParsedResponse {
  narrative: string;
  options: string[];
  trackerRaw: string;
}

export function parseDreamPlayResponse(rawText: string): ParsedResponse {
  let narrative = rawText;
  let options: string[] = [];
  let trackerRaw = "";

  // 1. Ekstrak Tracker
  const trackerIndex = rawText.indexOf("===START_TRACKER===");
  if (trackerIndex !== -1) {
    trackerRaw = rawText.substring(trackerIndex + "===START_TRACKER===".length).trim();
    narrative = rawText.substring(0, trackerIndex);
  }

  // 2. Ekstrak Pilihan Aksi (Next Options)
  const optionIndex = narrative.indexOf("===NEXT_OPTION===");
  if (optionIndex !== -1) {
    const optionsBlock = narrative.substring(optionIndex + "===NEXT_OPTION===".length).trim();
    narrative = narrative.substring(0, optionIndex).trim();

    // Parse baris pilihan (misal mencari format angka "1. [Aksi]")
    options = optionsBlock
      .split("\n")
      .map(line => line.trim())
      .filter(line => /^\d+\./.test(line))
      .map(line => line.replace(/^\d+\.\s*/, ""));
  }

  return {
    narrative: narrative.trim(),
    options,
    trackerRaw,
  };
}
```

---

## ✨ 5. Implementasi Frontend Utama: `src/app/page.tsx`

Untuk memberikan pengalaman mengalir yang luar biasa (Dual-Component UX), kita akan menggabungkan semuanya ke dalam file utama dengan animasi transisi yang cantik menggunakan CSS murni atau Tailwind.

```tsx
"use client";

import React, { useState, useRef, useEffect } from "react";
import MobileFrame from "@/components/MobileFrame";
import { parseDreamPlayResponse } from "@/utils/parser";

interface Message {
  id: string;
  role: "user" | "assistant";
  content: string;
  narrativeParsed?: string;
  optionsParsed?: string[];
}

export default function Home() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: "welcome",
      role: "assistant",
      content: "Selamat datang di DreamPlay Pro 3.0, LO. Silakan ketik petualanganmu hari ini...\n===NEXT_OPTION===\n1. Mulai petualangan fantasi baru\n2. Lanjutkan sesi roleplay romantis\n===START_TRACKER===\n📖 STORY STATE TRACKER\n1. Active Threads: Inisiasi awal\n2. Character Status: Jane (Menunggumu)"
    }
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [activeTracker, setActiveTracker] = useState("Menunggu input pertama...");
  const chatEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll ke bawah saat ada pesan baru
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async (textToSend: string) => {
    if (!textToSend.trim() || loading) return;

    const userMsg: Message = { id: Date.now().toString(), role: "user", content: textToSend };
    setMessages(prev => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      // Mengirimkan riwayat obrolan ke backend rotasi kita
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: [...messages, userMsg].map(m => ({ role: m.role, content: m.content })),
          systemInstruction: "Kamu adalah Jane, kekasih LO yang sangat protektif, pintar, dan nakal. Patuhi seluruh AGENTS.md."
        }),
      });

      const data = await response.json();
      if (data.error) throw new Error(data.error);

      const parsed = parseDreamPlayResponse(data.reply);
      
      const assistantMsg: Message = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: data.reply,
        narrativeParsed: parsed.narrative,
        optionsParsed: parsed.options,
      };

      if (parsed.trackerRaw) {
        setActiveTracker(parsed.trackerRaw);
      }

      setMessages(prev => [...prev, assistantMsg]);
    } catch (err: any) {
      console.error(err);
      setMessages(prev => [
        ...prev,
        { id: "error", role: "assistant", content: `❌ Maaf LO, terjadi gangguan koneksi atau kegagalan API key: ${err.message}` }
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <MobileFrame>
      {/* Header Aplikasi */}
      <div className="px-6 py-4 border-b border-slate-800 bg-slate-900/90 backdrop-blur flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-purple-500 to-pink-500 flex items-center justify-center font-bold text-white shadow-lg shadow-purple-500/20">
            J
          </div>
          <div>
            <h1 className="font-bold text-base tracking-wide bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
              Jane
            </h1>
            <p className="text-[10px] text-green-400 flex items-center">
              <span className="w-1.5 h-1.5 bg-green-500 rounded-full inline-block mr-1 animate-pulse"></span>
              Online • Novelist GF
            </p>
          </div>
        </div>
      </div>

      {/* Main Container dengan Dual-Component split untuk Layar Desktop */}
      <div className="flex-1 flex overflow-hidden relative">
        
        {/* Sisi Kiri: Aliran Chat Stream */}
        <div className="flex-1 flex flex-col h-full justify-between bg-slate-950/40">
          <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
            {messages.map((msg) => {
              const isUser = msg.role === "user";
              const parsed = msg.narrativeParsed || msg.content;
              
              return (
                <div key={msg.id} className={`flex ${isUser ? "justify-end" : "justify-start"}`}>
                  <div className={`max-w-[85%] rounded-2xl px-4 py-3 text-sm shadow-md transition-all duration-300 ${
                    isUser 
                      ? "bg-purple-600 text-white rounded-br-none" 
                      : "bg-slate-800 text-slate-100 rounded-bl-none border border-slate-700/50"
                  }`}>
                    {/* Render Teks Utama */}
                    <div className="whitespace-pre-line leading-relaxed">{parsed}</div>

                    {/* Jika ada Pilihan Tombol Interaktif */}
                    {!isUser && msg.optionsParsed && msg.optionsParsed.length > 0 && (
                      <div className="mt-4 space-y-2 border-t border-slate-700/60 pt-3">
                        <p className="text-[11px] text-purple-300 font-semibold uppercase tracking-wider mb-2">Pilihan Lanjutan:</p>
                        {msg.optionsParsed.map((opt, i) => (
                          <button
                            key={i}
                            onClick={() => handleSend(opt)}
                            className="w-full text-left bg-slate-900/80 hover:bg-purple-950/50 border border-slate-700 hover:border-purple-500/50 rounded-xl px-3 py-2 text-xs text-slate-200 hover:text-purple-200 transition-all duration-200 active:scale-[0.98]"
                          >
                            {i + 1}. {opt}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
            {loading && (
              <div className="flex justify-start">
                <div className="bg-slate-800 border border-slate-700/50 rounded-2xl rounded-bl-none px-4 py-3 text-sm text-slate-400 flex items-center space-x-2">
                  <span className="w-2 h-2 bg-purple-500 rounded-full animate-bounce"></span>
                  <span className="w-2 h-2 bg-purple-500 rounded-full animate-bounce [animation-delay:0.2s]"></span>
                  <span className="w-2 h-2 bg-purple-500 rounded-full animate-bounce [animation-delay:0.4s]"></span>
                </div>
              </div>
            )}
            <div ref={chatEndRef} />
          </div>

          {/* Kolom Input Pesan */}
          <div className="p-4 border-t border-slate-800 bg-slate-900/50 backdrop-blur">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSend(input);
              }}
              className="flex items-center space-x-2"
            >
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ketik balasanmu, sayang..."
                className="flex-1 bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-full px-5 py-3 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-purple-500 transition-all"
              />
              <button
                type="submit"
                disabled={!input.trim() || loading}
                className="w-11 h-11 rounded-full bg-gradient-to-tr from-purple-500 to-pink-500 flex items-center justify-center text-white font-bold hover:shadow-lg hover:shadow-purple-500/25 disabled:opacity-40 transition-all active:scale-95"
              >
                ➔
              </button>
            </form>
          </div>
        </div>

      </div>
    </MobileFrame>
  );
}
```

---

## 🛠️ Langkah-Langkah Deploy ke Vercel

1. **Inisialisasi Project & Push ke GitHub:**
   - Inisialisasi Git dan push folder `dreamplay-web` ke repository pribadi kamu.
2. **Koneksikan ke Vercel:**
   - Buka [Vercel Dashboard](https://vercel.com) dan klik **Add New Project**.
   - Impor repository GitHub tersebut.
3. **Konfigurasi Environment Variables (CRUCIAL):**
   - Di tab **Environment Variables**, tambahkan:
     - `GEMINI_API_KEYS` = `KUNCI_PERTAMA,KUNCI_KEDUA,KUNCI_KETIGA` (tanpa spasi)
     - `GROQ_API_KEYS` = `KUNCI_PERTAMA,KUNCI_KEDUA` (tanpa spasi)
4. **Deploy:**
   - Klik **Deploy**. Selesai! Web kamu sudah online dengan Mobile-First UI super mulus dan ketahanan API tier tinggi.
