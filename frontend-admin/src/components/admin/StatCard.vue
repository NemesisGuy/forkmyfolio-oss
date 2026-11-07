<template>
  <div class="card h-100 glass-card interactive-card-lift">
    <div class="card-body">
      <div class="d-flex justify-content-between align-items-center">
        <div>
          <div class="card-title text-muted text-uppercase small mb-1">{{ title }}</div>
          <div class="h4 mb-0 fw-bold glass-text">{{ value }}</div>
        </div>
        <div class="stat-icon">
          <i :class="['bi', icon]"></i>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  value: string | number;
  /**
   * Icon class name. Deprecated Bootstrap Icon usage; migrating to Lucide components.
   */
  icon: string;
}>();
</script>

<style scoped>
.stat-icon {
  font-size: 2.5rem;
  color: var(--bs-primary);
  opacity: 0.3;
  transition: transform 0.3s ease;
}

.card:hover .stat-icon {
  transform: scale(1.1);
}

.card-title {
  font-weight: 500;
}
</style>
