# Nx6RtkHelper
Handle RTK module with NX6P


# Implementation
Dependency

settings.gradle
  - maven { url "https://repo.repsy.io/mvn/handheldgroup/handheldgroup" }
  - maven { url "https://jitpack.io" }

build.gradle
- implementation 'com.github.tfreinberger:Nx6RtkHelper:Tag'


# Setup
```
Nx6RtkHelper.powerOn(true);
Nx6RtkHelper nx6RtkHelper = new Nx6RtkHelper();
nx6RtkHelper.openSerialPath("/dev/ttyHSL1", Baudrate.DEFAULT_F9P, 0);  // DEFAULT_F9P = 38400
```

# Read Nmea Messages
```
nx6RtkHelper.startNmeaReading();
// nx6RtkHelper.stopNmeaReading();

nx6RtkHelper.receiveNmeaSentence(new Nx6RtkHelper.OnNmeaSentenceListener() {
            @Override
            public void onNmeaSentence(String nmeaSentence) {
                Log.i("Nmea Sentence: ", nmeaSentence);
            }
        });
```

# Ublox Configuration
Example:
```
nx6RtkHelper.sendUbxCommand(UbxCommands.enableGNSMessages());
nx6RtkHelper.saveUbxConfig();
```
