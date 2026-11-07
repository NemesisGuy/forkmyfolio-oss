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

      <LoadingModal :visible="isLoading"/>

      <div v-if="isLoading"
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
    </div>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from 'vue';
import {usePublicPortfolioStore} from '@/stores/publicPortfolioStore.js';
import {notificationService} from '@/services/notificationService.js';
import {ApiError, sendContactMessage} from '@/services/api/index.js';
import LoadingModal from '@/components/common/modals/LoadingModal.vue';
import { Mail, User, AlertCircle, MessageCircle, Send, Clock, ShieldCheck, CalendarCheck } from 'lucide-vue-next';

const {currentSlug, isLoading} = usePublicPortfolioStore();

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

const handleSubmit = async () => {
  if (!validateForm()) {
    return;
  }

  isSubmitting.value = true;
  try {
    if (!currentSlug.value) throw new Error("Portfolio not found");
    await sendContactMessage(currentSlug.value, {...form});
    notificationService.add({
      type: 'success',
      message: 'Thank you for your message! I will get back to you shortly.'
    });
    resetForm();
  } catch (err) {
    console.error("Failed to send message:", err);
    notificationService.add({
      type: 'error',
      message: err instanceof ApiError ? err.message : 'Could not send the message. Please try again later.'
    });
  } finally {
    isSubmitting.value = false;
  }
};
</script>

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
</style>
