# GoogleDialerMod
The ultimate All-In-One Utility to tweak Google Dialer behaviour.

# How do I use it?
Allow root access, choose what you want, open / force close Google Dialer app a couple of times, and then forget about it :)
Always make sure you are using the latest Google Dialer beta from the Play Store to enjoy the latest features.
There is no need to keep GoogleDialerMod installed after applying the desired changes, because they (should) survive Google Dialer updates / reinstalls over time.

# How does it work?
This app uses SQLite commands to override some flags related to Google Dialer into the Google Play Services. 
Google Play Services control a lot of features inside Google Dialer. Some of them are core functionality of Google Dialer, some of them are upcoming feature that are simply not yet released.
What this app does is making some SQLite queries in order to alter some features of Google Dialer.

# Current features:
- Supports arm / arm64 / x86 / x86_64 devices
- Enable / disable hidden features for all users when "multiple users" Android mode in enabled
- Force enable call recording feature even with unsupported devices or in unsupported countries
- Silence annoying call recording "registration has started / ended" sounds

And much more coming soon :)

# Troubleshooting:
- After enabling / disabling any switch, please force close and reopen the Google Dialer app via Android settings a couple of times.<br>You may also need to reboot for the changes to take effect.
- Before to report an issue try to delete Google Dialer app data, to reboot your phone and to try again what didn't work
- If the app really doesn't seem to work for you, please try the [Magisk module](https://github.com/jacopotediosi/GoogleDialerMod-Magisk/releases) as well

# Downloads:
 - ## [GoogleDialerMod](https://github.com/jacopotediosi/GoogleDialerMod/releases)
 - ## [GoogleDialerMod-Magisk](https://github.com/jacopotediosi/GoogleDialerMod-Magisk/releases) (Alternative to this app when it really doesn't seem to work)

# Credits:
[Gabriele Rizzo aka shmykelsa](https://github.com/shmykelsa), [Jen94](https://github.com/jen94) and [SAAX by agentdr8](https://gitlab.com/agentdr8/saax) for the AA-Tweaker app which inspired me making GoogleDialerMod
