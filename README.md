# OreExcavation para Minecraft 26.2 (Forge)

Port de **OreExcavation** (de Funwayguy y Darkosto) a **Minecraft Java 26.2 con Forge**.

Mina vetas completas de mineral y tala árboles enteros de un golpe. También puedes minar con **formas** (túneles de 3x3, escaleras, etc.).

## ⬇️ Descargar

**[Descargar el mod (.jar)](https://github.com/javieraguirre83/oreexcavation/releases/download/ultima/OreExcavation-1.13.174-forge-26.2.jar)**

1. Descarga el archivo `.jar`.
2. Presiona **Windows + R**, escribe `%appdata%\.minecraft\mods` y dale Enter.
3. Pega ahí el `.jar` y abre Minecraft con el perfil de **Forge 26.2**.

---

## ⛏️ Cómo se usa

1. Ten la herramienta correcta en la mano (pico para minerales, hacha para troncos, etc.).
2. **Mantén presionada la tecla `R`** (Excavar) mientras rompes un bloque.
3. Se rompen también todos los bloques iguales que estén pegados (la veta o el árbol completo).

Si sueltas la tecla, la excavación se detiene ("Must Hold Key").

> En la configuración puedes cambiar el modo de activación: **KEY** (tecla R), **SNEAK** (agachado), **ALWAYS** (siempre) o **DISABLED** (apagado).

---

## ⌨️ Teclas

| Tecla | Qué hace |
|---|---|
| **R** | Excavar (mantener presionada al romper) |
| **V** | Cambiar de forma (o mantener V y usar la rueda del ratón) |
| **B** | Abrir el editor de formas (también **Ctrl + V**) |

Puedes cambiarlas en **Opciones → Controles → Asignación de teclas → OreExcavation**.

---

## 🔷 Formas

Con **V** eliges una forma: `NONE` (veta normal), `1x2`, `3x3`, `5x5`, `Stairs Up` y `Stairs Down`. Con una forma activa, al excavar se rompe esa figura hacia donde miras, y verás un **contorno blanco** sobre el bloque.

### Editor de formas (tecla B)
- **< >**: cambiar entre formas · **+**: nueva forma · **-**: borrar forma.
- **Clic izquierdo** en la cuadrícula: agregar/quitar un bloque de la figura.
- **Clic derecho** en la cuadrícula: mover la mira (el punto donde empieza).
- Los números de la derecha: **profundidad**, **reversa**, **escalón** y **elevación** (para escaleras).
- 🔗 **Compartir**: copia un código de tu forma, o pega un código para cargar una forma.
- ⚙️ **Engrane**: abre la configuración.

---

## ⚙️ Configuración

Desde el menú principal: **Mods → OreExcavation → Config**, o con el engrane del editor de formas.

- **Del jugador**: modo de activación, mostrar contorno, mantener tecla, usar el lado del bloque golpeado.
- **Del mundo** (solo en un jugador): bloques por tick, límite de bloques, alcance, historial de deshacer, hambre y experiencia por bloque, mano vacía, recoger automático, tipos de herramienta, formas, protección de TPS y listas negras.
- **Reload JSON Files** / **Open Config Folder**: archivos avanzados en `config/oreexcavation/` (`block_groups.json`, `tool_overrides.json`, `shapes.json`).

---

## ↩️ Deshacer

`/undo_excavation` regresa los bloques de tu última excavación. Para hacerlo debes devolver los objetos y la experiencia que obtuviste (en creativo no hace falta).
