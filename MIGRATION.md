# Migración: P2P QR → Syncthing


## Cómo usar Syncthing

### Desktop
1. Instalá Syncthing: https://syncthing.net/downloads/
2. Abrí la interfaz web (http://127.0.0.1:8384)
3. Agregá la carpeta que querés compartir (por defecto `~/GestorGastos/sync`)
4. Pareá con el otro dispositivo usando el Device ID de Syncthing
5. Compartí esa misma carpeta con el otro dispositivo

### Android
1. Instalá Syncthing-Fork desde F-Droid o Google Play
2. Configurá la misma carpeta compartida que usás en Desktop
3. Asegurate de que la ruta en la app coincida con la que Syncthing-Fork gestiona

## Flujo de uso

```
Dispositivo A                           Dispositivo B
─────────────                           ─────────────
Tocar "Exportar datos"
  ↓
gastos_sync.json escrito
en ~/GestorGastos/sync/
  ↓
Syncthing sincroniza
automáticamente                →         gastos_sync.json aparece
                                          en ~/GestorGastos/sync/
                                                  ↓
                                          Tocar "Importar datos"
                                                  ↓
                                          Datos fusionados (upsert)
                                                  ↓
                                          Disponible "Deshacer sync"
                                          (durante la sesión)
```
