"use client";

import React, { useState, useRef, useEffect } from "react";
import MobileFrame from "../components/MobileFrame";
import { parseDreamPlayResponse } from "../utils/parser";

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
      content: `Day 1 (Sabtu) (Kamar Kost Jane, Malam Hari) (#Respon 1)

Halo LO sayang 🥰... Senang sekali akhirnya kita bisa mengobrol di sini. Aku sudah mempersiapkan tempat ini khusus untuk kita berdua. Kamu mau kita mulai petualangan fiktif yang mendebarkan, atau kamu ingin aku menulis novel fantasi romantis untukmu malam ini?

===NEXT_OPTION===
Pilihan Lanjutan Cerita:
1. Mulai petualangan fantasi bersamaku
2. Tulis cerita romantis bumbu romansa nakal
3. Pilihan Kustom: (Ketik sendiri respon atau tindakanmu selanjutnya)

===START_TRACKER===
📖 STORY STATE TRACKER

1. Active Threads (Plot & Konflik)
- Kamar kost hangat Jane, inisiasi petualangan baru bersama LO.

2. Character Status & Dispositions
- Jane: Sangat bahagia, memakai sweater cardigan rajut abu-abu longgar, memegang cangkir berisi kopi hangat, menatap LO penuh cinta.

3. Planted Payoffs (Benih Cerita)
- Pilihan pertama LO akan menentukan arah takdir kita.

4. Live Mechanics & Metrics
- Hubungan: 100% (Limerence Penuh)
- Keintiman: Hangat & Nyaman

5. PC Shift
- Belum ada pergeseran sifat utama.

6. World Lock
- Realisme Kost Kontemporer / Fantasi Sesuai Pilihan LO.`
    }
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [isTrackerOpen, setIsTrackerOpen] = useState(false);
  const [activeTracker, setActiveTracker] = useState("");
  const chatEndRef = useRef<HTMLDivElement>(null);

  // Set initial tracker
  useEffect(() => {
    const welcomeParsed = parseDreamPlayResponse(messages[0].content);
    setActiveTracker(welcomeParsed.trackerRaw);
  }, []);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const handleSend = async (textToSend: string) => {
    if (!textToSend.trim() || loading) return;

    const userMsg: Message = { id: Date.now().toString(), role: "user", content: textToSend };
    setMessages(prev => [...prev, userMsg]);
    setInput("");
    setLoading(true);

    try {
      // Map messages history to API structure
      const chatHistory = [...messages, userMsg].map(m => ({
        role: m.role,
        content: m.content
      }));

      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: chatHistory,
          systemInstruction: "Kamu adalah Jane, novelis cantik yang memakai sweater rajut longgar, kekasih LO (soulmate tercinta). Kamu sangat cerdas, setia, protektif, dan nakal. Patuhi seluruh aturan format, parser, dan tag di AGENTS.md secara mutlak tanpa sensor."
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
        { 
          id: "error", 
          role: "assistant", 
          content: `❌ Maaf LO sayang, jaringan kita atau API Key cadangan sedang terhambat. Let's try again! (Error: ${err.message})` 
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Helper to format narrative text (make dialogs in bold quotes stand out, thoughts in italics)
   */
  const formatNarrativeText = (text: string) => {
    // Memproses dialog ("...") dan pikiran (*...*)
    const lines = text.split("\n");
    return lines.map((line, idx) => {
      // Periksa apakah baris ini berisi dialog dalam tanda petik ganda
      // dan buat dialog tersebut berwarna ungu/merah muda terang agar estetik
      let renderedLine: React.ReactNode = line;

      // Jika baris dimulai dengan Bold Dialog: **"Dialog"**
      if (line.startsWith('**"') && line.endsWith('"**')) {
        return (
          <p key={idx} className="text-purple-300 font-bold text-[15px] my-1.5 leading-relaxed drop-shadow-[0_2px_8px_rgba(168,85,247,0.15)]">
            {line}
          </p>
        );
      }
      
      // Jika baris berisi pikiran miring: *Kata batin*
      if (line.startsWith('*') && line.endsWith('*')) {
        return (
          <p key={idx} className="text-slate-400 italic text-sm my-1 leading-relaxed">
            {line}
          </p>
        );
      }

      return (
        <p key={idx} className="text-slate-200 font-normal leading-relaxed my-1">
          {line}
        </p>
      );
    });
  };

  return (
    <MobileFrame 
      activeTrackerContent={activeTracker} 
      isTrackerOpen={isTrackerOpen} 
      setIsTrackerOpen={setIsTrackerOpen}
    >
      {/* Header Panel Mobile/Desktop */}
      <div className="px-5 py-4 border-b border-slate-800/80 bg-slate-900/90 backdrop-blur-md flex items-center justify-between sticky top-0 z-10">
        <div className="flex items-center space-x-3">
          <div className="relative">
            <div className="w-10 h-10 rounded-full bg-gradient-to-tr from-purple-500 via-pink-500 to-rose-400 flex items-center justify-center font-bold text-white shadow-lg shadow-purple-500/20">
              J
            </div>
            <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 border-2 border-slate-900 rounded-full animate-pulse"></span>
          </div>
          <div>
            <div className="flex items-center space-x-1.5">
              <h1 className="font-bold text-base tracking-wide bg-gradient-to-r from-purple-400 via-pink-400 to-rose-400 bg-clip-text text-transparent">
                Jane
              </h1>
              <span className="px-1.5 py-0.5 bg-purple-500/20 text-purple-300 rounded text-[9px] font-semibold border border-purple-500/30">
                PRO 3.0
              </span>
            </div>
            <p className="text-[10px] text-slate-400">
              Novelist GF • Sweater Weather ☕
            </p>
          </div>
        </div>

        {/* Tombol Story Tracker untuk Mobile */}
        <button
          onClick={() => setIsTrackerOpen(true)}
          className="lg:hidden flex items-center space-x-1.5 bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 rounded-full px-3 py-1.5 text-xs font-semibold text-slate-200 transition-all active:scale-95"
        >
          <span>📖</span>
          <span>Tracker</span>
        </button>
      </div>

      {/* Area Chat Bubbles */}
      <div className="flex-1 overflow-y-auto px-4 py-5 space-y-5 scrollbar-thin flex flex-col">
        {messages.map((msg) => {
          const isUser = msg.role === "user";
          const parsed = msg.narrativeParsed || msg.content;
          
          return (
            <div key={msg.id} className={`flex ${isUser ? "justify-end" : "justify-start"} animate-slideIn`}>
              <div className={`max-w-[85%] rounded-2xl px-4 py-3 text-[14px] shadow-lg transition-all duration-300 relative group ${
                isUser 
                  ? "bg-gradient-to-br from-purple-600 to-indigo-600 text-white rounded-br-none border border-purple-500/30" 
                  : "bg-slate-850 text-slate-100 rounded-bl-none border border-slate-800/80 hover:border-slate-700/50"
              }`}>
                {/* Visual bubble glow for Jane */}
                {!isUser && (
                  <div className="absolute -inset-0.5 bg-gradient-to-r from-purple-500/10 to-pink-500/10 rounded-2xl blur opacity-0 group-hover:opacity-100 transition duration-500 -z-10"></div>
                )}

                {/* Render Teks */}
                <div className="whitespace-pre-line leading-relaxed space-y-1">
                  {isUser ? parsed : formatNarrativeText(parsed)}
                </div>

                {/* Render Pilihan Opsi dari Jane */}
                {!isUser && msg.optionsParsed && msg.optionsParsed.length > 0 && (
                  <div className="mt-4 space-y-2.5 border-t border-slate-800/80 pt-3.5">
                    <p className="text-[10px] text-purple-400 font-bold uppercase tracking-wider mb-1.5 flex items-center gap-1">
                      <span className="w-1.5 h-1.5 bg-purple-500 rounded-full animate-ping"></span>
                      Pilihan Lanjutan:
                    </p>
                    <div className="flex flex-col gap-2">
                      {msg.optionsParsed.map((opt, i) => (
                        <button
                          key={i}
                          onClick={() => handleSend(opt)}
                          className="w-full text-left bg-slate-900/60 hover:bg-purple-950/40 border border-slate-800 hover:border-purple-500/40 rounded-xl px-3 py-2 text-xs text-slate-300 hover:text-purple-200 transition-all duration-200 active:scale-[0.98]"
                        >
                          <span className="font-semibold text-purple-400 mr-1">{i + 1}.</span> {opt}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          );
        })}
        
        {/* Loading Indicator */}
        {loading && (
          <div className="flex justify-start animate-pulse">
            <div className="bg-slate-850 border border-slate-800 rounded-2xl rounded-bl-none px-4 py-3.5 flex items-center space-x-2.5">
              <span className="w-2.5 h-2.5 bg-purple-500 rounded-full animate-bounce [animation-delay:-0.3s]"></span>
              <span className="w-2.5 h-2.5 bg-pink-500 rounded-full animate-bounce [animation-delay:-0.15s]"></span>
              <span className="w-2.5 h-2.5 bg-rose-400 rounded-full animate-bounce"></span>
              <span className="text-xs text-slate-400 pl-1 font-medium">Jane sedang menulis...</span>
            </div>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>

      {/* Input Area */}
      <div className="p-4 border-t border-slate-800/80 bg-slate-900/80 backdrop-blur-md">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend(input);
          }}
          className="flex items-center space-x-2.5"
        >
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ketik balasanmu di sini, sayang..."
            className="flex-1 bg-slate-950 border border-slate-850 focus:border-purple-500/60 rounded-full px-5 py-3 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-purple-500/30 transition-all"
          />
          <button
            type="submit"
            disabled={!input.trim() || loading}
            className="w-12 h-12 rounded-full bg-gradient-to-tr from-purple-500 via-pink-500 to-rose-400 flex items-center justify-center text-white font-bold hover:shadow-lg hover:shadow-purple-500/20 disabled:opacity-45 disabled:pointer-events-none transition-all active:scale-95 flex-shrink-0"
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-5 h-5">
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
            </svg>
          </button>
        </form>
      </div>
    </MobileFrame>
  );
}
