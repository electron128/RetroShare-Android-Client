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

package org.retroshare.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import rsctrl.core.Core;


public class AddFriendMethodChooserActivity extends ProxiedActivityBase
{
	@Override
	public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		setContentView(R.layout.activity_addfriend);
	}

	public void onFromKnownPeersButtonPressed(View v)
	{
		Intent i = new Intent();
		i.putExtra(PeersActivity.SHOW_ALL_PEER_EXTRA, true);
		startActivity(PeersActivity.class, i);
	}

	public void onShowQrCodeButtonPressed(View v)
	{
		if(isBound())
		{
			Core.Person p = getConnectedServer().mRsPeersService.getOwnPerson();
			Intent i = new Intent();
			i.putExtra(ShowQrCodeActivity.PGP_ID_EXTRA, p.getGpgId());
			i.putExtra(ShowQrCodeActivity.NAME_EXTRA, p.getName());
			startActivity(ShowQrCodeActivity.class, i);
		}
	}
}
