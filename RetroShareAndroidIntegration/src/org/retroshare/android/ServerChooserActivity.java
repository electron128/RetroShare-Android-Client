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
