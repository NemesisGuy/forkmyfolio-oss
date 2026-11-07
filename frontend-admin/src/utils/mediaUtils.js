/**
 * @file src/utils/mediaUtils.js
 * @description Shared utilities for handling rich media (YouTube, Vimeo, Images)
 */

/**
 * Detects the type of media from a URL.
 * @param {string} url - The media URL.
 * @returns {'youtube'|'vimeo'|'video'|'image'|'none'} The detected media type.
 */
export const getMediaType = (url) => {
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
 * @param {string} url - The media URL.
 * @returns {string|null} The thumbnail URL or null if not supported.
 */
export const getThumbnailUrl = (url) => {
  const type = getMediaType(url);

  if (type === 'youtube') {
    // Extract ID
    const vMatch = url.match(/(?:v=|\/)([a-zA-Z0-9_-]{11})/);
    if (vMatch) {
      // mqdefault is a standard medium quality thumbnail (320x180)
      return `https://img.youtube.com/vi/${vMatch[1]}/mqdefault.jpg`;
    }
  }

  // For standard images/GIFs, the URL itself is the thumbnail
  if (type === 'image') {
    return url;
  }

  // Vimeo requires an API call for thumbnails, so we return null here
  // and let the component handle the fallback.
  return null;
};

/**
 * Generates an embeddable URL for iframes.
 * @param {string} url - The media URL.
 * @param {boolean} autoplay - Whether to enable autoplay.
 * @returns {string} The embeddable URL.
 */
export const getEmbedUrl = (url, autoplay = false) => {
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
