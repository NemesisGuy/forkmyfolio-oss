<script setup lang="ts">
import JobApplicationList from '@/components/job-tracker/JobApplicationList.vue';
</script>

<template>
  <div class="job-tracker-page">
    <JobApplicationList />
  </div>
</template>

<style scoped>
.job-tracker-page {
  min-height: 100vh;
  /* Optional: Add page-specific styling if needed, otherwise inherit from global */
}
</style>
