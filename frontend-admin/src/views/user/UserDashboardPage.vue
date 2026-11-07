<template>
  <div class="dashboard-page py-5 flex-grow-1">
    <div class="container">
      <template v-if="user">
        <div class="mb-5 text-center">
          <h1 class="display-4 fw-light glass-text animate-fade-in-up">Welcome,
            {{ user.firstName || 'User' }}!</h1>
          <p class="lead glass-subtitle animate-fade-in-up" style="animation-delay: 0.1s;">
            This is your personal dashboard. Manage your public portfolio from here.
          </p>
        </div>

        <div class="row g-4 justify-content-center">
          <!-- Profile & Account -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.2s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><IdCard class="me-2" />Profile & Account
                </h5>
                <p class="card-text small text-muted">Update your public profile and private account
                  details.</p>
                <router-link :to="{ name: 'profile', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage Profile
                </router-link>
              </div>
            </div>
          </div>

          <!-- Projects -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.3s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><KanbanSquare class="me-2" />Projects</h5>
                <p class="card-text small text-muted">Add, edit, and manage the visibility of your
                  projects.</p>
                <router-link :to="{ name: 'my-projects', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage Projects
                </router-link>
              </div>
            </div>
          </div>

          <!-- Job Applications -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.35s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><Briefcase class="me-2" />Job Applications</h5>
                <p class="card-text small text-muted">Track your job search, interviews, and application status.</p>
                <router-link :to="{ name: 'job-tracker', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Track Jobs
                </router-link>
              </div>
            </div>
          </div>

          <!-- Skills -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.4s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><Tags class="me-2" />Skills</h5>
                <p class="card-text small text-muted">Curate the list of skills and technologies you
                  want to showcase.</p>
                <router-link :to="{ name: 'my-skills', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage Skills
                </router-link>
              </div>
            </div>
          </div>

          <!-- Experience -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.5s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><Briefcase class="me-2" />Experience</h5>
                <p class="card-text small text-muted">Update your professional work history and job
                  descriptions.</p>
                <router-link :to="{ name: 'my-experience', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage Experience
                </router-link>
              </div>
            </div>
          </div>

          <!-- Qualifications -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.6s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><BadgeCheck class="me-2" />Qualifications</h5>
                <p class="card-text small text-muted">List your degrees, certifications, and other
                  qualifications.</p>
                <router-link :to="{ name: 'my-qualifications', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage Qualifications
                </router-link>
              </div>
            </div>
          </div>

          <!-- Testimonials -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.7s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><MessageSquareQuote class="me-2" />Testimonials</h5>
                <p class="card-text small text-muted">Manage the quotes and recommendations from
                  colleagues.</p>
                <router-link :to="{ name: 'my-testimonials', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage Testimonials
                </router-link>
              </div>
            </div>
          </div>

          <!-- Messages -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.8s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><Mail class="me-2" />Messages</h5>
                <p class="card-text small text-muted">View and manage messages sent to you via your
                  public profile.</p>
                <!-- THIS IS THE FIX: Changed route name to 'user-messages' -->
                <router-link :to="{ name: 'user-messages', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  View Messages
                </router-link>
              </div>
            </div>
          </div>

          <!-- Data & Exports -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 0.9s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><CloudDownload class="me-2" />Data & Exports
                </h5>
                <p class="card-text small text-muted">Manage downloadable assets like your PDF resume,
                  vCard, and Markdown profile.</p>
                <router-link :to="{ name: 'my-data-exports', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start"> Manage Exports
                </router-link>
              </div>
            </div>
          </div>

          <!-- Backup & Restore -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 1.0s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><Database class="me-2" />Backup & Restore</h5>
                <p class="card-text small text-muted">Download a full backup of your portfolio or
                  restore from a file.</p>
                <router-link :to="{ name: 'my-backup', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Go to Backup
                </router-link>
              </div>
            </div>
          </div>

          <!-- Display Settings -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 1.1s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><SlidersHorizontal class="me-2" />Display Settings</h5>
                <p class="card-text small text-muted">Configure the visibility of sections on your
                  public portfolio.</p>
                <router-link :to="{ name: 'display-settings', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage Display
                </router-link>
              </div>
            </div>
          </div>

          <!-- PDF Settings -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 1.2s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title"><FileText class="me-2" />PDF Settings
                </h5>
                <p class="card-text small text-muted">Choose the default template for your
                  downloadable PDF portfolio.</p>
                <router-link :to="{ name: 'pdf-settings', params: { slug: user.slug } }"
                             class="btn btn-outline-primary mt-auto align-self-start">
                  Manage PDF
                </router-link>
              </div>
            </div>
          </div>

          <!-- Guest Passes -->
          <div class="col-md-6 col-lg-4 animate-fade-in-up" style="animation-delay: 1.3s;">
            <div class="card h-100 glass-card interactive-card-lift interactive-card-shadow-primary border-primary-subtle">
              <div class="card-body d-flex flex-column">
                <h5 class="card-title text-primary"><Ticket class="me-2" />Guest Passes</h5>
                <p class="card-text small text-muted">Generate secure, temporary access links for recruiters and reviewers.</p>
                <router-link :to="{ name: 'my-guest-passes', params: { slug: user.slug } }"
                             class="btn btn-primary mt-auto align-self-start shadow-sm">
                  Manage Passes
                </router-link>
              </div>
            </div>
          </div>

        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {authService} from '@/services/authService';
import type { User } from '@/types';
import { IdCard, KanbanSquare, Briefcase, Tags, BadgeCheck, MessageSquareQuote, Mail, CloudDownload, Database, SlidersHorizontal, FileText, Ticket } from 'lucide-vue-next';

const user = computed(() => authService.user.value as User | null);
</script>

<style scoped>
.dashboard-page .display-4,
.dashboard-page .display-5 {
  font-weight: 300;
}

.card-title i {
  vertical-align: -0.125em;
  opacity: 0.8;
}

/* Responsive styles for mobile */
@media (max-width: 576px) {
  .dashboard-page {
    padding: 1rem 0;
  }

  .dashboard-page .container {
    padding: 0 1rem;
  }

  .dashboard-page .display-4 {
    font-size: 2rem;
  }

  .dashboard-page .lead {
    font-size: 1rem;
  }

  .dashboard-page .card-body {
    padding: 1rem;
  }

  .dashboard-page .card-title {
    font-size: 1.1rem;
  }

  .dashboard-page .card-text {
    font-size: 0.9rem;
  }

  .dashboard-page .btn {
    padding: 0.5rem 1rem;
    font-size: 0.9rem;
    min-height: 44px; /* Touch-friendly */
  }
}

/* Tablet enhancements */
@media (min-width: 768px) {
  .dashboard-page .row {
    --bs-gutter-x: 2rem; /* Increase gap on tablet */
  }
}

/* Desktop enhancements */
@media (min-width: 1024px) {
  .dashboard-page .container {
    max-width: 1200px;
  }
}
</style>
