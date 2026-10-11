<template>
  <svg class="chip-tarjeta" width="46" height="36" viewBox="0 0 46 36" aria-hidden="true">
    <defs>
      <linearGradient id="chip-dorado" x1="0" y1="0" x2="1" y2="1">
        <stop offset="0" stop-color="#E6C66E" />
        <stop offset="0.5" stop-color="#B8913A" />
        <stop offset="1" stop-color="#D9B45A" />
      </linearGradient>
    </defs>
    <rect x="0.5" y="0.5" width="45" height="35" rx="6.5" fill="url(#chip-dorado)" stroke="rgba(94, 66, 12, 0.55)" />
    <g fill="none" stroke="rgba(94, 66, 12, 0.6)" stroke-width="1" stroke-linecap="round">
      <path d="M23 1v9M23 26v9" />
      <path d="M1 12h12M33 12h12M1 24h12M33 24h12" />
      <rect x="13" y="10" width="20" height="16" rx="3.5" />
      <path d="M13 18h-6M33 18h6" />
    </g>
  </svg>
</template>

<style scoped>
.chip-tarjeta { display: block; flex: none; filter: drop-shadow(0 1px 1px rgba(0, 0, 0, 0.25)); }
</style>
