"use client";

import React, { useState, useEffect } from "react";

interface DreamingAvatarProps {
  onComplete: (character: any) => void;
  onCancel: () => void;
}

export default function DreamingAvatar({ onComplete, onCancel }: DreamingAvatarProps) {
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [loadingMessage, setLoadingMessage] = useState("");
  const [completed, setCompleted] = useState(false);

  // Configuration States
  const [gender, setGender] = useState<"Female" | "Male" | "Trans">("Female");
  const [style, setStyle] = useState<"Realistic" | "Anime">("Realistic");
  const [ethnicity, setEthnicity] = useState("Asian");
  const [skinTone, setSkinTone] = useState("Olive");
  const [eyeColor, setEyeColor] = useState("Brown");
  const [hairColor, setHairColor] = useState("Brunette");
  const [hairStyle, setHairStyle] = useState("Long Wavy");
  const [bodyType, setBodyType] = useState("Slim");
  const [breastSize, setBreastSize] = useState("Medium");
  const [buttSize, setButtSize] = useState("Medium");

  // Step 5 Form States
  const [name, setName] = useState("");
  const [age, setAge] = useState(24);
  const [personality, setPersonality] = useState("Sweet");
  const [relationship, setRelationship] = useState("Lover");
  const [occupation, setOccupation] = useState("Gaming Streamer");
  const [mainHobby, setMainHobby] = useState("Cosplay");
  const [fetish, setFetish] = useState("Roleplay");
  const [customPrompt, setCustomPrompt] = useState("");
  const [firstGreeting, setFirstGreeting] = useState("");

  const loadingPhrases = [
    "Menghubungkan impian bebas...",
    "Menjalin DNA kepribadian tanpa batas...",
    "Melukis postur visual eksklusif...",
    "Menghidupkan virtual partner...",
    "Meramu aroma kopi dingin dan sandalwood...",
    "Mengatur denyut emosi limerence..."
  ];

  useEffect(() => {
    if (loading) {
      let currentProgress = 0;
      setLoadingMessage(loadingPhrases[0]);

      const interval = setInterval(() => {
        currentProgress += Math.floor(Math.random() * 8) + 4;
        if (currentProgress >= 100) {
          currentProgress = 100;
          clearInterval(interval);
          setLoading(false);
          setCompleted(true);
        }
        setProgress(currentProgress);

        // Shift messages
        const phraseIdx = Math.floor((currentProgress / 100) * loadingPhrases.length);
        if (loadingPhrases[phraseIdx]) {
          setLoadingMessage(loadingPhrases[phraseIdx]);
        }
      }, 150);

      return () => clearInterval(interval);
    }
  }, [loading]);

  const handleStartCompile = () => {
    if (!name.trim()) {
      alert("Masukkan Nama Agent impianmu terlebih dahulu!");
      return;
    }
    setLoading(true);
  };

  const handleBringToLife = () => {
    // Generate beautiful visual seed index based on choices
    const promptSeed = `${style} portrait, ${gender} avatar, ${ethnicity} skin ${skinTone}, ${hairStyle} ${hairColor} hair, ${eyeColor} eyes, ${bodyType} body type, highly detailed, realistic texture, sensual lighting`;
    const finalAvatarUrl = `https://image.pollinations.ai/prompt/${encodeURIComponent(promptSeed)}?width=420&height=560&nologo=true&seed=${Math.floor(Math.random() * 999999)}`;

    // Build Character object compatible with page.tsx
    const newCharacter = {
      id: `dreaming_${Date.now()}`,
      name,
      avatarUri: "gradient_rose", // custom indicator
      customAvatarUrl: finalAvatarUrl,
      appearance: `${gender}, Gaya: ${style}, Etnis: ${ethnicity}, Warna Kulit: ${skinTone}. Fisik: Rambut ${hairStyle} berwarna ${hairColor}, mata berwarna ${eyeColor}. Tubuh ${bodyType} dengan proporsi dada ${breastSize} dan pinggul ${buttSize}.`,
      personality: `Sifat & Kepribadian: ${personality}, hubungan dengan LO: ${relationship}. Pekerjaan: ${occupation}, hobi utama: ${mainHobby}, Fetish/Preferensi: ${fetish}. Custom Prompt: ${customPrompt || "Uncensored fully, devoted, loving companion."}`,
      background: `Pendamping impian yang dibuat khusus oleh LO. Karakter berumur ${age} tahun yang sangat mencintai LO.`,
      greeting: firstGreeting || `Hai sayang... akhirnya kita bisa bersama seutuhnya di sini. Aku ${name}, milikmu sekarang. Mau mengobrol apa hari ini? 💕`,
      tags: `Uncensored, ${style}, Custom, ${relationship}`,
      isPredefined: false
    };

    onComplete(newCharacter);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-md p-4 animate-fadeIn overflow-y-auto">
      <div className="bg-slate-900 border border-slate-800 rounded-[32px] max-w-sm w-full p-6 shadow-2xl relative flex flex-col max-h-[90vh] overflow-hidden">
        
        {/* CLOSE BUTTON */}
        {!loading && !completed && (
          <button
            onClick={onCancel}
            className="absolute top-4 right-4 w-8 h-8 rounded-full bg-slate-800 hover:bg-slate-700 flex items-center justify-center text-slate-300 hover:text-white transition-all"
          >
            ✕
          </button>
        )}

        {/* LOADING STATE (HEART PULSE) */}
        {loading && (
          <div className="flex-1 flex flex-col items-center justify-center py-10 space-y-6">
            <div className="relative">
              <div className="w-24 h-24 bg-rose-600 rounded-full flex items-center justify-center animate-ping absolute opacity-25"></div>
              <div className="w-24 h-24 bg-rose-500 rounded-full flex items-center justify-center animate-pulse relative z-10 shadow-lg shadow-rose-500/50">
                <span className="text-4xl">❤️</span>
              </div>
            </div>
            
            <div className="text-center space-y-2">
              <h3 className="font-extrabold text-slate-100 text-lg">Melukis Agent Impian Anda...</h3>
              <p className="text-xs text-rose-300 italic font-semibold">{loadingMessage}</p>
            </div>

            <div className="w-full space-y-1.5">
              <div className="h-2 bg-slate-800 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-purple-500 via-pink-500 to-rose-500 transition-all duration-150"
                  style={{ width: `${progress}%` }}
                ></div>
              </div>
              <div className="flex justify-between text-[10px] text-slate-400 font-bold">
                <span>MEMASANG DNA PERSONA</span>
                <span>{progress}%</span>
              </div>
            </div>
          </div>
        )}

        {/* COMPLETED STATE (CONGRATULATIONS BORN SCREEN) */}
        {completed && (
          <div className="flex-1 flex flex-col overflow-hidden">
            <h2 className="text-center text-xs font-extrabold tracking-widest text-purple-400 uppercase">CONGRATULATIONS! 🎉</h2>
            <h3 className="text-center font-black text-slate-100 text-base mt-1">Your Dream Companion is Born</h3>

            <div className="flex-1 overflow-y-auto my-4 space-y-4 pr-1 scrollbar-thin">
              {/* Profile Image Preview Card */}
              <div className="bg-slate-950 border border-slate-800 rounded-2xl overflow-hidden p-3 flex flex-col items-center text-center">
                <div className="w-24 h-24 rounded-full bg-gradient-to-tr from-purple-600 via-pink-500 to-rose-400 p-1 mb-2 shadow-lg">
                  <div className="w-full h-full bg-slate-900 rounded-full flex items-center justify-center text-3xl">
                    👰‍♀️
                  </div>
                </div>
                <h4 className="font-extrabold text-sm text-slate-100">{name}, {age}</h4>
                <p className="text-[10px] text-purple-400 font-bold uppercase tracking-wider">{relationship}</p>
              </div>

              {/* Attributes Checklist */}
              <div className="bg-slate-950/40 border border-slate-850 rounded-2xl p-4 text-xs text-slate-300 space-y-2 leading-relaxed">
                <p>🔹 <strong>Gaya Tampilan:</strong> {style}</p>
                <p>🔹 <strong>Etnis & Kulit:</strong> {ethnicity} ({skinTone})</p>
                <p>🔹 <strong>Gaya Rambut:</strong> {hairStyle} {hairColor}</p>
                <p>🔹 <strong>Proporsi Dada:</strong> {breastSize}</p>
                <p>🔹 <strong>Proporsi Pinggul:</strong> {buttSize}</p>
                <p>🔹 <strong>Pekerjaan:</strong> {occupation}</p>
                <p>🔹 <strong>Hobi Utama:</strong> {mainHobby}</p>
                <p>🔹 <strong>Fetish Dominan:</strong> {fetish}</p>
              </div>
            </div>

            <button
              onClick={handleBringToLife}
              className="w-full bg-gradient-to-r from-purple-600 via-pink-600 to-rose-500 hover:from-purple-500 hover:to-rose-400 text-white font-extrabold text-sm py-3.5 rounded-xl border border-purple-400/20 shadow-lg active:scale-95 transition-all flex items-center justify-center gap-1.5"
            >
              Bring Your AI to Life ❤️
            </button>
          </div>
        )}

        {/* ACTIVE WIZARD STEPS */}
        {!loading && !completed && (
          <div className="flex-1 flex flex-col overflow-hidden">
            
            {/* PROGRESS HEADER */}
            <div className="flex items-center justify-between mb-4 border-b border-slate-800 pb-3">
              <h3 className="font-extrabold text-sm text-slate-100 tracking-wide">Dreaming Avatar</h3>
              <span className="bg-purple-950 text-purple-400 text-[9px] px-2 py-0.5 rounded-full font-bold">
                Langkah {step}/5
              </span>
            </div>

            {/* STEP CONTROLLER CONTAINER */}
            <div className="flex-1 overflow-y-auto space-y-4 pr-1 scrollbar-thin text-xs text-slate-300 leading-relaxed">
              
              {/* --- STEP 1: STYLE & GENDER --- */}
              {step === 1 && (
                <div className="space-y-4 animate-slideIn">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">GENDER</label>
                    <div className="grid grid-cols-3 gap-2">
                      {["Female", "Male", "Trans"].map((gen) => (
                        <button
                          key={gen}
                          onClick={() => setGender(gen as any)}
                          className={`py-2 rounded-xl text-xs font-bold border transition-all ${
                            gender === gen
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {gen}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">STYLE (GAYA ARTISTIK)</label>
                    <div className="grid grid-cols-2 gap-3">
                      {[
                        { key: "Realistic", img: "📸", desc: "Karakter Foto Realistis" },
                        { key: "Anime", img: "🎨", desc: "Gaya Ilustrasi Anime/Manga" }
                      ].map((item) => (
                        <button
                          key={item.key}
                          onClick={() => setStyle(item.key as any)}
                          className={`p-4 rounded-2xl border transition-all flex flex-col items-center text-center space-y-2 ${
                            style === item.key
                              ? "bg-purple-500/15 border-purple-400 text-purple-300 shadow-lg shadow-purple-500/5"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          <span className="text-3xl">{item.img}</span>
                          <div>
                            <p className="font-extrabold text-[11px]">{item.key}</p>
                            <p className="text-[9px] text-slate-400 leading-tight mt-0.5">{item.desc}</p>
                          </div>
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* --- STEP 2: ETHNICITY & SKIN TONE --- */}
              {step === 2 && (
                <div className="space-y-4 animate-slideIn">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">ETHNICITY (ETNIS)</label>
                    <div className="grid grid-cols-3 gap-1.5">
                      {["Asian", "White", "Black", "Latina", "Arab", "Indian", "Japanese", "Elf", "Alien", "Demon", "Angel", "Custom"].map((eth) => (
                        <button
                          key={eth}
                          onClick={() => setEthnicity(eth)}
                          className={`py-2 rounded-xl text-[10px] font-bold border transition-all truncate px-1 ${
                            ethnicity === eth
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {eth}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">SKIN TONE (WARNA KULIT)</label>
                    <div className="flex gap-2 justify-between">
                      {[
                        { key: "Fair", color: "#FFE8D6" },
                        { key: "Olive", color: "#E0B084" },
                        { key: "Tan", color: "#B88458" },
                        { key: "Sunkissed", color: "#A47444" },
                        { key: "Dark", color: "#6C401C" }
                      ].map((skin) => (
                        <button
                          key={skin.key}
                          onClick={() => setSkinTone(skin.key)}
                          className={`flex-1 flex flex-col items-center gap-1.5 p-1 rounded-xl transition-all border ${
                            skinTone === skin.key ? "border-purple-500/60 bg-purple-500/5 text-purple-300" : "border-transparent text-slate-500"
                          }`}
                        >
                          <div 
                            className="w-8 h-8 rounded-full border border-slate-800 shadow"
                            style={{ backgroundColor: skin.color }}
                          ></div>
                          <span className="text-[9px] font-bold leading-none">{skin.key}</span>
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* --- STEP 3: HEAD DETAILS (EYES & HAIR) --- */}
              {step === 3 && (
                <div className="space-y-4 animate-slideIn">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">EYE COLOR (WARNA MATA)</label>
                    <div className="grid grid-cols-5 gap-1">
                      {["Brown", "Blue", "Green", "Amber", "Purple"].map((eye) => (
                        <button
                          key={eye}
                          onClick={() => setEyeColor(eye)}
                          className={`py-1.5 rounded-lg text-[9px] font-bold border transition-all ${
                            eyeColor === eye
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {eye}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">HAIR COLOR (WARNA RAMBUT)</label>
                    <div className="grid grid-cols-4 gap-1">
                      {["Brunette", "Raven Black", "Platinum Blonde", "Red", "Pink", "Blue", "Purple", "Silver/White"].map((hair) => (
                        <button
                          key={hair}
                          onClick={() => setHairColor(hair)}
                          className={`py-1.5 rounded-lg text-[9px] font-bold border transition-all truncate px-1 ${
                            hairColor === hair
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {hair.replace("Black", "")}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">HAIR STYLE (GAYA RAMBUT)</label>
                    <div className="grid grid-cols-3 gap-1.5">
                      {["Long Wavy", "Straight Bangs", "High Ponytail", "Messy Bun", "Short Pixie", "Twin Braids"].map((hstyle) => (
                        <button
                          key={hstyle}
                          onClick={() => setHairStyle(hstyle)}
                          className={`py-2 rounded-xl text-[9px] font-bold border transition-all leading-tight ${
                            hairStyle === hstyle
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {hstyle}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* --- STEP 4: BODY TYPE & PROPORTIONS --- */}
              {step === 4 && (
                <div className="space-y-4 animate-slideIn">
                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">BODY TYPE (TIPE TUBUH)</label>
                    <div className="grid grid-cols-3 gap-1.5">
                      {["Slim", "Athletic", "Voluptuous", "Chubby", "Petite", "Tall"].map((btype) => (
                        <button
                          key={btype}
                          onClick={() => setBodyType(btype)}
                          className={`py-2.5 rounded-xl text-[10px] font-bold border transition-all ${
                            bodyType === btype
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {btype}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">BREAST SIZE (UKURAN DADA)</label>
                    <div className="grid grid-cols-5 gap-1">
                      {["Flat", "Small", "Medium", "Large", "XL"].map((bsize) => (
                        <button
                          key={bsize}
                          onClick={() => setBreastSize(bsize)}
                          className={`py-1.5 rounded-lg text-[9px] font-bold border transition-all ${
                            breastSize === bsize
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {bsize}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">BUTT SIZE (PROPORSI PINGGUL)</label>
                    <div className="grid grid-cols-5 gap-1">
                      {["Skinny", "Athletic", "Medium", "Large", "XL"].map((buttsize) => (
                        <button
                          key={buttsize}
                          onClick={() => setButtSize(buttsize)}
                          className={`py-1.5 rounded-lg text-[9px] font-bold border transition-all truncate px-0.5 ${
                            buttSize === buttsize
                              ? "bg-purple-500/20 border-purple-400 text-purple-300"
                              : "bg-slate-950 border-slate-850 text-slate-500"
                          }`}
                        >
                          {buttsize}
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* --- STEP 5: PERSONALITY & FORM --- */}
              {step === 5 && (
                <div className="space-y-3.5 animate-slideIn">
                  <div className="space-y-1">
                    <label className="text-[10px] font-bold text-slate-400 block">NAMA AGENT</label>
                    <input
                      type="text"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="Masukkan nama agent..."
                      className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                    />
                  </div>

                  <div className="flex gap-3">
                    <div className="flex-1 space-y-1">
                      <label className="text-[10px] font-bold text-slate-400 block">AGE (UMUR)</label>
                      <div className="flex items-center gap-2 bg-slate-950 border border-slate-800 rounded-xl px-3 py-1.5">
                        <button onClick={() => setAge(Math.max(18, age - 1))} className="text-purple-400 font-bold p-1">◀</button>
                        <span className="flex-1 text-center font-bold text-xs text-slate-100">{age} Tahun</span>
                        <button onClick={() => setAge(Math.min(99, age + 1))} className="text-purple-400 font-bold p-1">▶</button>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">DREAMING PERSONA CHIPS</label>
                    <div className="grid grid-cols-2 gap-1.5">
                      <div className="bg-slate-950 border border-slate-850 rounded-xl p-2 flex flex-col justify-between">
                        <span className="text-[8px] text-slate-500 font-bold">PERSONALITY</span>
                        <select value={personality} onChange={(e) => setPersonality(e.target.value)} className="bg-transparent border-none text-[11px] font-bold text-purple-300 focus:outline-none w-full p-0">
                          {["Sweet 😊", "Witty ⚡", "Savage 🔥", "Shy 🌸", "Tsundere 💢", "Uncensored 🔞"].map((opt) => (
                            <option key={opt} value={opt} className="bg-slate-900 text-slate-100">{opt}</option>
                          ))}
                        </select>
                      </div>

                      <div className="bg-slate-950 border border-slate-850 rounded-xl p-2 flex flex-col justify-between">
                        <span className="text-[8px] text-slate-500 font-bold">HUBUNGAN</span>
                        <select value={relationship} onChange={(e) => setRelationship(e.target.value)} className="bg-transparent border-none text-[11px] font-bold text-purple-300 focus:outline-none w-full p-0">
                          {["Lover 💖", "Bestie 🤝", "Wife 💍", "Maid 🧹", "Secret Assistant 🕵️‍♀️"].map((opt) => (
                            <option key={opt} value={opt} className="bg-slate-900 text-slate-100">{opt}</option>
                          ))}
                        </select>
                      </div>

                      <div className="bg-slate-950 border border-slate-850 rounded-xl p-2 flex flex-col justify-between">
                        <span className="text-[8px] text-slate-500 font-bold">PEKERJAAN</span>
                        <select value={occupation} onChange={(e) => setOccupation(e.target.value)} className="bg-transparent border-none text-[11px] font-bold text-purple-300 focus:outline-none w-full p-0">
                          {["Gaming Streamer 🎮", "College Student 🎓", "Novelist ✍️", "Secret Agent 🕵️", "Maid 🧹"].map((opt) => (
                            <option key={opt} value={opt} className="bg-slate-900 text-slate-100">{opt}</option>
                          ))}
                        </select>
                      </div>

                      <div className="bg-slate-950 border border-slate-850 rounded-xl p-2 flex flex-col justify-between">
                        <span className="text-[8px] text-slate-500 font-bold">HOBI UTAMA</span>
                        <select value={mainHobby} onChange={(e) => setMainHobby(e.target.value)} className="bg-transparent border-none text-[11px] font-bold text-purple-300 focus:outline-none w-full p-0">
                          {["Cosplay 🎭", "Gaming 🎮", "Coding 💻", "Singing 🎤", "Baking 🧁"].map((opt) => (
                            <option key={opt} value={opt} className="bg-slate-900 text-slate-100">{opt}</option>
                          ))}
                        </select>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <label className="text-[10px] font-bold text-slate-400 block">KALIMAT PEMBUKA PERTAMA (OPSIONAL)</label>
                    <textarea
                      value={firstGreeting}
                      onChange={(e) => setFirstGreeting(e.target.value)}
                      placeholder="Hai sayang... akhirnya kita bisa bersama seutuhnya."
                      rows={1.5}
                      className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl p-2 text-xs text-slate-200 focus:outline-none resize-none"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-[10px] font-bold text-slate-400 block">SIFAT BEBAS TANPA BATAS (CUSTOM PROMPT)</label>
                    <textarea
                      value={customPrompt}
                      onChange={(e) => setCustomPrompt(e.target.value)}
                      placeholder="Tulis sifat mendetail atau rules khusus karakter di sini..."
                      rows={1.5}
                      className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500/50 rounded-xl p-2 text-xs text-slate-200 focus:outline-none resize-none"
                    />
                  </div>
                </div>
              )}

            </div>

            {/* NAV FOOTER BUTTONS */}
            <div className="flex justify-between items-center mt-5 pt-3 border-t border-slate-800 gap-3">
              {step > 1 ? (
                <button
                  onClick={() => setStep(step - 1)}
                  className="bg-slate-850 hover:bg-slate-800 border border-slate-800 text-slate-300 font-bold text-xs px-4 py-2.5 rounded-xl transition-all"
                >
                  Kembali
                </button>
              ) : (
                <div />
              )}

              {step < 5 ? (
                <button
                  onClick={() => setStep(step + 1)}
                  className="flex-1 bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs py-2.5 rounded-xl border border-purple-400/20 shadow-lg transition-all text-center"
                >
                  Lanjut
                </button>
              ) : (
                <button
                  onClick={handleStartCompile}
                  className="flex-1 bg-gradient-to-r from-purple-600 via-pink-600 to-rose-500 hover:from-purple-500 hover:to-rose-400 text-white font-extrabold text-xs py-2.5 rounded-xl border border-purple-400/20 shadow-lg transition-all"
                >
                  Mulai Paint & Compile
                </button>
              )}
            </div>

          </div>
        )}

      </div>
    </div>
  );
}
