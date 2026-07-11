export interface ColorTheme {
  dialogueColor: string;       // 1. Warna Dialog ("Dialog Karakter")
  thoughtsColor: string;       // 2. Warna Kata Batin ("Kata Batin")
  narrativeColor: string;      // 3. Warna Narasi Novel ("Narasi Cerita")
  aiBubbleBg: string;          // 4. Background Balasan AI
  userBubbleBg: string;        // 5. Background Balasan User
  mainChatBg: string;          // 6. Background Ruangan Chat Utama
}

export const DEFAULT_THEMES: Record<string, ColorTheme> = {
  pink_lavender: {
    dialogueColor: "#FFFF79C6",
    thoughtsColor: "#FFFF8DAE",
    narrativeColor: "#FFE2E8F0",
    aiBubbleBg: "#FF1E1B4B",
    userBubbleBg: "#FF6B21A8",
    mainChatBg: "#FF090D16"
  },
  cosmic_green: {
    dialogueColor: "#FF50FA7B",
    thoughtsColor: "#FF8BE9FD",
    narrativeColor: "#FFF8F8F2",
    aiBubbleBg: "#FF112211",
    userBubbleBg: "#FF00AA55",
    mainChatBg: "#FF0A0F0D"
  },
  ocean_breeze: {
    dialogueColor: "#FF8BE9FD",
    thoughtsColor: "#FF50FA7B",
    narrativeColor: "#FFF8F8F2",
    aiBubbleBg: "#FF0B253A",
    userBubbleBg: "#FF1E88E5",
    mainChatBg: "#FF05101A"
  },
  obsidian_gold: {
    dialogueColor: "#FFF1FA8C",
    thoughtsColor: "#FFFF79C6",
    narrativeColor: "#FFF8F8F2",
    aiBubbleBg: "#FF2A1B05",
    userBubbleBg: "#FFFFB86C",
    mainChatBg: "#FF110B05"
  }
};
