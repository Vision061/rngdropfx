# RNG Drop FX (Fabric, Minecraft 26.1.2)

Client-Mod für Hypixel SkyBlock: Wenn du einen krassen RNG-Drop bekommst
(z. B. Chimera, Digested Mosquito, Primordial Eye), gibt es eine Animation
mit Bildschirm-Blitz, Lichtstrahlen, Regenbogen-Titel, Konfetti, Feuerwerk und Sounds.

## Bauen (auf deinem PC)
1. **Java 25 (JDK)** installieren, z. B. von https://adoptium.net
2. Den Ordner entpacken, darin ein Terminal öffnen und ausführen:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. Die fertige Mod liegt danach in `build/libs/rngdropfx-1.0.0.jar`

**Ohne Java-Installation:** Den Ordner in ein GitHub-Repo hochladen – die
GitHub-Action baut die Mod automatisch, die .jar gibt es unter *Actions → Artifacts*.

## Installieren
- Fabric Loader für 26.1.2 + **Fabric API** installieren
- `rngdropfx-1.0.0.jar` in den `mods`-Ordner legen

## Animationen testen
Drück **K** im Spiel (änderbar unter *Optionen → Steuerung → RNG Drop FX*) oder tippe `/rngfx menu`.
Im Test-Menü kannst du:
- die große und kleine Animation direkt abspielen
- die **Drop-Stufe** wählen (z. B. `CRAZY RARE DROP!`) und dann auf ein **Item** klicken –
  die Mod baut daraus eine echte Hypixel-Chatnachricht und schickt sie durch die Erkennung.
  So siehst du genau, was bei einem echten Drop passieren würde.
- die Mod an/aus schalten

## Befehle
| Befehl | Was passiert |
|---|---|
| `/rngfx menu` | Test-Menü öffnen |
| `/rngfx test` | große Animation testen |
| `/rngfx test <Item>` | Drop von einem bestimmten Item simulieren (mit Tab-Vervollständigung) |
| `/rngfx simulate <Nachricht>` | komplette Chatnachricht testen, z. B. `/rngfx simulate CRAZY RARE DROP! (Chimera I)` |
| `/rngfx testsmall` | kleine Animation testen |
| `/rngfx toggle` | Mod an/aus |
| `/rngfx reload` | Config neu laden |

## Einstellungen – `config/rngdropfx.json`
- `epicItems` – Items, die **immer** die große Animation auslösen (eigene einfach hinzufügen)
- `epicTiers` – Drop-Stufen für die große Animation (`PRAY TO RNGESUS DROP!`, `INSANE DROP!` …)
- `goodTiers` – Stufen für die kleine Animation (`CRAZY RARE DROP!`)
- `animateCrazyRareDrops` – kleine Animation an/aus
- `volume`, `durationSeconds`

Die Mod liest nur den Chat und zeichnet aufs HUD – sie verändert nichts am Spiel,
also im Rahmen von Hypixels erlaubten QoL-Mods.
