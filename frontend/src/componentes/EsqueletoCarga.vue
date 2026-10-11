<script setup>
defineProps({
  variante: { type: String, default: 'tabla' },
  etiqueta: { type: String, default: 'Cargando información' },
  filas: { type: Number, default: 5 }
})
</script>

<template>
  <div class="esqueleto-carga" role="status" :aria-label="etiqueta">
    <template v-if="variante === 'resumen'">
      <div class="esqueleto-rejilla-indicadores">
        <div v-for="indice in 4" :key="indice" class="esqueleto esqueleto-indicador"></div>
      </div>
      <div class="esqueleto-paneles">
        <div class="esqueleto esqueleto-panel-grande"></div>
        <div class="esqueleto esqueleto-panel-pequeno"></div>
      </div>
    </template>

    <template v-else-if="variante === 'detalle'">
      <div class="esqueleto-encabezado">
        <div class="esqueleto esqueleto-titulo"></div>
        <div class="esqueleto esqueleto-subtitulo"></div>
      </div>
      <div class="esqueleto-paneles">
        <div class="esqueleto esqueleto-panel-mediano"></div>
        <div class="esqueleto esqueleto-panel-mediano"></div>
      </div>
      <div class="esqueleto esqueleto-panel-ancho"></div>
    </template>

    <template v-else-if="variante === 'formulario'">
      <div class="esqueleto-encabezado">
        <div class="esqueleto esqueleto-titulo"></div>
        <div class="esqueleto esqueleto-subtitulo"></div>
      </div>
      <div class="esqueleto-campos">
        <div v-for="indice in 6" :key="indice" class="esqueleto-campo">
          <div class="esqueleto esqueleto-etiqueta"></div>
          <div class="esqueleto esqueleto-control"></div>
        </div>
      </div>
    </template>

    <template v-else>
      <div class="esqueleto esqueleto-cabecera-tabla"></div>
      <div v-for="indice in filas" :key="indice" class="esqueleto-fila">
        <div class="esqueleto esqueleto-avatar"></div>
        <div class="esqueleto esqueleto-texto-largo"></div>
        <div class="esqueleto esqueleto-texto-medio"></div>
        <div class="esqueleto esqueleto-texto-corto"></div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.esqueleto-carga { display: flex; flex-direction: column; gap: 16px; padding: 4px 0; }
.esqueleto-rejilla-indicadores { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.esqueleto-indicador { height: 146px; }
.esqueleto-paneles { display: grid; grid-template-columns: minmax(0, 1.75fr) minmax(0, 1fr); gap: 24px; }
.esqueleto-panel-grande { height: 320px; }
.esqueleto-panel-pequeno { height: 320px; }
.esqueleto-panel-mediano { height: 220px; }
.esqueleto-panel-ancho { height: 260px; }
.esqueleto-encabezado { display: flex; flex-direction: column; gap: 10px; }
.esqueleto-titulo { width: 320px; max-width: 70%; height: 30px; }
.esqueleto-subtitulo { width: 460px; max-width: 90%; height: 14px; }
.esqueleto-campos { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px 24px; padding: 24px; border: 1px solid var(--color-borde); border-radius: var(--radio); background: var(--color-superficie); }
.esqueleto-campo { display: flex; flex-direction: column; gap: 8px; }
.esqueleto-etiqueta { width: 40%; height: 12px; }
.esqueleto-control { height: var(--altura-control); }
.esqueleto-cabecera-tabla { height: 42px; }
.esqueleto-fila { display: grid; grid-template-columns: 36px 2fr 1.4fr 1fr; align-items: center; gap: 20px; padding: 8px 20px; }
.esqueleto-avatar { width: 36px; height: 36px; border-radius: 50%; }
.esqueleto-texto-largo, .esqueleto-texto-medio, .esqueleto-texto-corto { height: 14px; }
.esqueleto-texto-medio { width: 80%; }
.esqueleto-texto-corto { width: 60%; }
@media (max-width: 900px) {
  .esqueleto-rejilla-indicadores { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .esqueleto-paneles, .esqueleto-campos { grid-template-columns: 1fr; }
}
</style>
