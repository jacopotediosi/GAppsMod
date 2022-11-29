---
name: Bug Report
about: Report something that isn't working
title: ''
labels: bug
assignees: ''

---

## Overview
[NOTE]: # ( Give a BRIEF summary about your problem )


## Steps to Reproduce
[NOTE]: # ( Provide a simple set of steps to reproduce this bug. )
1.  
2.  
3.  

## Expected Behavior
[NOTE]: # ( Tell us what you expected to happen )


## Actual Behavior
[NOTE]: # ( Tell us what actually happens )


## Screenshots
[NOTE]: # ( If applicable, add screenshots to help explain your problem. )


## System Information
- Device and model: 

- ROM and Android version: 

- Is Google Dialer installed as system app: 

- Installed Magisk / other SU Manager version: 

[NOTE]: # ( Paste below the output of the `adb shell "dumpsys package com.jacopomii.googledialermod | grep version"` command )
- Installed GoogleDialerMod version:


[NOTE]: # ( Paste below the output of the `adb shell "dumpsys package com.google.android.dialer | grep version"` command )
- Installed Google Dialer version:


[NOTE]: # ( Paste below the output of the `adb shell "getprop | grep locale"` command )
- Your device language (locale):


[NOTE]: # ( Paste below the output of the `adb shell "getprop | grep iso-country"` command )
- Your location (country of the SIM and country where you are):


## Logcat
[NOTE]: # ( 
Launch the Dialer in Debug mode using the `adb shell "am start -D com.google.android.dialer"` command.
Open another terminal and use the `adb logcat > logs.txt` command to start capturing logs.
Perform the necessary steps to replicate the bug, then press CTRL+C to stop capturing logs.
Attach below the resulting logs.txt file.
)
