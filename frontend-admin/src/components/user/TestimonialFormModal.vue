<template>
  <div id="testimonialFormModal" ref="modalRef" aria-hidden="true"
       aria-labelledby="testimonialModalLabel" class="modal fade" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content glass-modal">
        <div class="modal-header">
          <h5 id="testimonialModalLabel" class="modal-title">
            {{ isEditing ? 'Edit Testimonial' : 'Add New Testimonial' }}</h5>
          <button aria-label="Close" class="btn-close" data-bs-dismiss="modal"
                  type="button"></button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="submitForm">
            <div class="mb-3">
              <label class="form-label" for="testimonialQuote">Quote</label>
              <textarea id="testimonialQuote" v-model="formState.quote"
                        class="form-control" required rows="4"></textarea>
            </div>
            <div class="row">
              <div class="col-md-6 mb-3">
                <label class="form-label" for="testimonialAuthorName">Author's Name</label>
                <input id="testimonialAuthorName" v-model="formState.authorName"
                       class="form-control"
                       required type="text">
              </div>
              <div class="col-md-6 mb-3">
                <label class="form-label" for="testimonialAuthorTitle">Author's Title (e.g.,
                  "CEO at Company")</label>
                <input id="testimonialAuthorTitle" v-model="formState.authorTitle"
                       class="form-control"
                       type="text">
              </div>
            </div>
            <div class="form-check form-switch">
              <input id="testimonialVisible" v-model="formState.visible"
                     class="form-check-input"
                     role="switch" type="checkbox">
              <label class="form-check-label" for="testimonialVisible">Visible on public
                portfolio</label>
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" data-bs-dismiss="modal" type="button">Close</button>
          <button class="btn btn-primary" type="button" @click="submitForm">
            {{ isEditing ? 'Save Changes' : 'Add Testimonial' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive, ref, watch} from 'vue';
import {Modal} from 'bootstrap';
import type { Testimonial } from '@/types';

export interface TestimonialFormState {
  uuid: string | null;
  quote: string;
  authorName: string;
  authorTitle: string;
  visible: boolean;
}

const props = defineProps<{
  testimonial?: Testimonial | null;
  isEditing?: boolean;
}>();

const emit = defineEmits<{
  (e: 'save', state: TestimonialFormState): void;
}>();

const modalRef = ref<HTMLElement | null>(null);
let modalInstance: Modal | null = null;

const getInitialState = (): TestimonialFormState => ({
  uuid: null,
  quote: '',
  authorName: '',
  authorTitle: '',
  visible: true,
});

const formState = reactive<TestimonialFormState>(getInitialState());

watch(() => props.testimonial, (newVal) => {
  if (newVal) {
    Object.assign(formState, JSON.parse(JSON.stringify(newVal)));
  } else {
    Object.assign(formState, getInitialState());
  }
}, {deep: true});

onMounted(() => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value);
  }
});

const submitForm = () => {
  emit('save', {...formState});
};

defineExpose({
  show: () => modalInstance?.show(),
  hide: () => modalInstance?.hide(),
});
</script>

<style scoped>
.form-label {
  font-weight: 500;
  color: var(--glass-text);
}
</style>
