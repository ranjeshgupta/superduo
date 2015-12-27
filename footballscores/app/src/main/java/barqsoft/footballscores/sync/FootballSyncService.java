package barqsoft.footballscores.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Nish on 24-12-2015.
 */
public class FootballSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static FootballSyncAdapter sFootballSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("FootballSyncService", "onCreate - FootballSyncService");
        synchronized (sSyncAdapterLock) {
            if (sFootballSyncAdapter == null) {
                sFootballSyncAdapter = new FootballSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sFootballSyncAdapter.getSyncAdapterBinder();
    }
}
