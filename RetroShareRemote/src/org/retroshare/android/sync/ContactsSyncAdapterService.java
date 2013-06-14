package org.retroshare.android.sync;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import org.retroshare.android.RsServiceBaseNG;

import java.util.ArrayList;
import java.util.List;

import rsctrl.core.Core.Person;


public class ContactsSyncAdapterService extends RsServiceBaseNG
{
	private static final String TAG = "ContactsSyncAdapterService";
	private static SyncAdapterImpl sSyncAdapter = null;
	private static ContentResolver mContentResolver = null;

	private class SyncAdapterImpl extends AbstractThreadedSyncAdapter
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
			try { performSync(mContext, account, extras, authority, provider, syncResult); }
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

	private void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) throws OperationCanceledException
	{
		Log.i(TAG, "performSync: " + account.toString());

		//This is where the magic will happen!

		if(mBound)
		{
			mContentResolver = context.getContentResolver();

			List<Person> peers = mRsService.mRsCtrlService.peersService.getPeersList();
			for (Person peer:peers)
				_addContact(account, peer);
		}
	}

	private static void _addContact(Account account, Person peer)
	{
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

		String name = peer.getName();

		//Create RawContact
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
		builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
		builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
		builder.withValue(RawContacts.SYNC1, name);
		operationList.add(builder.build());

		//Create a Data record of common type 'StructuredName' for our RawContact
		builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
		builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
		builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
		builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
		operationList.add(builder.build());

		try { mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList); }
		catch (Exception e)
		{
			Log.e(TAG, "Something went wrong during creation! " + e);
			e.printStackTrace();
		}
	}
}
