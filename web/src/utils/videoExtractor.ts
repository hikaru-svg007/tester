// Video Link Extractor utility mirroring Android's VideoLinkExtractor.kt
// Resolves Dropbox, Google Drive, and direct stream links to be played in HTML5 player.

export function extractGoogleDriveFileId(url: string): string | null {
  const patterns = [
    /\/file\/d\/([a-zA-Z0-9-_]{25,})/,
    /id=([a-zA-Z0-9-_]{25,})/,
    /\/d\/([a-zA-Z0-9-_]{25,})/
  ];
  for (const pat of patterns) {
    const match = url.match(pat);
    if (match && match[1]) {
      return match[1];
    }
  }
  return null;
}

export function resolveDirectVideoUrl(url: string): string {
  if (!url) return "";
  const trimmed = url.trim();

  // 1. Google Drive Link Extraction
  if (trimmed.includes("drive.google.com") || trimmed.includes("docs.google.com")) {
    const fileId = extractGoogleDriveFileId(trimmed);
    if (fileId) {
      return `https://docs.google.com/uc?export=download&id=${fileId}`;
    }
  }

  // 2. Dropbox Link Extraction
  if (trimmed.includes("dropbox.com")) {
    let directUrl = trimmed;
    if (directUrl.includes("www.dropbox.com")) {
      directUrl = directUrl.replace("www.dropbox.com", "dl.dropboxusercontent.com");
    }
    if (directUrl.includes("dl=0")) {
      directUrl = directUrl.replace("dl=0", "dl=1");
    } else if (!directUrl.includes("dl=1")) {
      directUrl = directUrl.includes("?") ? `${directUrl}&dl=1` : `${directUrl}?dl=1`;
    }
    return directUrl;
  }

  // 3. Fallback: Return original URL
  return trimmed;
}

/**
 * Searches text for any links that might represent streamable video files.
 */
export function extractVideoUrlsFromText(text: string): string[] {
  if (!text) return [];
  const urlRegex = /(https?:\/\/[^\s]+)/gi;
  const matches = text.match(urlRegex) || [];
  
  return matches.filter(url => {
    const lower = url.toLowerCase();
    return (
      lower.includes(".mp4") ||
      lower.includes(".mov") ||
      lower.includes(".mkv") ||
      lower.includes(".webm") ||
      lower.includes(".3gp") ||
      lower.includes(".m3u8") ||
      lower.includes("drive.google.com") ||
      lower.includes("docs.google.com") ||
      lower.includes("dropbox.com")
    );
  }).map(url => {
    // Strip trailing punctuation like ), ., ,, etc. commonly matched by lazy regexes
    return url.replace(/[).,;:]+$/, "");
  });
}
