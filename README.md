# SHAUN
**SH**ell l**AUN**cher

<p align="center>
Hidden UI ->
![alt text](screenshots/hidden.png "img-hidden-shaun")
![alt text](screenshots/unhidden.png "img-unhidden-shaun")
<- Unhidden UI
</p>
                                                         
A very cut back, minimal homescreen consisting entirely of an auto-completing application search+launch field --- the "command line". The goal is to create an easily usable, mostly POSIX compliant terminal emulator, usable as a means of accessing apps on Android.

There are some major security limitations on Android apps imposed by Google... which makes sense, but will limit the functionality of the emulator.

# Features
Command-line:
  - Launching of apps (with "tab" completion).
        Start typing the name of an app, press enter to find the name of the app, enter again to launch the app.
    - use "url" to view a webpage with the default web browser
  - Can browse the filesystem:
    - "cd": change directory relative to current
    - "ls": list contents of current directory
Home screen:
  - As a home screen, the app does what you want.
    - Clean UI design, can quickly see the time 
        (phones are pretty much expensive watches half the time)
    - Apps launched with the stroke of but a few keys (thanks to the command line)
