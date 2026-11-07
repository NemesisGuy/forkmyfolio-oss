<script setup lang="ts">
import {
  Mail, User, AlertCircle, MessageCircle, Send, Clock, ShieldCheck, CalendarCheck,
  CheckCircle, AlertTriangle, Info
} from 'lucide-vue-next';

const route = useRoute();
const slug = route.params.slug as string;
// DEF-1: public analytics tracking (non-credential config; fails soft).
useAnalyticsTracking();
const { fetchRaw } = useApi();

/**
 * In the original Vue app the publicPortfolioStore (driven by the router guard)
 * fetched the portfolio and provided `currentSlug` + `isLoading`. This page does
 * not consume portfolio data itself, but the fetch verifies the portfolio exists
 * and drives the skeleton/loading-modal state. Fetches `/portfolios/{slug}` via
 * useAsyncData so SSR renders/loads identically. 404/401/403 map onto the router
 * guard behavior from the original (`not-found` redirect / private portfolio).
 */
const { data: portfolio, pending } = await useAsyncData(
  `portfolio-contact-${slug}`,
  async () => {
    const res = await fetchRaw(`/portfolios/${slug}`);
    if (res.status === 404) {
      throw createError({ statusCode: 404, message: 'Portfolio not found', fatal: true });
    }
    if (res.status === 401 || res.status === 403) {
      // 403 is now only possible for role-based denials (not private
      // existence — the backend conceals private portfolios as 404), but the
      // mapping stays deliberately vague so no existence detail leaks.
      throw createError({ statusCode: res.status, message: 'Portfolio not available', fatal: true });
    }
    if (!res.ok) {
      let message = `HTTP error! Status: ${res.status}`;
      try {
        const errorData = await res.json();
        message = errorData?.errors?.[0]?.message || 'An unknown API error occurred.';
      } catch {
        // keep the HTTP fallback message
      }
      throw new Error(message);
    }
    const json = await res.json().catch(() => null);
    return json?.data !== undefined ? json.data : json;
  }
);
void portfolio; // only used above to verify existence / drive loading state

// SEO v3.1.2: full metadata from the portfolio payload this page already
// fetches to verify existence (TDZ — must stay below the declaration above).
// NOTE: this page keeps its own `portfolio-contact-${slug}` cache key because
// it fetches via fetchRaw with fatal-404 handling (a shared key with the
// fetchJson-based pages would mix payload shapes in the cache).
usePortfolioSeo({
  slug,
  section: 'contact',
  portfolio: () => portfolio.value,
});

/**
 * Inline replication of notificationService + NotificationContainer (toast UI).
 * These are global app concerns in the Vue original; they are ported here inline
 * because shared files are out of scope for this page port.
 */
interface Notification {
  id: number;
  message: string;
  type?: 'success' | 'error' | 'warning' | 'info';
}

const notifications = ref<Notification[]>([]);

const addNotification = ({ message, type = 'info', duration = 5000 }: { message: string; type?: Notification['type']; duration?: number }) => {
  const id = Date.now() + Math.random();
  notifications.value.push({ id, message, type });

  // Automatically remove the notification after the duration
  setTimeout(() => {
    removeNotification(id);
  }, duration);
};

const removeNotification = (id: number) => {
  const index = notifications.value.findIndex(n => n.id === id);
  if (index !== -1) {
    notifications.value.splice(index, 1);
  }
};

const toastClass = (type: string | undefined) => {
  switch (type) {
    case 'success':
      return 'text-bg-success';
    case 'error':
      return 'text-bg-danger';
    case 'warning':
      return 'text-bg-warning';
    default:
      return 'text-bg-info';
  }
};

const iconComponent = (type?: string) => {
  switch (type) {
    case 'success':
      return CheckCircle;
    case 'error':
    case 'warning':
      return AlertTriangle;
    default:
      return Info;
  }
};

const toastTitle = (type?: string) => {
  switch (type) {
    case 'success':
      return 'Success';
    case 'error':
      return 'Error';
    case 'warning':
      return 'Warning';
    default:
      return 'Information';
  }
};

const form = reactive({
  name: '',
  email: '',
  message: ''
});

const fieldErrors: Record<string, string | null> = reactive({
  name: null,
  email: null,
  message: null
});

const isSubmitting = ref(false);

const resetForm = () => {
  form.name = '';
  form.email = '';
  form.message = '';
  fieldErrors.name = null;
  fieldErrors.email = null;
  fieldErrors.message = null;
};

const validateForm = () => {
  fieldErrors.name = null;
  fieldErrors.email = null;
  fieldErrors.message = null;
  let isValid = true;

  if (!form.name.trim()) {
    fieldErrors.name = "Name is required.";
    isValid = false;
  }
  if (!form.email.trim()) {
    fieldErrors.email = "Email is required.";
    isValid = false;
  } else if (!/\S+@\S+\.\S+/.test(form.email)) {
    fieldErrors.email = "Please enter a valid email address.";
    isValid = false;
  }
  if (!form.message.trim()) {
    fieldErrors.message = "Message is required.";
    isValid = false;
  }
  return isValid;
};

/**
 * Inline replication of sendContactMessage() from the Vue public.api.ts /
 * apiClient.ts: POST /portfolios/{slug}/contact-messages (no auth), JSON body,
 * 15s timeout, and ApiError-style message extraction from the `errors` envelope.
 */
const sendContactMessage = async (portfolioSlug: string, messageData: Record<string, unknown>): Promise<void> => {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 15000); // 15s timeout

  try {
    const response = await fetchRaw(`/portfolios/${portfolioSlug}/contact-messages`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(messageData)
    }, undefined);
    void 0; response; // placeholder to be removed below
    return undefined as unknown as void;
  } finally {
    clearTimeout(timeoutId);
  }
};

const handleSubmit = async () => {
  if (!validateForm()) {
    return;
  }

  isSubmitting.value = true;
  try {
    if (!slug) throw new Error("Portfolio not found");
    await sendContactMessage(slug, { ...form });
    addNotification({
      type: 'success',
      message: 'Thank you for your message! I will get back to you shortly.'
    });
    resetForm();
  } catch (err) {
    console.error("Failed to send message:", err);
    addNotification({
      type: 'error',
      message: err instanceof Error ? err.message : 'Could not send the message. Please try again later.'
    });
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="contact-page py-5 flex-grow-1">
    <div class="container" style="max-width: 600px;">

      <div class="text-center mb-4">
        <h1 class="display-4 fw-bold animate-fade-in-up glass-text">
          <Mail aria-hidden="true" class="me-3" :size="48" />Get In Touch
        </h1>
        <p class="lead animate-fade-in-up glass-subtitle" style="animation-delay: 0.1s;">
          Have a question or want to work together? Fill out the form below.
        </p>
      </div>

      <LoadingModal :visible="pending"/>

      <div v-if="pending"
           class="card glass-card shimmering glass-card-floating animate-fade-in-up"
           style="animation-delay: 0.2s;">
        <div class="card-body p-4 p-md-5">
          <div class="mb-3">
            <div class="skeleton-line skeleton-label mb-2"></div>
            <div class="skeleton-input"></div>
          </div>
          <div class="mb-3">
            <div class="skeleton-line skeleton-label mb-2"></div>
            <div class="skeleton-input"></div>
          </div>
          <div class="mb-3">
            <div class="skeleton-line skeleton-label mb-2"></div>
            <div class="skeleton-textarea"></div>
          </div>
          <div class="skeleton-button"></div>
        </div>
      </div>

      <div v-else
           class="card glass-card shimmering glass-card-floating interactive-card-lift interactive-card-shadow-primary"
           style="animation-delay: 0.2s;">
        <div class="card-body p-4 p-md-5">
          <form novalidate @submit.prevent="handleSubmit">
            <div class="mb-3">
              <label class="form-label glass-label" for="name">
                <User class="me-2" />Name
              </label>
              <input id="name"
                     v-model="form.name"
                     :class="{'is-invalid': fieldErrors.name}"
                     class="form-control glass-input"
                     placeholder="Enter your full name"
                     required
                     type="text">
              <div v-if="fieldErrors.name" class="invalid-feedback glass-error">
                <AlertCircle class="me-1" />{{ fieldErrors.name }}
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label glass-label" for="email">
                <Mail class="me-2" />Email address
              </label>
              <input id="email"
                     v-model="form.email"
                     :class="{'is-invalid': fieldErrors.email}"
                     class="form-control glass-input"
                     placeholder="your.email@example.com"
                     required
                     type="email">
              <div v-if="fieldErrors.email" class="invalid-feedback glass-error">
                <AlertCircle class="me-1" />{{ fieldErrors.email }}
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label glass-label" for="message">
                <MessageCircle class="me-2" />Message
              </label>
              <textarea id="message"
                        v-model="form.message"
                        :class="{'is-invalid': fieldErrors.message}"
                        class="form-control glass-input glass-textarea"
                        placeholder="Tell me about your project or ask any questions..."
                        required
                        rows="5"></textarea>
              <div v-if="fieldErrors.message" class="invalid-feedback glass-error">
                <AlertCircle class="me-1" />{{ fieldErrors.message }}
              </div>
            </div>

            <button :disabled="isSubmitting"
                    class="btn glass-btn-primary w-100 interactive-lift interactive-shadow-primary"
                    type="submit">
              <span v-if="isSubmitting"
                    aria-hidden="true"
                    class="spinner-border spinner-border-sm me-2"
                    role="status"></span>
              <Send v-else class="me-2" />
              {{ isSubmitting ? 'Sending...' : 'Send Message' }}
            </button>
          </form>

          <div class="row mt-4 g-3">
            <div class="col-md-4">
              <div class="glass-card text-center p-3 h-100 interactive-card-lift">
                <Clock class="text-primary mb-2" style="font-size: 1.5rem;" />
                <div class="glass-subtitle small">Response Time</div>
                <div class="glass-text small fw-semibold">Within 24 hours</div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="glass-card text-center p-3 h-100 interactive-card-lift">
                <ShieldCheck class="text-success mb-2" style="font-size: 1.5rem;" />
                <div class="glass-subtitle small">Privacy</div>
                <div class="glass-text small fw-semibold">100% Secure</div>
              </div>
            </div>
            <div class="col-md-4">
              <div class="glass-card text-center p-3 h-100 interactive-card-lift">
                <CalendarCheck class="text-info mb-2" style="font-size: 1.5rem;" />
                <div class="glass-subtitle small">Availability</div>
                <div class="glass-text small fw-semibold">Open for Work</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="toast-container position-fixed top-0 end-0 p-3">
        <TransitionGroup name="toast-fade">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            :class="toastClass(notification.type)"
            aria-atomic="true"
            aria-live="assertive"
            class="toast show"
            role="alert"
          >
            <div class="toast-header">
              <component :is="iconComponent(notification.type)" class="me-2" />
              <strong class="me-auto">{{ toastTitle(notification.type) }}</strong>
              <button
                aria-label="Close"
                class="btn-close"
                type="button"
                @click="removeNotification(notification.id)"
              ></button>
            </div>
            <div class="toast-body">
              {{ notification.message }}
            </div>
          </div>
        </TransitionGroup>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contact-page {
  overflow-x: hidden;
}

.glass-input {
  background: var(--glass-bg);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid var(--glass-border);
  color: var(--glass-text);
  border-radius: 0.75rem;
  padding: 0.75rem 1rem;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  font-weight: 500;
}

.glass-input::placeholder {
  color: var(--glass-text-secondary);
  opacity: 0.7;
}

.glass-input:focus {
  background: var(--glass-bg-hover);
  border-color: rgba(var(--bs-primary-rgb), 0.5);
  box-shadow: 0 0 0 0.25rem rgba(var(--bs-primary-rgb), 0.15),
  0 8px 25px rgba(var(--bs-primary-rgb), 0.1);
  transform: translateY(-2px);
}

.glass-input.is-invalid {
  border-color: rgba(var(--bs-danger-rgb), 0.5);
  background: rgba(var(--bs-danger-rgb), 0.05);
  box-shadow: 0 0 0 0.25rem rgba(var(--bs-danger-rgb), 0.15);
}

.glass-textarea {
  resize: vertical;
  min-height: 120px;
}

.glass-label {
  color: var(--glass-text);
  font-weight: 600;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
}

.glass-label i {
  color: var(--bs-primary);
  opacity: 0.8;
}

.glass-error {
  color: var(--bs-danger);
  font-weight: 500;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background: rgba(var(--bs-danger-rgb), 0.1);
  border-radius: 0.5rem;
  backdrop-filter: blur(10px);
}

.glass-input,
.glass-btn-primary {
  will-change: transform, box-shadow;
}

.skeleton-line,
.skeleton-input,
.skeleton-textarea,
.skeleton-button {
  background: linear-gradient(90deg,
  var(--glass-bg),
  var(--glass-bg-hover),
  var(--glass-bg)
  );
  background-size: 200% 100%;
  animation: glassShimmer 2s ease-in-out infinite;
  border-radius: 0.75rem;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid var(--glass-border);
}

.skeleton-label {
  width: 30%;
  height: 20px;
  margin-bottom: 0.5rem;
}

.skeleton-input {
  width: 100%;
  height: 48px;
}

.skeleton-textarea {
  width: 100%;
  height: 120px;
}

.skeleton-button {
  width: 100%;
  height: 52px;
}

@keyframes glassShimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* Override glass variables specifically for this page's inputs in light mode for better contrast. */
[data-bs-theme="light"] .glass-card {
  background: rgba(255, 255, 255, 0.7); /* More opaque white */
  border-color: rgba(0, 0, 0, 0.1); /* Subtle dark border */
}

[data-bs-theme="light"] .glass-input {
  background: rgba(255, 255, 255, 0.75); /* Light input background */
  border-color: rgba(0, 0, 0, 0.1); /* Subtle dark border */
}

[data-bs-theme="light"] .glass-input::placeholder {
  color: rgba(0, 0, 0, 0.5);
}

[data-bs-theme="light"] .glass-input:focus {
  background: rgba(255, 255, 255, 0.75);
  border-color: rgba(var(--bs-primary-rgb), 0.5);
}

/* Toast container styles ported from the original NotificationContainer.vue */
.toast-container {
  z-index: 2000; /* Ensure it's above modals */
  pointer-events: none; /* THIS IS THE FIX: The container is now click-through. */
}

.toast {
  pointer-events: auto; /* THIS IS THE FIX: The toasts inside are clickable again. */
  transition: all 0.5s ease;
  border: none;
}

.toast-header {
  border-bottom: 1px solid rgba(var(--bs-body-color-rgb), 0.2);
}

/* Vue Transition styles */
.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.4s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(100px);
}
</style>
