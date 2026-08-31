<template>
  <div class="ink-wash-backdrop" :data-scene="scene" aria-hidden="true">
    <svg class="ink-landscape" viewBox="0 0 1600 900" preserveAspectRatio="xMidYMid slice">
      <defs>
        <linearGradient id="ink-far" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--c-botany-300)" stop-opacity="0.28" />
          <stop offset="1" stop-color="var(--c-paper)" stop-opacity="0" />
        </linearGradient>
        <linearGradient id="ink-mid" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--c-botany-800)" stop-opacity="0.25" />
          <stop offset="0.8" stop-color="var(--c-botany-100)" stop-opacity="0.05" />
          <stop offset="1" stop-color="var(--c-paper)" stop-opacity="0" />
        </linearGradient>
        <linearGradient id="ink-near" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stop-color="var(--c-ink-900)" stop-opacity="0.2" />
          <stop offset="1" stop-color="var(--c-paper)" stop-opacity="0" />
        </linearGradient>
      </defs>

      <!-- 上疏下密、近深远浅：远山保持模糊轮廓，中近景只占视口下部。 -->
      <path class="mountain mountain-far" fill="url(#ink-far)"
        d="M0 642 C72 620 119 626 170 594 L219 555 L246 583 L278 567 L320 521 L350 566 C398 604 435 564 472 544 L510 562 L548 526 L584 544 L631 438 L658 466 L685 445 L711 521 C758 572 798 515 839 540 L886 506 L918 532 L958 468 L982 491 L1012 477 L1043 536 C1090 580 1131 524 1170 548 L1213 516 L1241 536 L1282 481 L1310 512 C1361 565 1405 514 1454 531 L1510 503 L1542 525 L1600 501 L1600 900 L0 900 Z" />
      <path class="mountain mountain-mid" fill="url(#ink-mid)"
        d="M0 704 C91 641 164 678 236 613 C292 563 347 635 413 574 C464 527 527 614 586 552 C640 496 704 602 770 539 C828 484 886 596 952 536 C1012 481 1081 602 1147 551 C1214 500 1279 610 1348 555 C1425 494 1496 566 1600 516 L1600 900 L0 900 Z" />
      <path class="mountain mountain-near" fill="url(#ink-near)"
        d="M0 810 C105 720 192 792 276 715 C343 654 413 754 487 690 C554 631 622 760 704 699 C775 647 843 759 930 704 C1012 650 1097 771 1182 706 C1262 644 1350 765 1436 701 C1502 652 1555 675 1600 658 L1600 900 L0 900 Z" />

      <g class="ink-sun" transform="translate(1260 205)">
        <circle r="72" fill="var(--c-autumn-500)" />
        <circle r="59" fill="var(--c-autumn-300)" opacity="0.24" />
      </g>

      <!-- 侧边竹影只承担取景框作用，中间内容区保持留白。 -->
      <g class="ink-bamboo ink-bamboo-left" fill="none" stroke="var(--c-ink-800)" stroke-linecap="round">
        <path d="M106 900 Q127 753 111 585" stroke-width="9" />
        <path d="M110 726 Q62 675 38 633 M116 675 Q168 623 189 568 M111 620 Q67 584 47 542" stroke-width="4" />
        <path d="M40 633 Q78 628 96 664 Q57 668 40 633 Z M189 568 Q151 571 130 607 Q171 607 189 568 Z M47 542 Q80 547 99 579 Q64 576 47 542 Z"
          fill="var(--c-ink-700)" stroke="none" />
      </g>
      <g class="ink-bamboo ink-bamboo-right" fill="none" stroke="var(--c-ink-800)" stroke-linecap="round">
        <path d="M1517 900 Q1494 781 1515 646" stroke-width="8" />
        <path d="M1506 788 Q1464 746 1441 710 M1510 728 Q1547 688 1574 665" stroke-width="4" />
        <path d="M1441 710 Q1477 709 1493 742 Q1458 745 1441 710 Z M1574 665 Q1540 668 1523 700 Q1558 701 1574 665 Z"
          fill="var(--c-ink-700)" stroke="none" />
      </g>

      <g class="ink-seal" transform="translate(1450 790)">
        <rect width="54" height="54" rx="4" fill="none" stroke="var(--c-autumn-700)" stroke-width="3" />
        <text x="27" y="37" text-anchor="middle" fill="var(--c-autumn-700)" font-size="28" font-family="var(--font-wuxia)">侠</text>
      </g>
    </svg>

    <WuxiaMarginalia class="wuxia-page-map" variant="map" />
    <WuxiaMarginalia class="wuxia-page-manual" variant="manual" />

    <div class="ink-bloom ink-bloom-a" />
    <div class="ink-bloom ink-bloom-b" />
    <div class="ink-mist ink-mist-far" />
    <div class="ink-mist ink-mist-near" />
    <div class="ink-water" />
    <div class="ink-petals">
      <i v-for="n in 6" :key="n" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import WuxiaMarginalia from './WuxiaMarginalia.vue'

const route = useRoute()

const scene = computed(() => {
  const path = route.path
  if (path.startsWith('/admin')) return 'admin'
  if (/^\/articles\/[^/]+/.test(path)) return 'reading'
  if (path === '/') return 'home'
  return 'collection'
})
</script>

<style scoped lang="scss">
.ink-wash-backdrop {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  pointer-events: none;
  contain: strict;
  opacity: 0;
  visibility: hidden;
  background: transparent;
  transition: opacity var(--theme-motion-base) var(--theme-motion-ease);
}

.ink-landscape {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.wuxia-page-map,
.wuxia-page-manual {
  position: absolute;
  color: var(--c-ink-800);
  width: min(20vw, 290px);
  opacity: .16;
}
.wuxia-page-map { left: 1%; bottom: 9%; }
.wuxia-page-manual { right: 1%; top: 16%; opacity: .12; }

.mountain-far { opacity: var(--theme-ink-mountain-far); }
.mountain-mid { opacity: var(--theme-ink-mountain-mid); }
.mountain-near { opacity: var(--theme-ink-mountain-near); }

.ink-sun {
  opacity: var(--theme-ink-sun-opacity);
  transform-origin: 1260px 205px;
  animation: ink-breathe var(--theme-ink-bloom-duration) var(--theme-motion-ease) infinite alternate;
}

.ink-cranes { opacity: var(--theme-ink-crane-opacity); }
.ink-bamboo { opacity: var(--theme-ink-bamboo-opacity); }
.ink-seal { opacity: var(--theme-ink-seal-opacity); }

.ink-bloom {
  position: absolute;
  width: 42vw;
  aspect-ratio: 1;
  border-radius: 50%;
  opacity: var(--theme-ink-bloom-opacity);
  background: radial-gradient(circle, rgba(var(--theme-ink-rgb), 0.16) 0 8%, rgba(var(--theme-primary-strong-rgb), 0.08) 18%, transparent 68%);
  animation: ink-bloom var(--theme-ink-bloom-duration) var(--theme-motion-ease) infinite alternate;
}

.ink-bloom-a { left: -18vw; bottom: -24vw; }
.ink-bloom-b { right: -22vw; top: -28vw; animation-delay: var(--theme-ink-bloom-delay); }

.ink-mist {
  position: absolute;
  left: -10%;
  width: 120%;
  height: 190px;
  opacity: var(--theme-ink-mist-opacity);
  background-image:
    radial-gradient(ellipse 220px 54px at 20% 52%, rgba(var(--theme-paper-rgb), 0.94), transparent 74%),
    radial-gradient(ellipse 310px 66px at 72% 46%, rgba(var(--theme-paper-rgb), 0.9), transparent 76%);
  background-repeat: repeat-x;
  background-size: 720px 190px;
  animation: ink-mist-drift var(--theme-ink-mist-duration) linear infinite;
}

.ink-mist-far { bottom: 19%; }
.ink-mist-near { bottom: 4%; opacity: var(--theme-ink-mist-near-opacity); animation-direction: reverse; animation-duration: var(--theme-ink-near-mist-duration); }

.ink-water {
  position: absolute;
  right: -4%;
  bottom: 4%;
  width: 58%;
  height: 120px;
  opacity: var(--theme-ink-water-opacity);
  mask-image: linear-gradient(90deg, transparent, black 18%, black 82%, transparent);
  -webkit-mask-image: linear-gradient(90deg, transparent, black 18%, black 82%, transparent);
  background: repeating-linear-gradient(0deg, transparent 0 11px, rgba(var(--theme-primary-dark-rgb), 0.3) 12px, transparent 13px 19px);
  background-size: 140px 100%;
  animation: ink-water-drift var(--theme-ink-water-duration) linear infinite;
}

.ink-petals i {
  --petal-x: 12vw;
  --petal-delay: 0s;
  --petal-duration: 18s;
  position: absolute;
  top: -24px;
  left: var(--petal-x);
  width: 9px;
  height: 14px;
  border-radius: 90% 15% 80% 25%;
  opacity: var(--theme-ink-petal-opacity);
  background: var(--c-autumn-500);
  animation: ink-petal-fall var(--petal-duration) linear var(--petal-delay) infinite;
}

.ink-petals i:nth-child(2) { --petal-x: 29vw; --petal-delay: -7s; --petal-duration: 21s; width: 7px; height: 11px; }
.ink-petals i:nth-child(3) { --petal-x: 47vw; --petal-delay: -13s; --petal-duration: 24s; }
.ink-petals i:nth-child(4) { --petal-x: 63vw; --petal-delay: -4s; --petal-duration: 20s; width: 6px; height: 10px; }
.ink-petals i:nth-child(5) { --petal-x: 78vw; --petal-delay: -16s; --petal-duration: 25s; }
.ink-petals i:nth-child(6) { --petal-x: 91vw; --petal-delay: -10s; --petal-duration: 22s; width: 7px; height: 12px; }

[data-scene='reading'] {
  .wuxia-page-manual { display: none; }
  .wuxia-page-map { opacity: .07; }
  .ink-sun,
  .ink-petals,
  .ink-bamboo-right { display: none; }
  .ink-water { animation: none; opacity: var(--theme-ink-reading-water-opacity); }
  .ink-landscape { opacity: 0.72; }
}

[data-scene='collection'] {
  .ink-sun { opacity: var(--theme-ink-collection-sun-opacity); }
  .ink-petals i:nth-child(n + 5) { display: none; }
}

[data-scene='admin'] {
  .wuxia-page-map,
  .wuxia-page-manual { display: none; }
  .ink-sun,
  .ink-cranes,
  .ink-bamboo,
  .ink-petals,
  .ink-water,
  .ink-bloom { display: none; }
  .ink-landscape { opacity: 0.42; transform: translateY(9%); }
  .ink-mist { animation: none; opacity: var(--theme-ink-mist-near-opacity); }
}

@keyframes ink-mist-drift {
  to { background-position: 720px 0, 720px 0; }
}

@keyframes ink-water-drift {
  to { background-position: 140px 0; }
}

@keyframes ink-bloom {
  from { transform: scale(0.86); opacity: var(--theme-ink-bloom-start-opacity); }
  to { transform: scale(1.12); opacity: var(--theme-ink-bloom-opacity); }
}

@keyframes ink-breathe {
  from { transform: scale(0.96); }
  to { transform: scale(1.04); }
}

@keyframes ink-petal-fall {
  0% { transform: translate3d(0, -4vh, 0) rotate(0deg); }
  45% { transform: translate3d(42px, 48vh, 0) rotate(190deg); }
  100% { transform: translate3d(-28px, 106vh, 0) rotate(390deg); }
}

@media (max-width: 768px) {
  .wuxia-page-map,
  .wuxia-page-manual { display: none; }
  .ink-bamboo,
  .ink-seal,
  .ink-petals i:nth-child(n + 4) { display: none; }
  .ink-landscape { transform: translateY(18%) scale(1.22); transform-origin: center bottom; }
  .ink-water { width: 82%; opacity: var(--theme-ink-mobile-water-opacity); }
  .ink-bloom { width: 72vw; }
}

@media (prefers-reduced-motion: reduce), (update: slow) {
  .ink-wash-backdrop,
  .ink-wash-backdrop * {
    animation: none !important;
    transition: none !important;
  }
  .ink-petals { display: none; }
}
</style>
