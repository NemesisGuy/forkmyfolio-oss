<template>
  <div class="tag-input-container" @click="focusInput">
    <span v-for="(tag, index) in tags" :key="index" class="badge tag-badge">
      {{ tag }}
      <button aria-label="Remove tag" class="btn-close btn-close-sm" type="button"
              @click.stop="removeTag(index)"></button>
    </span>
    <input
      ref="inputRef"
      v-model="newTag"
      :placeholder="tags.length === 0 ? placeholder : ''"
      class="tag-input"
      type="text"
      @blur="addTag"
      @keydown.enter.prevent="addTag"
      @keydown.backspace="handleBackspace"
      @keydown.,.prevent="addTag"
    />
  </div>
</template>

<script setup lang="ts">
import {ref, watch} from 'vue';

const props = withDefaults(defineProps<{
  modelValue?: string[];
  placeholder?: string;
}>(), {
  modelValue: () => [],
  placeholder: 'Add a tag...',
});

const emit = defineEmits<{
  (e: 'update:modelValue', tags: string[]): void;
}>();

const tags = ref<string[]>([...props.modelValue]);
const newTag = ref('');
const inputRef = ref<HTMLInputElement | null>(null);

watch(() => props.modelValue, (newValue) => {
  if (JSON.stringify(newValue) !== JSON.stringify(tags.value)) {
    tags.value = [...newValue];
  }
}, {deep: true});

const addTag = () => {
  const tagToAdd = newTag.value.replace(/,/g, '').trim();
  if (tagToAdd && !tags.value.includes(tagToAdd)) {
    tags.value.push(tagToAdd);
    emit('update:modelValue', tags.value);
  }
  newTag.value = '';
};

const removeTag = (index: number) => {
  tags.value.splice(index, 1);
  emit('update:modelValue', tags.value);
};

const handleBackspace = () => {
  if (newTag.value === '' && tags.value.length > 0) {
    removeTag(tags.value.length - 1);
  }
};

const focusInput = () => {
  inputRef.value?.focus();
};
</script>

<style scoped>
.tag-input-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: var(--glass-bg);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  border: 1px solid var(--glass-border);
  color: var(--glass-text);
  border-radius: 0.375rem;
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  cursor: text;
}

.tag-input-container:focus-within {
  border-color: rgba(var(--bs-primary-rgb), 0.5);
  box-shadow: 0 0 0 0.25rem rgba(var(--bs-primary-rgb), 0.15);
}

.tag-badge {
  display: inline-flex;
  align-items: center;
  padding: 0.4em 0.4em 0.4em 0.8em;
  font-size: 0.9em;
  font-weight: 500;
  background-color: var(--bs-primary);
  color: white;
  border-radius: 0.375rem;
}

.tag-badge .btn-close {
  margin-left: 0.5em;
  filter: invert(1) grayscale(100%) brightness(200%);
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.tag-badge .btn-close:hover {
  opacity: 1;
}

.tag-input {
  flex-grow: 1;
  border: none;
  outline: none;
  background-color: transparent;
  padding: 0.25rem;
  min-width: 120px;
  color: var(--glass-text);
}

.tag-input::placeholder {
  color: var(--glass-text-secondary);
  opacity: 0.7;
}
</style>
