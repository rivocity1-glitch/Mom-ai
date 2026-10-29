# Mom AI

A simple Marathi-first Android voice assistant prototype for Mom.

## Current prototype

- Large, easy-to-find Marathi voice button
- Cleaner, high-contrast interface with large text
- Requests speech recognition in Marathi (mr-IN)
- Shows recognised words and gives a few basic spoken responses
- Checks whether Android speech recognition is available before starting
- Shows helpful feedback if listening fails
- Uses Marathi text-to-speech when installed, with Hindi as a fallback where available

## Not implemented yet

- Individual speaker verification / voice enrollment
- Always-on wake phrase
- Phone calling or phone-wide app control
- Reliable offline speech recognition

Any future calling feature must read out the contact name and ask for explicit confirmation before placing a call. Keep voice recordings and voice profiles off GitHub. Speaker verification is probabilistic and must not be treated as secure authentication.

## Build workflow

The GitHub Actions workflow is **manual-only** for now. Ordinary code changes will not trigger an APK build. Do not select **Run workflow** until you are ready to build and test an APK.

When you decide to build later: open Actions → Build Mom AI APK → Run workflow. After a successful run, download the mom-ai-debug-apk artifact and extract app-debug.apk.

This is a starter prototype, not a production-ready assistant.
