# Tact

Une télécommande Android TV réduite à l'essentiel : une molette, quelques touches, un clavier qui écrit en direct sur la TV.

Tact parle directement au téléviseur sur le Wi-Fi. Pas de compte, pas de publicité, rien ne quitte ton réseau local.

[Voir la démo (50 s)](demo/tact-demo-sound.mp4)

## Fonctions

- **Appairage par code** : le téléviseur affiche un code à 6 caractères, une seule fois. Ensuite l'app se reconnecte seule.
- **Molette** : appui sur un quart pour une flèche, au centre pour OK, maintien pour répéter. Une LED s'allume dans la direction pressée.
- **Touches** : marche/arrêt, retour, accueil, lecture/pause, muet, volume + / − (le maintien répète).
- **Clavier en direct** : chaque lettre tapée sur le téléphone apparaît tout de suite dans le champ de la TV. Valider appuie sur Entrée.
- **Plusieurs téléviseurs** : le nom en haut ouvre la liste. Les TV du réseau sont détectées automatiquement (mDNS).
- **Reconnexion en un appui** : le bouton rond à côté du nom, ambre quand la connexion est perdue.

## Installer

Télécharge l'APK dans les [releases](../../releases), puis ouvre-le sur ton téléphone Android (8.0 ou plus). L'APK est signé avec une clé personnelle : Android te demandera d'autoriser l'installation depuis cette source.

## Compiler

Il faut un JDK 17 ou plus et le SDK Android (API 35).

```bash
gradle :app:testDebugUnitTest :app:assembleRelease
```

Crée un fichier `local.properties` avec `sdk.dir=<chemin du SDK Android>`. L'APK sort dans `app/build/outputs/apk/release/` (non signé : signe-le avec ta propre clé).

## Comment ça marche

Tact implémente le protocole Android TV Remote v2 sans aucune bibliothèque externe :

- **Appairage** (port 6467, TLS mutuel) : échange de messages protobuf, puis un secret calculé avec SHA-256 sur les clés publiques et le code affiché.
- **Commande** (port 6466, TLS mutuel) : touches, saisie de texte par le canal de saisie de la TV, réponses aux pings.
- Le codec protobuf et le certificat auto-signé sont écrits à la main (`proto/Proto.kt`, `atv/SelfSignedCert.kt`).
- L'interface est en Jetpack Compose, avec des icônes dessinées en code.

## Licence

MIT, voir [LICENSE](LICENSE).

## Remarques

Tact est un projet indépendant, sans lien avec Google. « Android TV » est une marque de Google LLC.
