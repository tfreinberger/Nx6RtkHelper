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
InputStream inputStream = nx6RtkHelper.getInputStream();
OutputStream outputStream = nx6RtkHelper.getOutputStream()
```
# Read Nmea Messages
```
ReadNmea getNMEA = new ReadNmea();
getNMEA.start();

class ReadNmea extends Thread {

        byte[] buffer = new byte[4096];
        int index = 0;

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    int b = inputStream.read();
                    buffer[index] = (byte) b;
                    if (buffer[index] == '\n') {
                        String nmeaSentence = new String(buffer, 0, index, StandardCharsets.ISO_8859_1);
                        Arrays.fill(buffer, 0, index, (byte) 0);
                        index = 0;
                    } else {
                        index++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
```

