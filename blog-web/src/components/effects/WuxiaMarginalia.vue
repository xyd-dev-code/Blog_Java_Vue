<template>
  <!-- Decorative diagrams, not navigational maps or martial-arts instructions. -->
  <svg v-if="variant === 'map'" viewBox="0 0 300 170" fill="none" aria-hidden="true" focusable="false">
    <g stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M12 66 31 38 42 57 59 24 76 55 91 37 110 65 M18 72 34 50 42 63 M46 61 59 38 72 64 M171 45 186 25 201 46 213 34 234 56 M226 136 244 105 260 126 275 95 290 124" />
      <path d="M12 117 Q53 92 82 108 T141 105 Q158 100 174 77 T224 72 Q260 84 290 61 M13 124 Q54 102 82 115 T142 112 Q164 107 181 83 T225 79 Q261 91 291 68" opacity=".65" />
      <path d="M65 77 Q106 61 148 62 T245 91 M65 77 Q74 123 175 138 T245 91" stroke-dasharray="3 6" />
      <circle cx="65" cy="77" r="4" /><circle cx="148" cy="62" r="4" /><circle cx="245" cy="91" r="4" /><circle cx="175" cy="138" r="4" />
    </g>
    <g fill="currentColor" font-size="13" font-family="var(--font-serif)">
      <text x="35" y="97">华山</text><text x="137" y="51">武当</text><text x="248" y="84">少林</text><text x="140" y="159">龙门客栈</text>
    </g>
    <text x="5" y="17" fill="var(--c-autumn-700)" font-size="20" font-family="var(--font-wuxia)">江湖舆图</text>
  </svg>
  <svg v-else viewBox="0 0 300 126" fill="none" aria-hidden="true" focusable="false">
    <g stroke="currentColor" stroke-linecap="round" stroke-linejoin="round">
      <!-- Three brush silhouettes are part of the existing SVG decoration system. -->
      <g transform="translate(44 46)">
        <circle cy="-9" r="5" fill="currentColor" stroke="none" />
        <path d="M0 0 -7 20 -13 31 4 25 10 12Z" fill="currentColor" stroke="none" />
        <path d="M-1 3 -16 12 -25 6 M4 5 20 -1 29 -17 M-4 26 -16 43 M4 26 21 41" stroke-width="3.2" />
        <path d="M24 -12 47 -45 M19 -13 29 -7" stroke-width="1.5" />
        <path d="M-27 -7 Q-23 -35 18 -40" stroke-width=".8" stroke-dasharray="2 4" />
      </g>
      <g transform="translate(143 46)">
        <circle cx="-2" cy="-9" r="5" fill="currentColor" stroke="none" />
        <path d="M-4 0 -10 18 9 26 3 6Z" fill="currentColor" stroke="none" />
        <path d="M-3 3 -22 -2 -29 -11 M1 7 17 10 32 2 M-6 21 -23 37 M5 24 21 39" stroke-width="3.2" />
        <path d="M28 5 57 -11 M26 0 31 10" stroke-width="1.5" />
        <path d="M20 -30 Q53 -23 57 8" stroke-width=".8" stroke-dasharray="2 4" />
      </g>
      <g transform="translate(247 46)">
        <circle cx="-2" cy="-9" r="5" fill="currentColor" stroke="none" />
        <path d="M-4 0 -13 19 7 24 3 5Z" fill="currentColor" stroke="none" />
        <path d="M-3 3 -19 9 -31 3 M1 6 15 -4 17 -20 M-7 21 -20 40 M3 24 12 43" stroke-width="3.2" />
        <path d="M17 -16 22 -43 M12 -17 23 -15" stroke-width="1.5" />
        <path d="M-26 -20 Q-2 -37 25 -24" stroke-width=".8" stroke-dasharray="2 4" />
      </g>
    </g>
    <g fill="currentColor" font-size="12" text-anchor="middle" font-family="var(--font-serif)">
      <text x="44" y="109">起势</text><text x="143" y="109">破风</text><text x="247" y="109">归鞘</text>
    </g>
    <path d="M12 122 H284" stroke="var(--c-autumn-700)" stroke-width=".8" opacity=".6" />
  </svg>
</template>

<script setup>
defineProps({ variant: { type: String, default: 'map' } })
</script>
