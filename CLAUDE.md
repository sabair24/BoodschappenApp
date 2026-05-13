# BoodschappenApp — Claude geheugen

## Versiebeheer (BELANGRIJK)
**Altijd versionCode en versionName ophogen in `app/build.gradle.kts` bij elke push met code-wijzigingen.**

- versionCode = geheel getal, elke release +1
- versionName = "major.minor" string (bijv. "1.8" → "1.9")
- Bestand: `app/build.gradle.kts` regels `versionCode` en `versionName`

De app gebruikt Firestore om te checken of er een update is. Als de versionCode niet omhoog gaat, verschijnt er geen update-popup bij de gebruiker.

**Controleer altijd vóór een push of de versie gebumpt is.**

## Git push (KRITISCH — update-popup werkt alleen via main)
Lokale `git push` geeft HTTP 403. Gebruik `mcp__github__push_files` om via de GitHub API te pushen.

**VERPLICHTE VOLGORDE bij elke sessie met code-wijzigingen:**
1. Push alle gewijzigde bestanden naar de feature branch (`claude/continue-app-development-JlS5N`) via `mcp__github__push_files`
2. Push DAARNA dezelfde bestanden ook naar `main` via `mcp__github__push_files` — dit triggert de GitHub Actions build
3. Commit lokaal: `git add <bestanden> && git commit -m "..."`
4. Sync lokaal met remote main: `git fetch origin main && git reset --hard origin/main`

**Waarom beide branches:** De workflow in `.github/workflows/release.yml` draait ALLEEN bij push naar `main`. De feature branch triggert NOOIT de build en NOOIT de Firestore-update. Zonder push naar `main` verschijnt er geen update-popup bij de gebruiker.

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
