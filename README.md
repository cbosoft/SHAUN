# SHAUN:
**SH**ell l**AUN**cher

A very cut back, minimal homescreen consisting entirely of an auto-completing application search+launch field --- the "command line". The goal is to create an easily usable, mostly POSIX compliant terminal emulator, usable as a means of accessing apps on Android.

There are some major security limitations on Android apps imposed by Google... which makes sense, but will limit the functionality of the emulator.

# Implemented Features
## Android-y features
  - Launching of apps (with "tab" completion).
        Start typing the name of an app, press enter to find the name of the app, enter again to launch the app.
  - "url": launch url using default app
## POSIX shell apps
  - "cd": change directory relative to current
  - "ls": list contents of current directory

# TODO:
  - Implement some common shell commands (cd, ls, rm, mkdir, cp, mv...)
  - Editable settings
