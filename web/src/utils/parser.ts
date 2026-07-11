export interface ParsedResponse {
  narrative: string;
  options: string[];
  trackerRaw: string;
}

export function parseDreamPlayResponse(rawText: string): ParsedResponse {
  let narrative = rawText;
  let options: string[] = [];
  let trackerRaw = "";

  // Normalisasi line endings ke \n untuk mempermudah parsing regex
  const normalizedText = rawText.replace(/\r\n/g, "\n");

  // 1. Cari & Ekstrak START_TRACKER
  const trackerMarker = "===START_TRACKER===";
  const trackerIndex = normalizedText.indexOf(trackerMarker);
  
  if (trackerIndex !== -1) {
    trackerRaw = normalizedText.substring(trackerIndex + trackerMarker.length).trim();
    narrative = normalizedText.substring(0, trackerIndex);
  }

  // 2. Cari & Ekstrak NEXT_OPTION
  const optionMarker = "===NEXT_OPTION===";
  const optionIndex = narrative.indexOf(optionMarker);
  
  if (optionIndex !== -1) {
    const optionsBlock = narrative.substring(optionIndex + optionMarker.length).trim();
    narrative = narrative.substring(0, optionIndex).trim();

    // Pisahkan berdasarkan baris dan cari format seperti "1. Pilihan"
    options = optionsBlock
      .split("\n")
      .map(line => line.trim())
      .filter(line => line.length > 0)
      // Filter out baris instruksi seperti "Pilihan Lanjutan Cerita:" jika ada
      .filter(line => /^\d+\./.test(line) || line.startsWith("-"))
      .map(line => line.replace(/^(\d+\.\s*|-\s*)/, ""));
  }

  return {
    narrative: narrative.trim(),
    options,
    trackerRaw,
  };
}
