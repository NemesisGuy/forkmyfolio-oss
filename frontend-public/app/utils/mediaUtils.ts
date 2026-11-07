/**
 * @file app/utils/mediaUtils.ts
 * @description Shared utilities for handling rich media (YouTube, Vimeo, Images).
 * Ported from ForkMyFolio-frontend-vue/src/utils/mediaUtils.js.
 * Nuxt auto-imports functions from `app/utils`, no manual import needed.
 */

export type MediaType = 'youtube' | 'vimeo' | 'video' | 'image' | 'none';

/**
 * Detects the type of media from a URL.
 * @param url - The media URL.
 * @returns The detected media type.
 */
export const getMediaType = (url?: string | null): MediaType => {
  if (!url) return 'none';
  const lowerUrl = url.toLowerCase();
  if (lowerUrl.includes('youtube.com') || lowerUrl.includes('youtu.be')) return 'youtube';
  if (lowerUrl.includes('vimeo.com')) return 'vimeo';
  if (lowerUrl.match(/\.(mp4|webm|ogg)$/)) return 'video';
  if (lowerUrl.match(/\.(gif|jpg|jpeg|png|webp)$/)) return 'image';
  return 'none';
};

/**
 * Generates a thumbnail URL for the given media.
 * @param url - The media URL.
 * @returns The thumbnail URL or null if not supported.
 */
export const getThumbnailUrl = (url?: string | null): string | null => {
  const type = getMediaType(url);

  if (type === 'youtube' && url) {
    // Extract ID
    const vMatch = url.match(/(?:v=|\/)([a-zA-Z0-9_-]{11})/);
    if (vMatch) {
      // mqdefault is a standard medium quality thumbnail (320x180)
      return `https://img.youtube.com/vi/${vMatch[1]}/mqdefault.jpg`;
    }
  }

  // For standard images/GIFs, the URL itself is the thumbnail
  if (type === 'image' && url) {
    return url;
  }

  // Vimeo requires an API call for thumbnails, so we return null here
  // and let the component handle the fallback.
  return null;
};

/**
 * Generates an embeddable URL for iframes.
 * @param url - The media URL.
 * @param autoplay - Whether to enable autoplay.
 * @returns The embeddable URL.
 */
export const getEmbedUrl = (url: string, autoplay = false): string => {
  const type = getMediaType(url);

  if (type === 'youtube') {
    const vMatch = url.match(/(?:v=|\/)([a-zA-Z0-9_-]{11})/);
    if (vMatch) {
      const baseUrl = `https://www.youtube.com/embed/${vMatch[1]}`;
      // Autoplay on YouTube requires mute=1 usually for modern browsers
      return autoplay ? `${baseUrl}?autoplay=1&mute=1&rel=0` : `${baseUrl}?rel=0`;
    }
  }

  if (type === 'vimeo') {
    // Regex to extract Vimeo ID from various URL formats
    const vMatch = url.match(/(?:vimeo\.com\/|player\.vimeo\.com\/video\/)([0-9]+)/);
    if (vMatch) {
      const baseUrl = `https://player.vimeo.com/video/${vMatch[1]}`;
      return autoplay ? `${baseUrl}?autoplay=1&muted=1` : baseUrl;
    }
  }

  return url;
};
