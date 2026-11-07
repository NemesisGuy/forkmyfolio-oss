## Objective

Make all pages fully responsive across common **mobile, tablet, and desktop** viewports, with a primary focus on **mobile-first design**.

## Primary Target Device

* **iPhone SE (2nd/3rd gen)**

  * Viewport: **375 × 667**
  * This is the minimum baseline. All layouts must render cleanly at this size with no horizontal scrolling.

## Supported Viewports

Design and test responsiveness for the following standard breakpoints:

### Mobile

* 320 × 568 (older small phones)
* **375 × 667 (iPhone SE baseline)**
* 390 × 844 (modern iPhone)
* 412 × 915 (large Android)

### Tablet

* 768 × 1024 (iPad portrait)
* 810 × 1080 (iPad 10th gen)
* 834 × 1194 (iPad Pro)

### Desktop

* 1024 × 768 (small laptop)
* 1366 × 768 (standard laptop)
* 1440 × 900
* 1920 × 1080 (full HD)

## Design Requirements

* Use **mobile-first CSS** (start at 375px, scale up)
* No fixed widths; use flexible layouts (Flexbox / Grid)
* Typography must scale appropriately
* Touch targets must be mobile-friendly (min 44×44 px)
* Images must be responsive and not overflow containers
* Navigation must adapt (hamburger / collapsible where needed)

## Technical Guidelines

* Prefer relative units: `rem`, `%`, `vw`, `vh`
* Avoid device-specific hacks
* Use logical breakpoints based on layout changes, not devices
* Ensure consistent spacing and alignment across viewports

## Testing Checklist

* [ ] No horizontal scrolling at any breakpoint
* [ ] Content readable without zoom on mobile
* [ ] Layout does not break between breakpoints
* [ ] Tablet layout is not just a stretched mobile layout
* [ ] Desktop layout makes effective use of space

## Deliverables

* Responsive updates applied to all pages
* Verified against listed breakpoints
* Any layout trade-offs or constraints documented

---

**Note:** This document is the single source of truth for responsive behavior. All future UI changes must comply with these requirements.
