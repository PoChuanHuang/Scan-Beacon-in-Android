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
                       
                    case "7C:EC:79:6F:47:64":
                     
                    case "A8:1B:6A:B3:0F:67":
                        average_rssi_A81B6AB30F67.add(rssi);
                        Log.e(TAG_bluetooth,"rssi = "+rssi);
                        t=true;record_A81B6AB30F67++;break;
                    case "D9:D0:82:EB:DC:69":
                        average_rssi_D9D082EBDC69.add(rssi);
                        t=true;break;
                    case "A4:34:F1:8A:1D:B0":
                        average_rssi_D9D082EBDC69.add(rssi);
                        t=true;break;
                    default:
                        Log.d(TAG,"不是我們的bluetooth");
                }
            }
            
 ```
