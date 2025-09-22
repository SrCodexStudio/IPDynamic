# 🛡️ IPDynamic 2.5-OMEGA

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.13--1.21.8-green.svg)](https://www.minecraft.net/)
[![Spigot](https://img.shields.io/badge/Spigot-Compatible-yellow.svg)](https://www.spigotmc.org/)
[![Discord](https://img.shields.io/badge/Discord-Integration-blue.svg)](https://discord.com/)

**Advanced security plugin for Minecraft servers with intelligent alt detection, dynamic IP banning, and comprehensive Discord integration.**

---

## 🚀 Key Features

### 🔒 **Advanced Security System**
- **Dynamic OP1/OP2 Bans**: Ban up to 65,536 IPs progressively without server lag
- **Intelligent Alt Detection**: Advanced algorithm to detect alternative accounts
- **IP Whitelist System**: Bypass bans for trusted players
- **Geolocation Integration**: Track player locations and detect proxies/VPNs

### 🤖 **Discord Bot Integration**
- **Real-time Stats**: Live country statistics updates every 5 minutes
- **Automatic Notifications**: Alt detection, bans, and admin connections
- **Message Editing**: Anti-spam system that edits existing messages
- **Modular Configuration**: Separate addons system for Discord and stats

### 🌍 **Multi-Language Support**
- **English**: Complete translation with help menus
- **Spanish**: Native language support with ASCII art menus
- **Russian**: Full Cyrillic support with proper formatting
- **Extensible**: Easy to add more languages

### ⚡ **Performance Optimized**
- **Asynchronous Processing**: No server lag with 4 dedicated threads
- **Smart Caching**: 10,000 IP cache for instant lookups
- **Progressive Processing**: Large ban operations handled incrementally
- **Memory Efficient**: Optimized data structures and JSON handling

---

## 📋 System Requirements

- **Java**: 11 or higher
- **Server**: Spigot, Paper, Purpur 1.13+
- **Maven**: 3.6+ (for compilation)
- **Internet**: For dependency downloads and GeoIP services

---

## 🏗️ Quick Start

### Compilation with ProGuard Protection:
```bash
# Single command - Maven handles everything automatically
mvn clean package
```

**No additional setup required!** Maven automatically:
- ✅ Downloads ProGuard dependencies
- ✅ Applies aggressive obfuscation
- ✅ Generates protected final JAR

### Installation:
1. Download the JAR from `target/IPDynamic-2.5-OMEGA.jar`
2. Place in your server's `plugins/` folder
3. Restart the server
4. Configure `config.yml` and addons as needed

---

## 🎮 Commands & Permissions

### Core Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/ipdy help` | Show command help menu | `ipdynamic.use` |
| `/ipdy ban op1 <IP> [reason]` | Ban 256 IPs (/24 range) | `ipdynamic.ban.op1` |
| `/ipdy ban op2 <IP> [reason]` | Ban 65,536 IPs (/16 range) | `ipdynamic.ban.op2` |
| `/ipdy unban op1/op2 <IP>` | Remove IP bans progressively | `ipdynamic.unban` |
| `/ipdy alts <player>` | View player's alternative accounts | `ipdynamic.alts` |
| `/ipdy info <player>` | Detailed player information | `ipdynamic.info` |
| `/ipdy stats` | Plugin statistics | `ipdynamic.stats` |
| `/ipdy whitelist <add\|remove\|list> [player]` | Manage IP ban whitelist | `ipdynamic.whitelist` |
| `/ipdy reload` | Reload plugin configuration | `ipdynamic.reload` |
| `/ipdy discord status/stats` | Discord bot management | `ipdynamic.admin` |

### Example Usage
```bash
# Ban an IP range for spam
/ipdy ban op1 192.168.1.100 "Spamming chat"

# Check player's alternative accounts
/ipdy alts Notch

# Add player to whitelist (bypass IP bans)
/ipdy whitelist add Steve

# View comprehensive player information
/ipdy info Alex
```

---

## ⚙️ Configuration

### 📁 File Structure
```
plugins/IPDynamic/
├── config.yml              # Main configuration
├── webhook-config.yml       # Discord webhook settings
├── addons/
│   ├── discord.yml         # Discord bot configuration
│   └── stats.yml           # Statistics embed settings
├── lang/
│   ├── english.yml         # English language pack
│   ├── spanish.yml         # Spanish language pack
│   └── russian.yml         # Russian language pack
└── data/
    ├── playerConnections.json
    ├── single-bans.json
    ├── op1-bans.json
    ├── op2-bans.json
    └── whitelist.json
```

### 🛠️ Main Configuration (config.yml)
```yaml
general:
  language: english          # spanish, english, russian
  debug-mode: false
  autosave-interval: 10      # minutes

bans:
  default-reason: "Suspicious behavior"
  op1-process-delay: 0       # immediate
  op2-process-delay: 300     # 5 minutes
  max-ips-per-cycle: 1000    # prevent lag

alt-detection:
  enabled: true
  notify-admins: true
  min-shared-ips: 1
  whitelist-immune: true

geoip:
  enabled: true
  provider: "ip-api.com"
  cache-duration: 3600       # 1 hour
```

### 🤖 Discord Bot Setup (addons/discord.yml)
```yaml
enabled: true
bot:
  token: "YOUR_BOT_TOKEN_HERE"
  activity: "IPDynamic 2.5-OMEGA"
  status: "ONLINE"

server:
  guild-id: "YOUR_GUILD_ID_HERE"
  logs-channel-id: "YOUR_LOGS_CHANNEL_ID_HERE"

connection:
  auto-reconnect: true
  reconnect-delay: 5
  max-reconnect-attempts: 10
```

### 📊 Statistics Configuration (addons/stats.yml)
```yaml
enabled: true
auto-send:
  enabled: true
  interval: 300              # 5 minutes
  channel-id: "YOUR_STATS_CHANNEL_ID"

embed:
  title: "➜ Top 10 Most Active Countries"
  color: "#00FF00"

templates:
  country-entry: "{medal} **{country}** {flag} ➜ `{connections:,}` connections (**{percentage}%**)"
```

---

## 🔧 Advanced Features

### 🌍 **Geolocation & Security**
- **Real-time IP Geolocation**: Powered by ip-api.com
- **Proxy/VPN Detection**: Automatic alerts for suspicious connections
- **Country-based Statistics**: Live Discord updates showing top player countries
- **IP History Tracking**: Complete connection history per player

### 🔍 **Alt Detection Algorithm**
- **Shared IP Analysis**: Intelligent detection of alternative accounts
- **Admin Notifications**: Real-time alerts for suspicious activity
- **Whitelist Integration**: Trusted players bypass alt detection
- **Historical Data**: Track alt patterns over time

### 🚫 **Dynamic Ban System**
- **Progressive Processing**: Large bans applied without server lag
- **OP1 Bans**: Target /24 subnets (256 IPs)
- **OP2 Bans**: Target /16 subnets (65,536 IPs)
- **Smart Scheduling**: Configurable delays to prevent overload

### 📱 **Discord Integration Features**
- **Live Statistics**: Real-time country leaderboard updates
- **Message Editing**: Anti-spam system for clean channels
- **Webhook Support**: Multiple notification types
- **Automatic Reconnection**: Robust connection management

---

## 🎨 Multi-Language Support

### Language Features
- **Complete Translations**: All messages, menus, and help text
- **ASCII Art Menus**: Localized command help with visual formatting
- **Cultural Adaptation**: Proper character encoding for all languages
- **Extensible System**: Easy to add new languages

### Supported Languages
| Language | Code | Status | Features |
|----------|------|--------|----------|
| English | `english` | ✅ Complete | Full ASCII menus, help system |
| Spanish | `spanish` | ✅ Complete | Native support, full localization |
| Russian | `russian` | ✅ Complete | Cyrillic support, proper encoding |

### Example: Help Menu in Multiple Languages

**English:**
```
╭─────────────── IPDynamic 2.5-OMEGA ───────────────╮
│  • /ipdy help           ➜  Show this menu        │
│  • /ipdy alts <player>  ➜  View alternate accounts │
│  • /ipdy whitelist <cmd> ➜  Manage whitelist       │
╰───────────────────────────────────────────────────╯
```

**Russian:**
```
╭─────────────── IPDynamic 2.5-OMEGA ───────────────╮
│  • /ipdy help           ➜  Показать это меню     │
│  • /ipdy alts <игрок>  ➜  Посмотреть альты      │
│  • /ipdy whitelist <cmd> ➜  Управлять белым списком│
╰───────────────────────────────────────────────────╯
```

---

## 📊 Performance Metrics

### System Performance
- **Thread Pool**: 4 dedicated threads for heavy operations
- **Memory Cache**: 10,000 IP entries for instant lookups
- **JSON Processing**: Optimized file I/O with atomic operations
- **Rate Limiting**: 30 requests/minute for external APIs

### Benchmark Results
- **OP1 Ban Processing**: ~1,000 IPs in <100ms
- **OP2 Ban Processing**: 65,536 IPs in 5-10 minutes (progressive)
- **Alt Detection**: <50ms per player check
- **Discord Updates**: <200ms per message edit

### Optimization Features
- **Asynchronous Operations**: No main thread blocking
- **Progressive Processing**: Large operations split into chunks
- **Smart Caching**: Frequently accessed data kept in memory
- **Efficient Data Structures**: Optimized for search and iteration

---

## 🔒 Security & Protection

### Code Protection (ProGuard)
```
Original Structure:
me.lssupportteam.ipdynamic.managers.BanManager
me.lssupportteam.ipdynamic.models.PlayerData

Obfuscated Result:
a.a (BanManager)
a.b (PlayerData)
```

### Protection Features
- **✅ Complete Package Renaming**: All packages flattened to simple letters
- **✅ Class Obfuscation**: Custom dictionary with unpredictable names
- **✅ Method/Field Renaming**: Single-letter names for all members
- **✅ Code Optimization**: Improved performance through optimization
- **✅ Debug Mapping**: Retraceable for support purposes

### Security Measures
- **IP Ban Evasion Protection**: Multiple ban types and ranges
- **Alt Account Detection**: Advanced algorithms to catch ban evaders
- **Whitelist System**: Trusted player bypass mechanism
- **Audit Logging**: Complete action tracking for administrators

---

## 🤝 Contributing

### Development Setup
1. **Clone Repository**: `git clone <repository-url>`
2. **Install Dependencies**: `mvn clean install`
3. **Development Build**: `mvn compile` (no obfuscation)
4. **Production Build**: `mvn package` (with obfuscation)

### Code Guidelines
- **Java 11+** compatibility required
- **Async-first** approach for heavy operations
- **Null-safe** coding practices
- **Comprehensive** error handling

### Testing
```bash
# Run tests
mvn test

# Integration tests with test server
mvn verify
```

---

## 📝 Changelog

### Version 2.5-OMEGA
- ✅ **NEW**: Discord Bot Integration with real-time stats
- ✅ **NEW**: Multi-language support (English, Spanish, Russian)
- ✅ **NEW**: Whitelist system for trusted players
- ✅ **NEW**: Addons architecture for modular configuration
- ✅ **IMPROVED**: Performance optimization and console spam reduction
- ✅ **IMPROVED**: Enhanced alt detection algorithms
- ✅ **IMPROVED**: Comprehensive error handling and logging
- ✅ **FIXED**: Various stability and compatibility issues

---

## 🆘 Support & Documentation

### Getting Help
- **Configuration Issues**: Check the example configs above
- **Discord Setup**: Ensure bot has proper permissions
- **Performance Problems**: Enable debug mode for detailed logging
- **Language Issues**: Verify language setting in config.yml

### Debugging
```yaml
# Enable debug mode for detailed logging
general:
  debug-mode: true

# Discord debug (addons/discord.yml)
debug:
  enabled: true
  log-level: "INFO"
```

### Troubleshooting
| Issue | Solution |
|-------|----------|
| Plugin not loading | Check Java version (11+ required) |
| Discord bot offline | Verify token and permissions |
| Bans not working | Check IP format and patterns |
| High memory usage | Reduce cache size in config |

---

## 📄 License

This project is proprietary software. All rights reserved.

**IPDynamic 2.5-OMEGA** - Professional Minecraft Security Solution

---

<div align="center">

**🛡️ Developed by SrCodex | Advanced Minecraft Security Solutions**

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)
![Discord](https://img.shields.io/badge/Discord-7289DA?style=for-the-badge&logo=discord&logoColor=white)

</div>