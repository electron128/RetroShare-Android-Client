package org.retroshare.android.sync;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


public class ContactsSyncAdapterService extends Service
{
	private static final String TAG = "ContactsSyncAdapterService";
	private static SyncAdapterImpl sSyncAdapter = null;
	private static ContentResolver mContentResolver = null;

	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter
	{
		private Context mContext;

		public SyncAdapterImpl(Context context)
		{
			super(context, true);
			mContext = context;
		}

		@Override
		public void onPerformSync ( Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult )
		{
			try { ContactsSyncAdapterService.performSync(mContext, account, extras, authority, provider, syncResult); }
			catch (OperationCanceledException e) {}
		}
	}


	@Override
	public IBinder onBind(Intent intent)
	{
		IBinder ret = null;
		ret = getSyncAdapter().getSyncAdapterBinder();
		return ret;
	}

	private SyncAdapterImpl getSyncAdapter()
	{
		if ( sSyncAdapter == null ) sSyncAdapter = new SyncAdapterImpl(this);
		return sSyncAdapter;
	}

	private static void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) throws OperationCanceledException
	{
		mContentResolver = context.getContentResolver();
		Log.i(TAG, "performSync: " + account.toString());
		//This is where the magic will happen!
	}
}
