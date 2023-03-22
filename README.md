# GoogleDialerMod
The ultimate All-In-One Utility to tweak Google Dialer.


## Downloads:
 - Please visit the [GoogleDialerMod Release Page](https://github.com/jacopotediosi/GoogleDialerMod/releases)


## How do I use it?
- Always make sure you are using the latest Google Dialer beta from the Play Store to enjoy the latest features
- Allow root access to GoogleDialerMod, apply the tweaks you want, then force close Google Dialer app and reopen it a couple of times for them to take effect
- There is no need to keep GoogleDialerMod installed after applying the desired mods, because they (should) survive Google Dialer updates / reinstalls over time


## How does it work?
In every Android device there is a database, called Phenotype.db, managed by Google Play Services, containing special flags that affect the behavior of all Google applications installed.

Some of those flags concern Google Dialer core functionalities, while others are about hidden or upcoming features not yet released.

What this app does is execute SQLite queries on that database and overwrite Google Dialer configuration files to enable or alter its features at will.


## Current features:
- Supports arm / arm64 / x86 / x86_64 devices and (hopefully) all Android versions
- Enable / disable hidden features for all users at once when Android "multiple users" mode is in use
- Force enable call recording feature even on unsupported devices or in unsupported countries
    - Enable also automatic call recording feature based on caller (otherwise only available in India)
- Silence the annoying "registration has started / ended" call recording sounds (only on Dialer version <= 94.x)
- Force enable call screening and revelio (advanced automatic call screening) even on unsupported devices or in unsupported countries
    - Allows users to choose the language for call screening

And much more coming soon :)


## Troubleshooting:
- After enabling / disabling any switch, please force close and reopen the Google Dialer app a couple of times. You may also need to reboot for the changes to take effect.
- Before to report an issue try to delete Google Dialer app data, to reboot your phone and to try again what didn't work


## Credits:
- Thanks to [Gabriele Rizzo aka shmykelsa](https://github.com/shmykelsa), [Jen94](https://github.com/jen94) and [SAAX by agentdr8](https://gitlab.com/agentdr8/saax) for their [AA-Tweaker](https://github.com/shmykelsa/AA-Tweaker) app, which inspired me making GoogleDialerMod
- [Libsu](https://github.com/topjohnwu/libsu) by [topjohnwu](https://github.com/topjohnwu)
