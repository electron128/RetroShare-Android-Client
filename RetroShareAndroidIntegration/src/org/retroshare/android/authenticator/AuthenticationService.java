/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.retroshare.android.authenticator;

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
        Log.v(TAG, "Authentication Service started.");
        mAuthenticator = new AccountAuthenticator(this);
    }

    @Override
    public void onDestroy()
    {
    	Log.v(TAG, "Authentication Service stopped.");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.v(TAG, "getBinder()...  returning the AccountAuthenticator binder for intent " + intent);
        return mAuthenticator.getIBinder();
    }
}
