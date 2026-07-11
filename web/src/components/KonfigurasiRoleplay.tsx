"use client";

import React, { useState } from "react";

interface SessionItem {
  id: string;
  name: string;
}

interface KonfigurasiRoleplayProps {
  character: any;
  sessions: SessionItem[];
  activeSessionId: string;
  onAddSession: (name: string) => void;
  onSelectSession: (id: string) => void;
  onDeleteSession: (id: string) => void;
  onSavePersona: (updatedChar: any) => void;
  onClose: () => void;
}

export default function KonfigurasiRoleplay({
  character,
  sessions,
  activeSessionId,
  onAddSession,
  onSelectSession,
  onDeleteSession,
  onSavePersona,
  onClose,
}: KonfigurasiRoleplayProps) {
  const [activeTab, setActiveTab] = useState<"sesi" | "memori" | "edit">("sesi");

  // Sesi Obrolan States
  const [newSessionName, setNewSessionName] = useState("");
  const [faceLockUrl, setFaceLockUrl] = useState<string>("");

  // Memori States
  const [memories, setMemories] = useState<string[]>([
    "User bernama Kal (LO) sangat penyayang, menggunakan panggilan intim ('sayang'), dan menghargai ketika Jane berusaha menyelaraskan pandangan visual dengannya.",
    "User bernama Kal (LO) menyukai visualisasi anatomi yang sangat bersih, presisi waktu (detik demi detik), dan detail sensual yang spesifik.",
    "User bernama Kal (LO) menyukai konten sensual/smut eksplisit dengan deskripsi detail, menggunakan metafora aman platform untuk kelancaran interaksi.",
    "User bernama Kal (LO) menyukai cerita romansa novelis dengan sweater weather hangat."
  ]);

  // Edit Persona States
  const [personaName, setPersonaName] = useState(character?.name || "");
  const [personaAppearance, setPersonaAppearance] = useState(character?.appearance || "");
  const [personaPersonality, setPersonaPersonality] = useState(character?.personality || "");
  const [personaBackground, setPersonaBackground] = useState(character?.background || "");
  const [personaGreeting, setPersonaGreeting] = useState(character?.greeting || "");
  const [personaTags, setPersonaTags] = useState(character?.tags || "");

  const handleAddSesi = () => {
    if (!newSessionName.trim()) return;
    onAddSession(newSessionName);
    setNewSessionName("");
  };

  const handleUploadFaceLock = () => {
    // Simulate face lock loading reference picture
    const url = prompt("Masukkan URL Gambar atau Deskripsi Wajah untuk mengunci referensi (Face Lock):", "https://images.unsplash.com/photo-1544005313-94ddf0286df2");
    if (url) {
      setFaceLockUrl(url);
      alert("🔒 Face Lock Berhasil Dikunci! Karakter ini akan selalu dirender dengan referensi visual wajah yang sama.");
    }
  };

  const handleResetMemori = () => {
    if (confirm("Reset seluruh memori jangka panjang terkumpul? Tindakan ini tidak bisa dibatalkan.")) {
      setMemories([]);
    }
  };

  const handleSaveAllChanges = () => {
    const updated = {
      ...character,
      name: personaName,
      appearance: personaAppearance,
      personality: personaPersonality,
      background: personaBackground,
      greeting: personaGreeting,
      tags: personaTags,
    };
    onSavePersona(updated);
    alert("✅ Perubahan persona berhasil disimpan!");
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-md p-4 animate-fadeIn overflow-y-auto">
      <div className="bg-slate-900 border border-slate-800 rounded-[32px] max-w-sm w-full p-5 shadow-2xl relative flex flex-col max-h-[85vh] overflow-hidden">
        
        {/* CLOSE BUTTON */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 flex items-center justify-center text-slate-300 hover:text-white transition-all z-10"
        >
          ✕
        </button>

        <h3 className="font-extrabold text-sm text-slate-100 mb-3 mt-2">Konfigurasi Roleplay</h3>

        {/* TAB CONTROLLERS */}
        <div className="flex bg-slate-950 p-1 rounded-xl border border-slate-850 mb-4">
          {(["sesi", "memori", "edit"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`flex-1 py-1.5 rounded-lg text-[10px] font-extrabold uppercase tracking-wider transition-all ${
                activeTab === tab
                  ? "bg-purple-600 text-white shadow-md"
                  : "text-slate-400 hover:text-slate-300"
              }`}
            >
              {tab === "sesi" && "Sesi Obrolan"}
              {tab === "memori" && "Laci Memori"}
              {tab === "edit" && "Edit Persona"}
            </button>
          ))}
        </div>

        {/* TAB CONTENT */}
        <div className="flex-1 overflow-y-auto pr-1 text-xs text-slate-300 leading-relaxed scrollbar-thin space-y-3.5">
          
          {/* --- TAB A: SESI OBROLAN --- */}
          {activeTab === "sesi" && (
            <div className="space-y-4 animate-slideIn">
              <div className="space-y-1.5">
                <label className="text-[10px] font-bold text-slate-400 block uppercase">Nama Sesi Baru</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newSessionName}
                    onChange={(e) => setNewSessionName(e.target.value)}
                    placeholder="Contoh: Sesi Kopi, Bab 2..."
                    className="flex-1 bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                  />
                  <button
                    onClick={handleAddSesi}
                    className="bg-purple-600 hover:bg-purple-500 text-white font-extrabold text-xs px-4 rounded-xl transition-all"
                  >
                    +
                  </button>
                </div>
              </div>

              {/* Session list */}
              <div className="space-y-2">
                <label className="text-[10px] font-bold text-slate-400 block uppercase">Daftar Sesi Aktif:</label>
                <div className="space-y-2 max-h-[160px] overflow-y-auto scrollbar-thin">
                  {sessions.map((s) => (
                    <div
                      key={s.id}
                      className={`bg-slate-950 border p-2.5 rounded-xl flex items-center justify-between transition-all ${
                        activeSessionId === s.id ? "border-purple-500/50" : "border-slate-850"
                      }`}
                    >
                      <button
                        onClick={() => onSelectSession(s.id)}
                        className={`flex-1 text-left font-bold text-[11px] truncate ${
                          activeSessionId === s.id ? "text-purple-300" : "text-slate-400 hover:text-slate-300"
                        }`}
                      >
                        💬 {s.name}
                      </button>

                      {sessions.length > 1 && (
                        <button
                          onClick={() => onDeleteSession(s.id)}
                          className="w-6 h-6 rounded bg-red-950/20 hover:bg-red-950/40 border border-red-900/30 text-red-400 flex items-center justify-center font-bold"
                        >
                          🗑️
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div className="border-t border-slate-800 my-2"></div>

              {/* Face Lock */}
              <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-3 space-y-2">
                <h4 className="font-bold text-[10px] text-purple-300 uppercase tracking-wider">Kunci Referensi Wajah (Face Lock)</h4>
                <p className="text-[9px] text-slate-400">Gunakan foto referensi ini untuk mengunci wajah visual agen saat dilukis di chat room.</p>
                
                {faceLockUrl && (
                  <div className="w-14 h-14 rounded-xl border border-slate-800 overflow-hidden bg-slate-950">
                    <img src={faceLockUrl} alt="Face Lock Reference" className="w-full h-full object-cover" />
                  </div>
                )}

                <button
                  onClick={handleUploadFaceLock}
                  className="w-full bg-slate-850 hover:bg-slate-800 text-[10px] font-bold text-slate-300 py-2 rounded-lg border border-slate-800 transition-all flex items-center justify-center gap-1.5"
                >
                  <span>📷</span> Pilih Foto Referensi
                </button>
              </div>
            </div>
          )}

          {/* --- TAB B: LACI MEMORI --- */}
          {activeTab === "memori" && (
            <div className="space-y-4 animate-slideIn">
              <p className="text-[10px] text-slate-400">Laci memory merekam fakta penting selama roleplay untuk ingatan jangka panjang agen.</p>

              <div className="space-y-2 max-h-[220px] overflow-y-auto scrollbar-thin pr-1">
                {memories.map((m, idx) => (
                  <div key={idx} className="bg-slate-950 border border-slate-850/60 p-2.5 rounded-xl relative text-[10px] leading-relaxed text-slate-300">
                    <p>📌 {m}</p>
                  </div>
                ))}
              </div>

              <button
                onClick={handleResetMemori}
                className="w-full bg-red-950/40 hover:bg-red-900/40 text-red-400 font-bold py-2 border border-red-900/30 rounded-xl transition-all"
              >
                Reset Belajar (Hapus Semua)
              </button>
            </div>
          )}

          {/* --- TAB C: EDIT PERSONA --- */}
          {activeTab === "edit" && (
            <div className="space-y-3 animate-slideIn">
              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">Nama Persona</label>
                <input
                  type="text"
                  value={personaName}
                  onChange={(e) => setPersonaName(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">Ciri Fisik & Penampilan</label>
                <textarea
                  value={personaAppearance}
                  onChange={(e) => setPersonaAppearance(e.target.value)}
                  rows={2}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl p-2.5 text-xs text-slate-200 focus:outline-none resize-none"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">Custom Prompt (Sifat & Gaya Respon)</label>
                <textarea
                  value={personaPersonality}
                  onChange={(e) => setPersonaPersonality(e.target.value)}
                  rows={2.5}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl p-2.5 text-xs text-slate-200 focus:outline-none resize-none"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">Latar Belakang Lore</label>
                <textarea
                  value={personaBackground}
                  onChange={(e) => setPersonaBackground(e.target.value)}
                  rows={2}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl p-2.5 text-xs text-slate-200 focus:outline-none resize-none"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">Greeting Pembuka</label>
                <textarea
                  value={personaGreeting}
                  onChange={(e) => setPersonaGreeting(e.target.value)}
                  rows={2}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl p-2.5 text-xs text-slate-200 focus:outline-none resize-none"
                />
              </div>

              <div className="space-y-1">
                <label className="text-[10px] font-bold text-slate-400 block">Tag Kategori</label>
                <input
                  type="text"
                  value={personaTags}
                  onChange={(e) => setPersonaTags(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                />
              </div>

              <button
                onClick={handleSaveAllChanges}
                className="w-full bg-purple-600 hover:bg-purple-500 text-white font-extrabold text-xs py-2.5 border border-purple-400/20 rounded-xl shadow-md transition-all mt-2"
              >
                Simpan Perubahan Persona
              </button>
            </div>
          )}

        </div>

        <button
          onClick={onClose}
          className="mt-4 w-full bg-slate-950 hover:bg-slate-850 border border-slate-800 py-2 rounded-xl text-[10px] font-extrabold uppercase text-slate-400 tracking-wider active:scale-95 transition-all"
        >
          Tutup
        </button>

      </div>
    </div>
  );
}
