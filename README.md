# Deprecation notice
## !!! This project is no longer maintained !!!

I had started researching how Phenotype DB works as a hobby, and this application was only meant to be a proof of concept.

Making things work required a huge amount of reverse engineering of Google applications, and today I no longer have time to dedicate to it.

I've also been told that the structure of Phenotype DB has been recently changed, so I expect this project to no longer work, or to stop working soon.

If you are looking for a replacement, I suggest you take a look at [GMS-Flags](https://github.com/polodarb/GMS-Flags), a still maintained application created by other users who were part of the community of this project itself.

# GAppsMod (ex GoogleDialerMod)
The ultimate All-In-One Utility to tweak Google applications.


## Downloads:
 - Please visit the [GAppsMod Release Page](https://github.com/jacopotediosi/GAppsMod/releases)


## How do I use it?
- Always make sure you're using the latest beta version of the Google apps you want to tweak to take advantage of the latest features
- Allow root access to GAppsMod, apply any mods you want, then force close and reopen Google apps a few times for them to take effect
- There is no need to keep GAppsMod installed after applying the desired mods, because they (should) survive Google applications updates / reinstalls over time


## How does it work?
In every Android device there is a database, called Phenotype.db, managed by Google Play Services, containing "flags" that affect the behavior of all installed Google applications.

Some of those flags concern applications core functionalities, while others pertain to hidden or upcoming features that have not yet been released.

What GAppsMod does is execute SQLite queries on that database and override the configuration files of Google applications to enable or modify their functionality at will.


## Features:
- Supports all arm / arm64 / x86 / x86_64 devices and all Android versions from 5.0 (Lollipop)
- Enable / disable hidden features for all users at once when Android "multiple users" mode is in use
- Allows users to list and change all Phenotype DB boolean flags for all installed Google applications
- A convenient home screen brings together the suggested mods for the most used Google applications


## Currently suggested mods
- For the **Phone** application ([link](https://play.google.com/store/apps/details?id=com.google.android.dialer)):
    - Force **enable call recording** feature, even on unsupported devices or in unsupported countries ([ref](https://support.google.com/phoneapp/answer/9803950))
        - Enable also **automatic call recording** ("always record") feature based on caller (otherwise only available in India)
    - Silence the annoying "registration has started / ended" **call recording announcements** (only on Phone version <= 94.x)
    - Force **enable call screening** and "revelio" (advanced automatic call screening) features, even on unsupported devices or in unsupported countries ([ref](https://support.google.com/phoneapp/answer/9118387))
        - Allows users to choose the language for call screening
- For the **Messages** application ([link](https://play.google.com/store/apps/details?id=com.google.android.apps.messaging)):
    - Force **enable debug menu** (it can also be enabled without mods by entering `*xyzzy*` in the application's search field)
    - Force **enable message organization** ("supersort")
    - Force **enable marking conversations as unread**
    - Force **enable verified SMS** settings menu ([ref](https://support.google.com/messages/answer/9326240))
    - Force **enable always sending images by Google Photos links in SMS** ([ref](https://9to5google.com/2022/02/19/messages-google-photos/))
    - Force **enable nudges and birthday reminders** ([ref](https://support.google.com/messages/answer/11555591))
    - Force **enable Bard AI draft suggestions** ("magic compose") ([ref](https://9to5google.com/2023/05/05/google-messages-magic-compose-ai/))
    - Force enable smart features: **spotlights suggestions** ([ref](https://9to5google.com/2023/02/02/google-messages-assistant/)), **stickers suggestions**, **smart compose** ([ref](https://9to5google.com/2020/06/30/gboard-android-smart-compose-google-messages/)), **smart actions (smart reply) in notifications**

And much more coming soon :)


## Demo
![Demo GIF](https://github.com/jacopotediosi/GAppsMod/assets/20026524/5b13c935-4b12-46ac-b67d-0182004c8ac0)


## Troubleshooting:
- After enabling / disabling any mod, please force close and reopen a few times the Google application you are trying to mod. You may also need to reboot for the changes to take effect.
- Before to report an issue try to delete Google apps data, to reboot your phone and to try again what didn't work


## Donations
If you really like my work, please consider a donation via [Paypal](https://paypal.me/jacopotediosi) or [Github Sponsor](https://github.com/sponsors/jacopotediosi). Even a small amount will be appreciated.


## Credits:
- Thanks to [Gabriele Rizzo aka shmykelsa](https://github.com/shmykelsa), [Jen94](https://github.com/jen94) and [SAAX by agentdr8](https://gitlab.com/agentdr8/saax) for their [AA-Tweaker](https://github.com/shmykelsa/AA-Tweaker) app, which inspired me making GAppsMod
- [Libsu](https://github.com/topjohnwu/libsu) by [topjohnwu](https://github.com/topjohnwu)
