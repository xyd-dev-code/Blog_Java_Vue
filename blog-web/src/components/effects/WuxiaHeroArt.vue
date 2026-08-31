<template>
  <div class="wuxia-hero-art" aria-hidden="true">
    <svg class="wuxia-filter-defs" width="0" height="0" focusable="false">
      <defs>
        <filter id="wuxia-swordsman-white-key" color-interpolation-filters="sRGB">
          <feColorMatrix in="SourceGraphic" type="matrix" values="
            0 0 0 0 0
            0 0 0 0 0
            0 0 0 0 0
            -0.2126 -0.7152 -0.0722 0 1" result="whiteKey" />
          <feComponentTransfer in="whiteKey" result="inkAlpha">
            <feFuncA type="linear" slope="40" intercept="-0.4" />
          </feComponentTransfer>
          <feComposite in="SourceGraphic" in2="inkAlpha" operator="in" />
        </filter>
      </defs>
    </svg>
    <div class="wuxia-duelist">
      <img src="/images/wuxia-swordsman-complete-v2.png" alt="" fetchpriority="high" decoding="async" />
    </div>
    <img class="wuxia-jianghu-border" src="/images/wuxia-jianghu-border.webp" alt="" decoding="async" />
    <WuxiaMarginalia class="wuxia-hero-map" variant="map" />
    <WuxiaMarginalia class="wuxia-hero-manual" variant="manual" />
    <span class="wuxia-seal">江湖</span>
  </div>
</template>

<script setup>
import WuxiaMarginalia from './WuxiaMarginalia.vue'
</script>

<style scoped lang="scss">
.wuxia-hero-art {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.wuxia-filter-defs { position: absolute; }
.wuxia-duelist {
  position: absolute;
  // User-marked space between the title and weather card, anchored to the
  // existing 1200px container. Only the decorative layer is positioned here.
  left: calc(50% - 333px);
  top: 24px;
  width: 440px;
  aspect-ratio: 1;
  opacity: .66;
  mix-blend-mode: multiply;
  filter: grayscale(1) contrast(1.12);
  // The completed silhouette is fitted whole, without zoom, crop or feather.
}
.wuxia-duelist img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  object-position: center;
  // Remove the flat white backing by color, not by a rectangular edge mask.
  filter: url(#wuxia-swordsman-white-key);
}
.wuxia-jianghu-border {
  position: absolute;
  bottom: -2%;
  left: 0;
  width: 100%;
  height: 69%;
  object-fit: fill;
  opacity: .63;
  mask-image: linear-gradient(transparent, #000 42%, #000 65%, transparent 100%);
}
.wuxia-hero-map {
  position: absolute;
  left: 38%;
  bottom: 1%;
  width: 200px;
  opacity: .35;
}
.wuxia-hero-manual {
  position: absolute;
  right: 10%;
  bottom: 3%;
  width: 265px;
  opacity: .28;
}
.wuxia-seal {
  position: absolute;
  right: 4.5%;
  top: 8%;
  width: 34px;
  padding: 6px 3px;
  border: 2px solid currentColor;
  color: var(--c-autumn-700);
  font: 25px/1.1 var(--font-wuxia);
  text-align: center;
  writing-mode: vertical-rl;
  transform: rotate(5deg);
  opacity: .75;
  box-shadow: inset 0 0 0 1px currentColor;
}
@media (max-width: 1199px) {
  // Narrow screens use only the hero's original 104px top padding.
  .wuxia-duelist {
    left: max(16px, calc((100% - 1200px) / 2 + 24px));
    top: 8px;
    width: 120px;
    height: 88px;
    aspect-ratio: auto;
    opacity: .4;
  }
}
@media (max-width: 900px) {
  .wuxia-jianghu-border { bottom: 5%; width: 154%; left: -25%; height: 47%; opacity: .42; }
  .wuxia-hero-map { left: 4%; bottom: 2%; width: 180px; opacity: .22; }
  .wuxia-hero-manual { right: 5%; bottom: 2%; width: 210px; opacity: .28; }
}
@media (max-width: 480px) {
  .wuxia-jianghu-border { bottom: 5%; left: -63%; width: 223%; height: 40%; opacity: .3; }
  .wuxia-hero-map { display: none; }
  .wuxia-hero-manual { width: 180px; right: 8%; bottom: 1%; }
  .wuxia-seal { right: 6%; top: 3%; opacity: .5; }
}
</style>
