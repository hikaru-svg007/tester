import { NextResponse } from "next/server";

// Ambil list kunci dari env (dipisahkan koma)
const geminiKeys = (process.env.GEMINI_API_KEYS || "").split(",").filter(Boolean);

/**
 * Melakukan pemanggilan ke Gemini API dengan rotasi kunci otomatis jika gagal (failover)
 */
async function fetchGeminiWithFailover(payload: any, customApiKey?: string, selectedModel: string = "gemini-1.5-pro", attempt: number = 0): Promise<Response> {
  const activeKey = customApiKey || (geminiKeys.length > 0 ? geminiKeys[attempt % geminiKeys.length] : undefined);
  
  if (!activeKey) {
    throw new Error("Tidak ada API Key yang dikonfigurasi. Silakan masukkan API Key Anda di menu Setelan.");
  }

  if (customApiKey) {
    console.log("[Failover Engine] Menggunakan API Key kustom dari Setelan.");
  } else {
    console.log(`[Failover Engine] Percobaan #${attempt + 1} menggunakan API Key index: ${attempt % geminiKeys.length}`);
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000);

  // Map model names to actual Gemini API endpoint models
  let modelEndpoint = selectedModel;
  if (selectedModel.includes("flash")) {
    modelEndpoint = "gemini-1.5-flash";
  } else if (selectedModel.includes("pro")) {
    modelEndpoint = "gemini-1.5-pro";
  }

  try {
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${modelEndpoint}:generateContent?key=${activeKey}`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
        signal: controller.signal,
      }
    );

    // Jika menggunakan custom key, langsung return response apa adanya (baik sukses maupun error)
    if (customApiKey || response.ok) {
      return response;
    }

    // Jika terkena rate limit (429) atau error server internal (5xx), dan masih ada key lain
    if ((response.status === 429 || response.status >= 500) && geminiKeys.length > 0 && attempt < geminiKeys.length - 1) {
      console.warn(`[Failover Engine] Key index ${attempt % geminiKeys.length} mengembalikan status ${response.status}. Mencoba key berikutnya...`);
      return fetchGeminiWithFailover(payload, undefined, selectedModel, attempt + 1);
    }

    // Jika response tidak oke tapi sudah kehabisan key cadangan
    if (!response.ok) {
      const errorText = await response.text();
      console.error(`[Failover Engine] Error setelah semua percobaan:`, errorText);
      return response;
    }

    return response;
  } catch (error: any) {
    console.error(`[Failover Engine] Kesalahan jaringan:`, error.message);
    
    if (!customApiKey && geminiKeys.length > 0 && attempt < geminiKeys.length - 1) {
      return fetchGeminiWithFailover(payload, undefined, selectedModel, attempt + 1);
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

export async function POST(req: Request) {
  try {
    const { messages, systemInstruction, customApiKey, model } = await req.json();

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

    const response = await fetchGeminiWithFailover(payload, customApiKey, model || "gemini-1.5-pro");

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
