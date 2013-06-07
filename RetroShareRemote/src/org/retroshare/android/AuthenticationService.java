
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
    private AccountAuthenticator mAuthenticator;

    @Override
    public void onCreate()
    {
    	super.onCreate();
        Log.v(TAG, "Authentication Service started.");
        mAuthenticator = new AccountAuthenticator(this);
    }

    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	Log.v(TAG, "Authentication Service stopped.");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.v(TAG, "getBinder()...  returning the AccountAuthenticator binder for intent " + intent);
        return mAuthenticator.getIBinder();
    }
}
