<template>
  <div v-if="visible" class="modal-backdrop" @click="onCancel">
    <div class="modal-content glass-card" @click.stop>
      <h5 class="modal-title mb-3">Verify Password</h5>
      <p class="text-muted small mb-3">
        Enter your password to verify backup access. This is required for sensitive backup operations.
      </p>
      <div class="mb-3">
        <input
          ref="passwordInput"
          v-model="password"
          :type="showPassword ? 'text' : 'password'"
          class="form-control"
          placeholder="Password"
          @keyup.enter="onConfirm"
        />
      </div>
      <div class="d-flex justify-content-end gap-2">
        <button class="btn btn-secondary" @click="onCancel">Cancel</button>
        <button class="btn btn-primary" @click="onConfirm" :disabled="!password">
          Verify
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';

const props = defineProps<{
  visible: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'confirm', password: string): void;
}>();

const password = ref('');
const showPassword = ref(false);

watch(() => props.visible, async (v) => {
  if (v) {
    password.value = '';
    await nextTick();
    passwordInput.value?.focus();
  }
});

const onCancel = () => {
  password.value = '';
  emit('close');
};

const onConfirm = () => {
  if (password.value) {
    emit('confirm', password.value);
  }
};
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}
.modal-content {
  width: 100%;
  max-width: 420px;
  padding: 1.5rem;
}
</style>
