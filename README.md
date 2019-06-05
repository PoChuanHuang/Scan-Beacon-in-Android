# Scan-Beacon-in-Android
  This is an app which developed in Android Studio.
  You can use it to scan nearby bluetooth and get some information.
  (E.g UUID Major Rssi)
 In OnBatchScanResults , You can set which beacon's mac is you want.
 ```bluetooth_scan=
 super.onBatchScanResults(results);
            boolean t=false;
            for(ScanResult scanResult : results)
            {
                Double rssi = Double.valueOf(scanResult.getRssi());
                switch (scanResult.getDevice().getAddress())
                {
                    case "5C:31:3E:31:02:85":
                      break;
                    case "7C:EC:79:6F:47:64":
                      break;
                    case "A8:1B:6A:B3:0F:67":
                      break;
                    case "D9:D0:82:EB:DC:69":
                      break;
                    case "A4:34:F1:8A:1D:B0":
                      break;
                    default:
                      Log.d(TAG,"不是我們的bluetooth");
                }
            }
            
 ```
