# Nx6RtkHelper
Handle RTK module with NX6P


# Implementation
*Dependency*

settings.gradle
  - maven { url "https://repo.repsy.io/mvn/handheldgroup/handheldgroup" }
  - maven { url "https://jitpack.io" }

build.gradle
- implementation 'com.github.tfreinberger:Nx6RtkHelper:Tag' 


# Setup
```
Nx6RtkHelper.powerOn(true);
// Nx6RtkHelper.powerOn(false);
Nx6RtkHelper nx6RtkHelper = new Nx6RtkHelper();
nx6RtkHelper.openSerialPath("/dev/ttyHSL1", Baudrate.DEFAULT_F9P, 0);  // DEFAULT_F9P = 38400
// nx6RtkHelper.closeSerialPath();
```
### Baudrate
> Tip: Use the module with Baudrate 115200 for better RTK performance! -> See Ublox Configuration.

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
*Example:*
```
nx6RtkHelper.sendUbxCommand(UbxCommands.setBaudRate115200);
nx6RtkHelper.sendUbxCommand(UbxCommands.enableGNSMessages());
nx6RtkHelper.sendUbxCommand(UbxCommands.enableGSTMessages());
.
.
nx6RtkHelper.saveUbxConfig();
```

# RTK Client
**Source Table**

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
Get back Mountpoint list.
```
ArrayList<String> mpList = rtkSourceTable.getMpList();
```


**Connect to Source**

Connect to a NTRIP Source by using your Credentials.
```
RtkEngine rtkEngine = new RtkEngine();
NtripParameter ntripParameter = new NtripParameter("address", port, "mountpoint", "username", "password");
rtkEngine.setNtripParameter(ntripParameter);
rtkEngine.start(OnDataReceivedListener, OnStatusChangeListener);  
```
## Attention

To receive correction data, you have to send NMEA GGA messages to the Source.

*Example:*
```
nx6RtkHelper.receiveNmeaSentence(new Nx6RtkHelper.OnNmeaSentenceListener() {
            @Override
            public void onNmeaSentence(String nmeaSentence) {
                Log.i("Nmea Sentence: ", nmeaSentence);
                if (nmeaSentence.startsWith("$GNGGA")) {
                    rtkEngine.sendGGA(nmeaSentence + "\n");
                }
            }
        });
```
*Callbacks:*

After sending valid GGA sentences, rtk corrections are receiving. 
Send the rtk corrections to the module like this.
The module will automatically handle it and you are able to receive corrected NMEA data.
```
@Override
    public void onRTKData(byte[] data, int size) {
        nx6RtkHelper.sendRTKData(data, size);
    }

    @Override
    public void onChange(Status status, String... msg) {
        switch (status) {
            case CONNECTED:
                ...
                break;
        }
    }
```




