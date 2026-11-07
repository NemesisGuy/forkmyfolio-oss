<template>
  <div class="user-guest-passes-page py-5 flex-grow-1">
    <div class="container">
      <!-- Header -->
      <div class="mb-4">
        <nav aria-label="breadcrumb">
          <ol class="breadcrumb mb-2">
            <li class="breadcrumb-item">
              <router-link :to="{ name: 'dashboard', params: { slug } }">Dashboard</router-link>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Guest Passes</li>
          </ol>
        </nav>
        <div class="d-flex align-items-center">
          <h1 class="h2 glass-text mb-0">
            <Ticket class="text-primary me-2" />Guest Pass Management
          </h1>
        </div>
        <p class="text-muted mt-2">
          Create and manage temporary access tokens for recruiters, interviewers, or clients to view your private portfolio.
        </p>
      </div>

      <div class="row">
        <div class="col-12">
          <GuestPassManagement :slug="slug" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Ticket } from 'lucide-vue-next';
import GuestPassManagement from '@/components/user/GuestPassManagement.vue';

defineProps<{
  slug: string;
}>();
</script>

<style scoped>
.breadcrumb-item a {
  text-decoration: none;
  color: var(--bs-primary);
}

.breadcrumb-item.active {
  color: var(--bs-secondary);
}
</style>
