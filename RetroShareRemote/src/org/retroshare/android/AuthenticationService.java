
package org.retroshare.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class AuthenticationService extends Service
{
    private static final String TAG = "AuthenticationService";

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.v(TAG, "getBinder()...  returning the AccountAuthenticator binder for intent " + intent);
        return (new AccountAuthenticator(this).getIBinder());
    }
}
