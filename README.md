# Mom AI

A simple Marathi-first Android voice assistant starter for Mom.

## First version
- Large Marathi microphone button
- Uses Android's available Marathi speech-recognition service
- Shows recognised speech and speaks simple responses when Marathi TTS is available
- GitHub Actions workflow builds a debug APK in the cloud

## Not implemented yet
- Individual speaker verification / voice enrollment
- Always-on wake phrase
- Actual phone calling or phone-wide app control

Any future calling feature must read out the contact name and ask for explicit confirmation before calling. Keep voice recordings and voice profiles off GitHub. Speaker verification is probabilistic, not secure authentication.

## Build APK
Open Actions → Build Mom AI APK → Run workflow. After a successful run, download the mom-ai-debug-apk artifact and extract app-debug.apk.

This is a starter prototype, not a production-ready assistant.
