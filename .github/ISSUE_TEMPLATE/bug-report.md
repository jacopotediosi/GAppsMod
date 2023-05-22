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

- Is the Google app you are trying to tweak (e.g., Phone by Google) installed as system app: yes/no

- Installed Magisk / other SU Manager version: 

[NOTE]: # ( Paste below the output of the `adb shell "dumpsys package com.jacopomii.gappsmod | grep version"` command )
- Installed GAppsMod version:


[NOTE]: # ( Paste below the output of the `adb shell "dumpsys package REPLACE_WITH_PACKAGENAME | grep version"` command )
- The version of the Google app you are trying to tweak (e.g., Phone by Google):


[NOTE]: # ( Paste below the output of the `adb shell "getprop | grep locale"` command )
- Your device language (locale):


[NOTE]: # ( Paste below the output of the `adb shell "getprop | grep iso-country"` command )
- Your location (country of the SIM and country where you are):


## Logcat
[NOTE]: # ( 
Launch the Google app you are trying to tweak (e.g., Phone by Google) in Debug mode using the `adb shell "am start -D REPLACE_WITH_PACKAGENAME"` command.
Open another terminal and use the `adb logcat > logs.txt` command to start capturing logs.
Perform the necessary steps to replicate the bug, then press CTRL+C to stop capturing logs.
Attach below the resulting logs.txt file.
)
