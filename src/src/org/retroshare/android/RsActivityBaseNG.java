package org.retroshare.android;

import org.retroshare.android.RsService.RsBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


/**
 * @author G10h4ck
 * This class is aimed to be inherited by Activityes that needs to communicate with RsService
 * provide out of the box almost all needed stuff to communicate with RsService
 * so each activity doesn't need to handle all this common stuff
 */
public abstract class RsActivityBaseNG extends Activity
{
    private static final String TAG="RsActivityBaseNG";

    protected RsService mRsService;
    protected boolean mBound = false;

    private class RsServiceConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            RsBinder binder = (RsBinder) service;
            mRsService = binder.getService();
            mBound = true;
            Log.v(TAG, "onServiceConnected");
            RsActivityBaseNG.this.onServiceConnected();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mBound = false;
            Log.v(TAG, "onServiceDisconnected");
        }
    };

    private RsServiceConnection mConnection = null;
    private void _bindRsService()
    {
        if(mConnection == null)
        {
            mConnection = new RsServiceConnection();
            Intent intent = new Intent(this, RsService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }
    private void _unBindRsService() { if(mConnection != null && mBound) unbindService(mConnection); }


    /**
     * This method should be overridden by child classes that want to do something between Activity.onCreate and connection initialization it is guaranteed to be executed before onServiceConnected
     * It is suggested for inflating your activity layout, so you are sure that your widget are in the right place when onServiceConnected() is called
     */
    protected void onCreateBeforeConnectionInit(Bundle savedInstanceState)
    {}

    /**
     * This method should be overridden by child classes that want to do something when connection to RsService is available.
     */
    protected void onServiceConnected()
    {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        onCreateBeforeConnectionInit(savedInstanceState);
        _bindRsService();
    }

    @Override
    public void onDestroy()
    {
        _unBindRsService();
        super.onDestroy();
    }
}