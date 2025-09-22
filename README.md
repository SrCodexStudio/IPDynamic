# IPDynamic 2.5-OMEGA

**Plugin premium de seguridad para Minecraft con sistema avanzado de detección de alts y bans dinámicos**

## 🚀 Características

- **Bans Dinámicos OP1/OP2**: Banea hasta 65,536 IPs progresivamente
- **Detección de Alts Avanzada**: Algoritmo inteligente para detectar cuentas alternativas
- **Rendimiento Optimizado**: Sistema asíncrono que no causa lag
- **Webhooks Premium**: Integración elegante con Discord
- **Multi-versión**: Compatible con Minecraft 1.13 - 1.21.8
- **Ofuscación Avanzada**: Protección total del código

## 📋 Requisitos

- **Java**: 11 o superior
- **Servidor**: Spigot, Paper, Purpur 1.13+
- **Maven**: 3.6+ (para compilación)
- **Conexión a Internet**: Para descargar dependencias automáticamente

## 🏗️ Compilación

### Compilación Automática con Ofuscación Total:
```bash
# Una sola línea - Maven descarga e integra ProGuard automáticamente
mvn clean package
```

**¡No necesitas instalar nada extra!** Maven maneja todo automáticamente:
- ✅ Descarga ProGuard como dependencia
- ✅ Aplica ofuscación agresiva
- ✅ Genera JAR final ofuscado

### Proceso de Ofuscación:

Durante la compilación con ProGuard, el sistema realizará:

1. **Preparación del Diccionario**: Copia `proguard-dictionary.txt` al directorio target
2. **Ofuscación Agresiva**:
   - Renombra TODOS los packages a letras simples (`a`, `b`, `c`, etc.)
   - Aplana la jerarquía de packages (todo va al package `a`)
   - Renombra todas las clases usando el diccionario personalizado
   - Ofusca métodos y variables con nombres de una sola letra
3. **Archivos Generados**:
   - `target/IPDynamic-2.5-OMEGA.jar` - JAR final ofuscado
   - `target/proguard_mapping.txt` - Mapeo para debugging
   - `target/proguard_configuration.txt` - Configuración utilizada

### Ejemplo de Transformación:

**Antes (Código Original):**
```
me.lssupportteam.ipdynamic.managers.BanManager
me.lssupportteam.ipdynamic.models.PlayerData
me.lssupportteam.ipdynamic.utils.IPUtils
```

**Después (Ofuscado):**
```
a.a (BanManager → clase 'a')
a.b (PlayerData → clase 'b')
a.c (IPUtils → clase 'c')
```

## 📁 Estructura del JAR Ofuscado

```
IPDynamic-2.5-OMEGA.jar
├── a/              # Todos los packages van aquí
│   ├── a.class     # Clase principal ofuscada
│   ├── b.class     # Manager ofuscado
│   ├── c.class     # Listener ofuscado
│   └── ...         # Más clases con nombres de 1-2 letras
├── plugin.yml      # Configuración del plugin
├── config.yml      # Configuración principal
└── META-INF/
    └── MANIFEST.MF
```

## 🎯 Comandos del Plugin

```bash
/ipdy ban op1 127.0.0.* "Spam"        # Ban 256 IPs
/ipdy ban op2 192.168.*.* "Hack"      # Ban 65,536 IPs
/ipdy unban op1 127.0.0.*             # Unban progresivo
/ipdy alts NombreJugador              # Ver alts
/ipdy info NombreJugador              # Info detallada
/ipdy stats                           # Estadísticas
```

## ⚙️ Configuración

### config.yml
```yaml
general:
  language: spanish
  debug-mode: false
  autosave-interval: 10

bans:
  op2-process-delay: 300  # 5 minutos
  max-ips-per-cycle: 1000

alt-detection:
  enabled: true
  notify-admins: true
```

### webhook-config.yml
```yaml
webhooks:
  enabled: true
  urls:
    connection: "https://discord.com/api/webhooks/..."
    alt-detection: "https://discord.com/api/webhooks/..."
    ban: "https://discord.com/api/webhooks/..."
```

## 🔒 Nivel de Protección

La ofuscación con ProGuard proporciona:

- **✅ Renombrado Completo**: Todos los packages, clases y métodos
- **✅ Diccionario Personalizado**: Nombres impredecibles
- **✅ Aplanamiento de Packages**: Estructura simplificada
- **✅ Optimización de Código**: Mejor rendimiento
- **✅ Mapeo para Debug**: Soporte técnico posible

## 📊 Rendimiento

- **4 Hilos Dedicados**: Para operaciones pesadas
- **Cache de 10,000 IPs**: Búsqueda instantánea
- **Procesamiento Progresivo**: Sin lag del servidor
- **Rate Limiting**: 45 requests/min para APIs externas

## 🆘 Soporte

Para debugging del código ofuscado:
1. Consulta `target/proguard_mapping.txt`
2. Usa las herramientas retrace de ProGuard
3. Habilita `debug-mode: true` en la configuración

## 📝 Notas de Desarrollo

- El sistema de ofuscación es **completamente automático**
- Los archivos de configuración NO se ofuscan para mantener compatibilidad
- Las APIs de Bukkit/Spigot se mantienen intactas
- El mapeo permite debugging en producción

---

**Desarrollado por SrCodex | IPDynamic 2.5-OMEGA**