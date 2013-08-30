/**
 * @license
 *
 * Copyright (c) 2013 Gioacchino Mazzurco <gio@eigenlab.org>
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

package org.retroshare.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;


public class LobbiesListFragmentActivity extends ProxiedFragmentActivityBase
{
	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.factivity_lobbieslist);

		onNewIntent(getIntent());
	}

	@Override protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		Bundle fragmentArgs = new Bundle(1);
		fragmentArgs.putString(LobbiesListFragment.SERVER_NAME_EXTRA_KEY, serverName);

		LobbiesListFragment lobbiesListFragment = new LobbiesListFragment();
		lobbiesListFragment.setArguments(fragmentArgs);

		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		fragmentTransaction.replace(R.id.lobbyListFragmentContainer, lobbiesListFragment);
		fragmentTransaction.commit();
	}
}
