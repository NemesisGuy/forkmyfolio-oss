import { describe, it, expect, vi, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import SuccessModal from './SuccessModal.vue';

/**
 * Regression pin (e2e run G): dismissing the success modal during its ~300ms
 * SHOW fade-in must still close it.
 *
 * <p>Bootstrap's `Modal.hide()` early-returns while `_isTransitioning` (the
 * show transition), and by the time the user clicks Great! the component's
 * `visible` prop is already false — so the old code emitted close, the hide
 * no-opped, and nothing ever removed the `show` class: the modal stayed stuck
 * open forever (run G: `critical-path` saw `.modal.show` still visible 5s
 * after Great!). The same race class as GuestPassManagement's Cancel button.
 *
 * <p>The Bootstrap mock reproduces exactly that race: `show()` adds the
 * `show` class (what Bootstrap does at fade-in start) and `hide()` is a
 * deliberate NO-OP (what Bootstrap does when hide() is called mid-transition).
 */
vi.mock('bootstrap', () => ({
  Modal: class {
    private el: HTMLElement;
    constructor(el: HTMLElement) {
      this.el = el;
    }
    show() {
      this.el.classList.add('show');
    }
    hide() {
      // Intentional no-op: simulates Bootstrap's `_isTransitioning` early return.
    }
  },
}));

const modalEl = () => document.querySelector('.modal') as HTMLElement;

const greatButton = () =>
  Array.from(document.querySelectorAll('button')).find(b =>
    b.textContent?.includes('Great!'));

describe('SuccessModal dismissal (run G stuck-modal race)', () => {
  afterEach(() => {
    document.querySelectorAll('.modal-backdrop').forEach(b => b.remove());
    document.body.classList.remove('modal-open');
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
  });

  it('forces dismissal when visible flips false and Bootstrap hide() no-ops', async () => {
    const wrapper = mount(SuccessModal, {
      props: { message: 'Saved successfully', visible: true },
    });
    const el = modalEl();
    expect(el.classList.contains('show')).toBe(true);

    // State Bootstrap would have set during show(): backdrop + locked body.
    const backdrop = document.createElement('div');
    backdrop.className = 'modal-backdrop show';
    document.body.appendChild(backdrop);
    document.body.classList.add('modal-open');

    await wrapper.setProps({ visible: false });
    await nextTick();

    expect(el.classList.contains('show')).toBe(false);
    expect(el.getAttribute('aria-hidden')).toBe('true');
    expect(el.style.display).toBe('none');
    expect(document.querySelector('.modal-backdrop')).toBeNull();
    expect(document.body.classList.contains('modal-open')).toBe(false);

    wrapper.unmount();
  });

  it('Great! click emits close AND dismisses when hide() no-ops mid-transition', async () => {
    const wrapper = mount(SuccessModal, {
      props: { message: 'Saved successfully', visible: true },
    });
    const el = modalEl();
    expect(el.classList.contains('show')).toBe(true);

    greatButton()?.click();
    await nextTick();

    expect(wrapper.emitted('close')).toBeTruthy();
    expect(el.classList.contains('show')).toBe(false);
    expect(el.getAttribute('aria-hidden')).toBe('true');
    expect(el.style.display).toBe('none');

    wrapper.unmount();
  });

  it('Close (X) button follows the same forced-dismissal path', async () => {
    const wrapper = mount(SuccessModal, {
      props: { message: 'Saved successfully', visible: true },
    });
    const el = modalEl();
    expect(el.classList.contains('show')).toBe(true);

    (document.querySelector('.btn-close') as HTMLButtonElement).click();
    await nextTick();

    expect(wrapper.emitted('close')).toBeTruthy();
    expect(el.classList.contains('show')).toBe(false);

    wrapper.unmount();
  });
});
