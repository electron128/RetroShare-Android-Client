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
 * This class is aimed to be inherited by all Activity that needs to communicate with RsService
 * provide out of the box all needed stuff to communicate with RsService
 * so each activity doesn't need to handle all this common stuff
 */
public abstract class RsActivityBase extends Activity
{
	protected RsService mRsService;
	protected boolean mBound = false;
	
	private static final String TAG="RsActivityBase";
	
    private class RsServiceConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            RsBinder binder = (RsBinder) service;
            mRsService = binder.getService();
            mBound = true;
            Log.v(TAG, "onServiceConnected");
            RsActivityBase.this.onServiceConnected();
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
	        if(bindService(intent, mConnection, Context.BIND_AUTO_CREATE)){ Log.v(TAG, "onCreate: bindService returned true"); }
	        else{ Log.e(TAG, "onCreate: bindService returned false"); }
    	}
    }
    
    
    /**
     * This method should be overridden by child classes that want to do something between Activity.onCreate and connection initialization it is guaranteed to be executed before onServiceConnected
     */
    protected void onCreateBeforeConnectionInit()
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
        onCreateBeforeConnectionInit();
        _bindRsService();
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	unbindService(mConnection);
    }
}
