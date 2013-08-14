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

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by G10h4ck on 7/16/13.
 */
public class ServerChooserActivity extends ProxiedActivityBase implements AdapterView.OnItemSelectedListener
{
	@Override
	protected void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		setContentView(R.layout.activity_server_chooser);
	}

	@Override
	protected void onServiceConnected()
	{
		Set<String> rsAvailableServers = rsProxy.getActivableWithoutUiServers();

		if (rsAvailableServers.size() < 1) startActivity(MainActivity.class);
		else
		{
			List<String> rsSelectableServers = new ArrayList<String>(rsAvailableServers);
			serverName = rsSelectableServers.get(0);

			ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(ServerChooserActivity.this, android.R.layout.simple_spinner_item, rsSelectableServers);
			Spinner serverSpinner = (Spinner) findViewById(R.id.serverSpinner);
			serverSpinner.setAdapter(spinnerAdapter);
		}
	}

	public void onOkButtonPressed(View v)
	{
		setResult(RESULT_OK, getIntent().putExtra(SERVER_NAME_EXTRA, serverName));
		finish();
	}

	@Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { serverName = ((Spinner) adapterView).getSelectedItem().toString(); }
	@Override public void onNothingSelected(AdapterView<?> adapterView) {}
}
