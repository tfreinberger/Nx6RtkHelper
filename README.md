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
// nx6RtkHelper.closeSerialPath();
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

# RTK Client
*Source Table*
Get your Sourcetable by searching with address and port. (Address can be IP or Domain)

Use Domain without (http/s://) e.g. Address: euref-ip.net & Port: 2101 

Requires Permission: 
``` <uses-permission android:name="android.permission.INTERNET"/> ```
```
RtkSourceTable rtkSourceTable = new RtkSourceTable();
rtkSourceTable.searchForSourceTable("address", port);
rtkSourceTable.getSourceTableList(new RtkSourceTable.OnSourceTableList() {
            @Override
            public void onSourceTableListToString(String sourceTable) {
                Log.i("SourceTable: ", sourceTable);
            }
        });
```
Find Mountpoints of Source type Stream.
```
rtkSourceTable.getSourceTypeStream(new RtkSourceTable.OnSourceTypeStream() {
            @Override
            public void onSourceType(String mountPoint) {
                Log.i("Mountpoint: ", mountPoint);
            }
        });
```
