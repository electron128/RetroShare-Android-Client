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
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Data;
import android.util.Log;

import org.retroshare.android.R;
import org.retroshare.android.ProxiedServiceBase;
import org.retroshare.android.RsPeersService;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import rsctrl.core.Core.Location;
import rsctrl.core.Core.Person;
public class ContactsSyncAdapterService extends ProxiedServiceBase
{
	public String TAG() { return "ContactsSyncAdapterService"; }

	public static final String MIME = "vnd.retroshare.android.cursor.item/vnd.org.retroshare.android.sync.profile";
	public static final String PGP_ID_COLUMN = RawContacts.SYNC1;
	public static final String RS_NUMBER_PREFIX = "//rs/";

	private static SyncAdapterImpl sSyncAdapter = null;

    // TODO Check if we can to it smarter
    private static class SyncEntry
	{
        public Long raw_id = 0L;
    }

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
	public IBinder onBind(Intent intent) { return getSyncAdapter().getSyncAdapterBinder(); }

	private SyncAdapterImpl getSyncAdapter()
	{
		if ( sSyncAdapter == null ) sSyncAdapter = new SyncAdapterImpl(this);
		return sSyncAdapter;
	}

	private void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) throws OperationCanceledException
	{
		Log.d(TAG(), "performSync( .., " + account.toString() + ", .. )");

		if(mBound)
		{
			/**
			 * Get retroshare server
			 * and ask refresh data inside RsPeersService probably this is not useful for this sync but for the next one ( maybe it would be nice to do this automatically on retroshare server connection so we already have some data for the first sync)
			 */
			RsPeersService peersService = rsProxy.activateServer(account.name).mRsPeersService;
			peersService.updateFriendsList();

			/** Get content resolver */
			ContentResolver contentResolver = context.getContentResolver();

			/**
			 *  Create and populate a map PGP ID -> sync antryID
			 *  to keep track of contacts that are already present on android contacts list
 			 */
			HashMap<String, SyncEntry> localContacts = new HashMap<String, SyncEntry>();
			Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon()
					.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
					.appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
					.build();
			Cursor c1 = contentResolver.query(rawContactUri, new String[] { BaseColumns._ID, PGP_ID_COLUMN }, null, null, null);
			while (c1.moveToNext())
			{
				SyncEntry entry = new SyncEntry();
				entry.raw_id = c1.getLong(c1.getColumnIndex(BaseColumns._ID));
				localContacts.put(c1.getString(1), entry);
			}

			Collection<Person.Relationship> r = new ArrayList<Person.Relationship>();
			r.add(Person.Relationship.FRIEND);
			r.add(Person.Relationship.YOURSELF);
			Collection<Person> peers = peersService.getPersonsByRelationship(r);

			ContentProviderOperation.Builder builder;
			String name;
			String pgpId;
			boolean isOnline;
			ArrayList<ContentProviderOperation> operationList;
			for (Person peer : peers)
			{
				name = peer.getName();
				pgpId = peer.getGpgId();
				isOnline = false;
                for(Location l : peer.getLocationsList()) isOnline |= (l.getState() & Location.StateFlags.CONNECTED_VALUE) == Location.StateFlags.CONNECTED_VALUE;

                operationList = new ArrayList<ContentProviderOperation>();

				/**
				 * Create contact if doesn't exists else just update it
				 */
				if ( ! localContacts.containsKey(pgpId) )
				{
					/**
					 * Create a row for the contact ( ContactsContract.RawContacts )
					 */
					builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
					builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
					builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
					builder.withValue(PGP_ID_COLUMN, pgpId);
					operationList.add(builder.build());

					/**
					 * Create a row for the name associated with the contact ( ContactsContract.Data )
					 */
					builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
					builder.withValueBackReference(CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
					builder.withValue(Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
					builder.withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, name);
					operationList.add(builder.build());

					/**
					 * Create a row for the PGP ID associated with the contact as a instant messaging contact ( ContactsContract.CommonDataKinds.Im )
					 */
					builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
					builder.withValueBackReference(CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
					builder.withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE);
					builder.withValue(CommonDataKinds.Im.TYPE, CommonDataKinds.Im.TYPE_CUSTOM);
					builder.withValue(CommonDataKinds.Im.LABEL, getString(R.string.app_name));
					builder.withValue(CommonDataKinds.Im.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM);
					builder.withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, getString(R.string.retroshare_im_custom_proto));
					builder.withValue(CommonDataKinds.Im.DATA, account.name + "/" + pgpId);
					operationList.add(builder.build());

					/**
					 * Create a row for the avatar image ( CommonDataKinds.Photo.PHOTO )
					 */
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					Bitmap icon;
					if(isOnline) icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.retrosharelogo2);
					else icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.retrosharelogo2_gray);
					icon.compress(CompressFormat.PNG, 0, stream);

					builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
					builder.withValueBackReference(CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
					builder.withValue(Data.MIMETYPE, CommonDataKinds.Photo.MIMETYPE);
					builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray());
					operationList.add(builder.build());

					/**
					 * TODO Create a row for status message
					 */
				}
				else {} //TODO update contacts

				try { if ( operationList.size() > 0 ) contentResolver.applyBatch(ContactsContract.AUTHORITY, operationList); }
				catch (Exception e)
				{
					Log.e(TAG(), "Something went wrong putting data into the database! " + e);
					e.printStackTrace();
				}
            }
		}
	}
}
