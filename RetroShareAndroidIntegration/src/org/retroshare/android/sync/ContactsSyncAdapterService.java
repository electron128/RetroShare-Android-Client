package org.retroshare.android.sync;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import org.retroshare.android.R;
import org.retroshare.android.ProxiedServiceBase;
import org.retroshare.android.RsPeersService;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rsctrl.core.Core.Location;
import rsctrl.core.Core.Person;
public class ContactsSyncAdapterService extends ProxiedServiceBase
{
	private static final String TAG = "ContactsSyncAdapterService";
	private static SyncAdapterImpl sSyncAdapter = null;
	private static ContentResolver mContentResolver = null;
    private static String UsernameColumn = ContactsContract.RawContacts.SYNC1;
    private static String PhotoTimestampColumn = ContactsContract.RawContacts.SYNC2;
    private static String MIME="retroshare.android.cursor.item/org.retroshare.android.sync.profile"; // TODO Shouldn't this be of the form vnd.*/vnd.* ? // TODO Move to string.xml

    // TODO Check if we can to it smarter
    private static class SyncEntry
	{
        public Long raw_id = 0L;
        public Long photo_timestamp = null;
    }

    // We should try to keep it updated every edit
    HashMap<String, SyncEntry> localContacts = new HashMap<String, SyncEntry>();

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


    private void updateContactList(Account account)
	{
		Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, account.name).appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type).build();
        Cursor c1 = mContentResolver.query(rawContactUri, new String[] { BaseColumns._ID, UsernameColumn, PhotoTimestampColumn }, null, null, null);
        while (c1.moveToNext())
		{
            SyncEntry entry = new SyncEntry();
            entry.raw_id = c1.getLong(c1.getColumnIndex(BaseColumns._ID));
            entry.photo_timestamp = c1.getLong(c1.getColumnIndex(PhotoTimestampColumn));
            localContacts.put(c1.getString(1), entry);
        }
    }

	private void performSync(Context context, Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) throws OperationCanceledException
	{
		Log.i(TAG, "performSync( .., " + account.toString() + ", .. )");

		if(mBound)
		{
			mContentResolver = context.getContentResolver();
            updateContactList(account);

			RsPeersService peersService = rsProxy.activateServer(account.name).mRsPeersService;
			// Ask refresh data inside RsPeersService
			peersService.getOwnPerson();
			peersService.updatePeersList();

			List<Person> peers = new ArrayList<Person>();
			List<Location> locationList = new ArrayList<Location>();
			Map<Location,Person> mapLocationToPerson = new HashMap<Location,Person>();

			Person rsMe = peersService.getOwnPerson();
			if(rsMe != null) peers.add(rsMe);
			peers.addAll(peersService.getPeersList());

            String name;
            boolean online = false;
            Location lfound = null;
			for (Person peer:peers)
			{
                for(Location l:peer.getLocationsList())
				{
                    locationList.add(l);
                    mapLocationToPerson.put(l, peer);
                }

                ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
                try
				{
                    // If the contact doesn't exist, create it. Otherwise, set a status message
                    if (!_contactExist(account,peer))
					{
                        _addContact(account, peer);
                        updateContactList(account);
                    }
					else
					{
                        name = peer.getName();
                        online = false;
                        for(Location l : locationList)
						{
                            Person p = mapLocationToPerson.get(l);
                            if(p.equals(peer))
							{
                                if( (l.getState() & Location.StateFlags.CONNECTED_VALUE) == Location.StateFlags.CONNECTED_VALUE )
								{
									online = true;
									lfound = l;
									break;
								}
                                lfound=l;
                            }
                        }
                        //if (localContacts.get(name).photo_timestamp == null || System.currentTimeMillis() > (localContacts.get(name).photo_timestamp + 604800000L)) {
						// TODO bisogna trovare il modo di capire se lo stato del peer e' cambiato oppure come sopra aggiornare ogni tot invece che ogni volta
                        if(true)
						{
                            //You would probably download an image file and just pass the bytes, but this sample doesn't use network so we'll decode and re-compress the icon resource to get the bytes
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();

                            Bitmap icon = null;
                            if(online) icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.retrosharelogo2);
                            else icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.retrosharelogo2_gray);
                            icon.compress(CompressFormat.PNG, 0, stream);
                            _updateContactPhoto(operationList, localContacts.get(name).raw_id, stream.toByteArray());
                        }
                        _updateContactStatus(operationList, localContacts.get(name).raw_id, "Test aggiornamento ("+ lfound.getLocation()+")");
                    }
                    if (operationList.size() > 0) mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                }
				catch (Exception e1) { e1.printStackTrace(); } // TODO Auto-generated catch block

				if( ! _contactExist(account,peer) ) _addContact(account, peer); // TODO why we do this another time ??
            }
		}
	}

    /**
	 * @param peer Person to check if is already existend on android contact list
	 * @param account Android Account to check if the peer is already added
	 * @return true if this peer is already in contact list, false otherwise
	 */
    private boolean _contactExist(Account account, Person peer){ return localContacts.containsKey(peer.getName()); }

    private static void _updateContactPhoto(ArrayList<ContentProviderOperation> operationList, long rawContactId, byte[] photo)
	{
        ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI);
        builder.withSelection( ContactsContract.Data.RAW_CONTACT_ID + " = '" + rawContactId + "' AND " + ContactsContract.Data.MIMETYPE + " = '" + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null);
        operationList.add(builder.build());

        try
		{
            if(photo != null)
			{
                builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
                builder.withValue(ContactsContract.CommonDataKinds.Photo.RAW_CONTACT_ID, rawContactId);
                builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
                builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photo);
                operationList.add(builder.build());

                builder = ContentProviderOperation.newUpdate(ContactsContract.RawContacts.CONTENT_URI);
                builder.withSelection(ContactsContract.RawContacts.CONTACT_ID + " = '" + rawContactId + "'", null);
                builder.withValue(PhotoTimestampColumn, String.valueOf(System.currentTimeMillis()));
                operationList.add(builder.build());
            }
        }
		catch (Exception e) { e.printStackTrace(); } // TODO Auto-generated catch block
    }

	private static void _addContact(Account account, Person peer)
	{
		ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

		String name = peer.getName();
        List<Location> listl=peer.getLocationsList();
        Location l=null;
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

        String nickname = name;
        if(!listl.isEmpty()){
            l = listl.get(0); //XXX: a muzzo, sperimentalmente funziona, sarebbe interessante anche capire perche'
            nickname = l.getLocation();
        }

        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, MIME);
        builder.withValue(ContactsContract.Data.DATA1, nickname);
        builder.withValue(ContactsContract.Data.DATA2, l.getSslId());
        builder.withValue(ContactsContract.Data.DATA3, "Send message"); //TODO HARDCODED string
        operationList.add(builder.build());



		try { mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList); }
		catch (Exception e)
		{
			Log.e(TAG, "Something went wrong during creation! " + e);
			e.printStackTrace();
		}
	}

    private static void _updateContactStatus(ArrayList<ContentProviderOperation> operationList, long rawContactId, String status)
	{
        Uri rawContactUri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId);
        Uri entityUri = Uri.withAppendedPath(rawContactUri, ContactsContract.RawContacts.Entity.CONTENT_DIRECTORY);
        Cursor c = mContentResolver.query(entityUri, new String[] { ContactsContract.RawContacts.SOURCE_ID, ContactsContract.RawContacts.Entity.DATA_ID, ContactsContract.RawContacts.Entity.MIMETYPE, ContactsContract.RawContacts.Entity.DATA1 }, null, null, null);
        try
		{
            while (c.moveToNext())
			{
                if (!c.isNull(1))
				{
                    String mimeType = c.getString(2);

                    if (mimeType.equals(MIME)) // TODO Cannot we enforce this on the query ?
					{
                        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(ContactsContract.StatusUpdates.CONTENT_URI);
                        builder.withValue(ContactsContract.StatusUpdates.DATA_ID, c.getLong(1));
                        builder.withValue(ContactsContract.StatusUpdates.STATUS, status);
                        builder.withValue(ContactsContract.StatusUpdates.STATUS_RES_PACKAGE, "org.retroshare.android.sync"); //TODO HARDCODED string
                        builder.withValue(ContactsContract.StatusUpdates.STATUS_LABEL, R.string.app_name);
                        builder.withValue(ContactsContract.StatusUpdates.STATUS_ICON, R.drawable.retrosharelogo2);
                        builder.withValue(ContactsContract.StatusUpdates.STATUS_TIMESTAMP, System.currentTimeMillis());
                        operationList.add(builder.build());

                        //Only change the text of our custom entry to the status message pre-Honeycomb, as the newer contacts app shows statuses elsewhere
                        if(Build.VERSION.SDK_INT < 11)
						{
                            builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI);
                            builder.withSelection(BaseColumns._ID + " = '" + c.getLong(1) + "'", null);
                            builder.withValue(ContactsContract.Data.DATA3, status);
                            operationList.add(builder.build());
                        }
                    }
                }
            }
        }
		finally { c.close(); }
    }
}
