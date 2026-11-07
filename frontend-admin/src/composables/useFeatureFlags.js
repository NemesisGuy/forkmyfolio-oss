import {computed, ref} from 'vue';

// Define the default state of all feature flags
const getDefaultFlags = () => ([
  {
    name: 'SHOW_PROJECTS',
    enabled: true,
    description: 'Display the "Projects" section on the public site.'
  },
  {
    name: 'SHOW_SKILLS',
    enabled: true,
    description: 'Display the "Skills" section on the public site.'
  },
  {
    name: 'SHOW_EXPERIENCE',
    enabled: true,
    description: 'Display the "Experience" section on the public site.'
  },
  {
    name: 'SHOW_TESTIMONIALS',
    enabled: true,
    description: 'Display the "Testimonials" section on the public site.'
  },
  {
    name: 'SHOW_QUALIFICATIONS',
    enabled: true,
    description: 'Display the "Qualifications" section on the public site.'
  },
  {
    name: 'SHOW_CONTACT_FORM',
    enabled: true,
    description: 'Display the "Contact Me" section on the public site.'
  },
]);

// Helper to load flags from localStorage on startup
const loadFlagsFromStorage = () => {
  try {
    const storedFlags = localStorage.getItem('featureFlags');
    if (storedFlags) {
      const parsed = JSON.parse(storedFlags);
      // Basic validation to ensure we're loading valid data
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed;
      }
    }
  } catch (e) {
    console.error('Failed to parse feature flags from localStorage', e);
  }
  // Return defaults if storage is empty, invalid, or fails to parse
  return getDefaultFlags();
};

// This state is created only once because it's at the top level of the module.
// All calls to useFeatureFlags() will share this same reactive `flags` ref.
const flags = ref(loadFlagsFromStorage());

/**
 * A Vue composable for managing site-wide feature flags.
 * State is reactive and persisted to localStorage.
 */
export function useFeatureFlags() {

  const saveFlags = (newFlags) => {
    flags.value = newFlags;
    localStorage.setItem('featureFlags', JSON.stringify(newFlags));
  };

  const isFeatureEnabled = computed(() => {
    // This getter returns a function for easy use in templates, e.g., isFeatureEnabled('SHOW_PROJECTS')
    return (featureName) => flags.value.find(f => f.name === featureName)?.enabled ?? false;
  });

  return {flags, saveFlags, isFeatureEnabled};
}
