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
import java.util.HashSet;

import rsctrl.core.Core.Location;
import rsctrl.core.Core.Person;
import rsctrl.peers.Peers;

public class ContactsSyncAdapterService extends ProxiedServiceBase
{
	public String TAG() { return "ContactsSyncAdapterService"; }

	public static final String PGP_ID_COLUMN = RawContacts.SYNC1;

	private static SyncAdapterImpl sSyncAdapter = null;

	/** Convenience class represents Android Contact in the map PGP_id -> Android_Contact */
	private static class SyncEntry { public Long raw_id = 0L; }

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
		Log.d(TAG(), "performSync( .., " + account.name + ", .. )");

		if(mBound)
		{
			/**
			 * Return doing nothing if the server is not usable without user interaction
			 */
			if ( ! rsProxy.getActivableWithoutUiServers().contains(account.name) ) return;

			/**
			 * Get retroshare server
			 * and ask refresh data inside RsPeersService probably this is not useful for this sync but for the next one ( maybe it would be nice to do this automatically on retroshare server connection so we already have some data for the first sync)
			 */
			RsPeersService peersService = rsProxy.activateServer(account.name).mRsPeersService;
			peersService.requestPersonsUpdate(Peers.RequestPeers.SetOption.ALL, Peers.RequestPeers.InfoOption.ALLINFO);

			/** Get content resolver */
			ContentResolver contentResolver = context.getContentResolver();

			/**
			 *  Create and populate a map PGP ID -> sync entryID
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
			Collection<Person> peers = new HashSet<Person>(peersService.getPersonsByRelationship(r));

			/**
			 * Contact operation list
			 */
			ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
			int actualContactBackReferenceNumber = 0;

			/**
			 * Add insert of new RetroShare friends to android contacts to operation list
			 */
			for (Person peer : peers)
			{
				String pgpId = peer.getGpgId();

				/**
				 * Create contact if doesn't exists already
				 */
				if ( ! localContacts.containsKey(pgpId) )
				{
					ContentProviderOperation.Builder builder;
					String name = peer.getName();
					boolean isOnline = false;
					for(Location l : peer.getLocationsList()) isOnline |= (l.getState() & Location.StateFlags.CONNECTED_VALUE) == Location.StateFlags.CONNECTED_VALUE;

					/**
					 * Append an operation for the contact ( ContactsContract.RawContacts )
					 */
					builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
					builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
					builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
					builder.withValue(PGP_ID_COLUMN, pgpId);
					operationList.add(builder.build());
					actualContactBackReferenceNumber = operationList.size() - 1;

					/**
					 * Append an operation for the name associated with the contact ( ContactsContract.Data )
					 */
					builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
					builder.withValueBackReference(CommonDataKinds.StructuredName.RAW_CONTACT_ID, actualContactBackReferenceNumber);
					builder.withValue(Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
					builder.withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, name);
					operationList.add(builder.build());

					/**
					 * Append an operation for the PGP ID associated with the contact as a instant messaging contact ( ContactsContract.CommonDataKinds.Im )
					 */
					builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
					builder.withValueBackReference(CommonDataKinds.StructuredName.RAW_CONTACT_ID, actualContactBackReferenceNumber);
					builder.withValue(Data.MIMETYPE, CommonDataKinds.Im.CONTENT_ITEM_TYPE);
					builder.withValue(CommonDataKinds.Im.TYPE, CommonDataKinds.Im.TYPE_CUSTOM);
					builder.withValue(CommonDataKinds.Im.LABEL, getString(R.string.app_name));
					builder.withValue(CommonDataKinds.Im.PROTOCOL, CommonDataKinds.Im.PROTOCOL_CUSTOM);
					builder.withValue(CommonDataKinds.Im.CUSTOM_PROTOCOL, getString(R.string.retroshare_im_custom_proto));
					builder.withValue(CommonDataKinds.Im.DATA, account.name + "/" + pgpId);
					operationList.add(builder.build());

					/**
					 * Append an operation for the avatar image ( CommonDataKinds.Photo.PHOTO )
					 */
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					Bitmap icon;
					if(isOnline) icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.retrosharelogo2);
					else icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.retrosharelogo2_gray);
					icon.compress(CompressFormat.PNG, 0, stream);

					builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
					builder.withValueBackReference(CommonDataKinds.StructuredName.RAW_CONTACT_ID, actualContactBackReferenceNumber);
					builder.withValue(Data.MIMETYPE, CommonDataKinds.Photo.MIMETYPE);
					builder.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, stream.toByteArray());
					operationList.add(builder.build());
				}
            }


			/**
			 * Delete contacts that are no more friends on RetroShare
			 * TODO: contact deletion is not working ( already tried everything i found online... )
			 */
			if(peers.size() > 1) // Check if we have a populated list or an incomplete one before deleting contact
			{
				for( String pgpId : localContacts.keySet() )
				{
					/**
					 * Delete contact only if is not in friends set
					 */
					if( ! peers.contains(peersService.getPersonByPgpId(pgpId)) )
					{
						/**
						 * add delete the contact to operation list, android/sqlite will delete all related data too
						 * must specify on the URI that we are sync provider otherwise the contact will be flagged as deleted and not really deleted
						 */

						// Batched version ( better performance )
						ContentProviderOperation.Builder builder;
						builder = ContentProviderOperation.newDelete(RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build());
						builder.withSelection(RawContacts._ID + "=?", new String[]{String.valueOf(localContacts.get(pgpId))});
						operationList.add(builder.build());

						// Not batched version ( just for testing )
//						contentResolver.delete(
//								RawContacts.CONTENT_URI.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build(),
//								RawContacts._ID + "=?",
//								new String[]{String.valueOf(localContacts.get(pgpId))});
					}
				}
			}

			if ( operationList.size() > 0 )
			{
				try { contentResolver.applyBatch(ContactsContract.AUTHORITY, operationList); }
				catch (Exception e)
				{
					Log.e(TAG(), "Something went wrong editing friends into the database! " + e);
					e.printStackTrace();
				}
			}

		}
	}
}
