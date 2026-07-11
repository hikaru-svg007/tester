import { NextResponse } from "next/server";

// Ambil list kunci dari env (dipisahkan koma)
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
        signal: AbortSignal.timeout(15000), // Timeout 15s untuk failover cepat
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
      return response;
    }

    return response;
  } catch (error: any) {
    console.error(`[Failover Engine] Kesalahan jaringan pada percobaan #${attempt + 1}:`, error.message);
    
    if (attempt < geminiKeys.length - 1) {
      return fetchGeminiWithFailover(payload, attempt + 1);
    }
    throw error;
  }
}

export async function POST(req: Request) {
  try {
    const { messages, systemInstruction } = await req.json();

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
