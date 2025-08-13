public class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                // Do something when boot is completed
                Log.d("BootReceiver", "Device booted successfully!");
            }
        }
    }