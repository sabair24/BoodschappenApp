# BoodschappenApp — Claude geheugen

## Versiebeheer (BELANGRIJK)
**Altijd versionCode en versionName ophogen in `app/build.gradle.kts` bij elke push met code-wijzigingen.**

- versionCode = geheel getal, elke release +1
- versionName = "major.minor" string (bijv. "1.8" → "1.9")
- Bestand: `app/build.gradle.kts` regels `versionCode` en `versionName`

De app gebruikt Firestore om te checken of er een update is. Als de versionCode niet omhoog gaat, verschijnt er geen update-popup bij de gebruiker.

**Controleer altijd vóór een push of de versie gebumpt is.**

## Git push
Lokale `git push` geeft HTTP 403. Gebruik altijd `mcp__github__create_or_update_file` of `mcp__github__push_files` om direct via de GitHub API te pushen. Daarna: `git fetch origin main && git reset --hard origin/main` om lokaal te synchroniseren.

## Build pipeline
- GitHub Actions bouwt automatisch bij elke push naar `main`
- Bouwtijd ~10 minuten
- Na de build wordt Firestore bijgewerkt via `scripts/update_firestore_ci.js`
- Gebruiker opent app opnieuw → update-popup verschijnt

## Communicatie
**Altijd een korte samenvatting geven als een taak klaar is:**
- Wat er gedaan is
- Welke versie gebouwd wordt
- Of de gebruiker moet wachten op de build
