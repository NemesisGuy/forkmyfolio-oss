import { onMounted, onUnmounted, ref } from 'vue';

export function useModalHistory(modalInstanceRef, onClose) {
  const isModalOpen = ref(false);

  const openWithHistory = () => {
    isModalOpen.value = true;
    // Push a hash to the URL to create a history entry
    // We utilize a hash so it doesn't trigger a full route change but allows back button capture
    history.pushState({ modalOpen: true }, '', '#modal');
    modalInstanceRef.value?.show();
  };

  const closeWithHistory = () => {
    isModalOpen.value = false;
    modalInstanceRef.value?.hide();
    // If we have the hash state, go back
    if (window.location.hash === '#modal') {
      history.back();
    }
  };

  // Handle explicit close (e.g. Save button, X button) without double-back
  // If the user clicks 'Save', we hide the modal manually.
  // We need to verify if we need to pop the history state.
  const handleExplicitClose = () => {
     if (isModalOpen.value) {
        isModalOpen.value = false;
        modalInstanceRef.value?.hide();
        if (window.location.hash === '#modal') {
            history.back();
        }
     }
  };

  // Listen for back button (popstate)
  const onPopState = () => {
    if (isModalOpen.value && window.location.hash !== '#modal') {
      // The user pressed back, removing the #modal hash
      // We should close the modal now.
      isModalOpen.value = false;
      modalInstanceRef.value?.hide();
      if (onClose) onClose();
    }
  };

  onMounted(() => {
    window.addEventListener('popstate', onPopState);
    const modalEl = modalInstanceRef.value?._element || document.querySelector('.modal');
    if (modalEl) {
        modalEl.addEventListener('hidden.bs.modal', () => {
             // Catch-all: if clicked backdrop
             if (isModalOpen.value && window.location.hash === '#modal') {
                 history.back();
                 isModalOpen.value = false;
             }
        });
    }
  });

  onUnmounted(() => {
    window.removeEventListener('popstate', onPopState);
  });

  return {
    openWithHistory,
    closeWithHistory,
    handleExplicitClose
  };
}
