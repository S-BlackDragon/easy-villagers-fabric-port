---
name: Easy Villagers Fabric Port — Plan y reglas
description: Plan de 3 fases para portar Easy Villagers de NeoForge a Fabric 1.21.1, con reglas de flujo de trabajo
type: project
---

Portando Easy Villagers (NeoForge 1.21.1) a Fabric 1.21.1.

**Why:** El usuario quiere el mod en Fabric con soporte para mods de pipes/automatización.

**How to apply:** Seguir el plan de 3 fases en `plan-easy.txt`. Verificar el plan antes de cualquier cambio. No saltar fases.

## Rutas clave
- Original NeoForge: `c:/Users/alexf/Downloads/easy-villagers-original`
- Proyecto Fabric: `c:/Users/alexf/Downloads/Easyvillager-fabric-port`
- Plan: `c:/Users/alexf/Downloads/Easyvillager-fabric-port/plan-easy.txt`

## Reglas del flujo de trabajo
1. Verificar `plan-easy.txt` antes de hacer cualquier cambio
2. NO pasar a Fase 2 sin que Fase 1 esté probada y aprobada por el usuario
3. NO pasar a Fase 3 sin que Fase 2 esté probada y aprobada por el usuario
4. Al final de cada fase: decirle al usuario qué probar + correr Minecraft para que lo pruebe

## Fases
- **Fase 1**: Fundación — mod carga, bloques aparecen en creative tab, se colocan y rompen sin crash
- **Fase 2**: Lógica core — capturar villager, GUI funciona, breeding/farming/trading funcionan, pipes compatibles
- **Fase 3**: Polish — renderers de villagers dentro de bloques, item renderers, JEI/Jade integrations

## Decisiones técnicas ya tomadas
- Fabric 1.21.1 + Fabric API (incluye Transfer API para pipes)
- SidedInventory + InventoryStorage.of() para compatibilidad con pipe mods
- Cloth Config para ServerConfig/ClientConfig
- ModSoundEvents necesita Mixin (no hay evento equivalente en Fabric)
- SyncableTileentity y todos los renderers son vanilla puro — copiar sin cambios
