package org.retroshare.android;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import rsctrl.core.Core;
import rsctrl.system.System.RequestSystemStatus;
import rsctrl.system.System.ResponseSystemStatus;

import org.retroshare.android.RsCtrlService.RsCtrlServiceListener;
import org.retroshare.android.RsCtrlService.RsMessage;

import com.google.protobuf.InvalidProtocolBufferException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.content.Intent;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class MainActivity extends ProxiedActivityBase implements RsCtrlServiceListener, AdapterView.OnItemSelectedListener
{
	private static final String TAG="MainActivity";
	
	private static final int UPDATE_INTERVAL = 1000;

	Handler mHandler;
	
    boolean connectButtonRecentlyPressed = false;

    List<View> showIfConnected;
    List<View> showIfConnectionError;
    List<View> showIfNotConnected;
	
    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
    {
        setContentView(R.layout.activity_main);

		((Spinner) findViewById(R.id.serverSpinner)).setOnItemSelectedListener(this);

        showIfConnected = new ArrayList<View>();
        showIfConnected.add(findViewById(R.id.textViewNetStatus));
        showIfConnected.add(findViewById(R.id.textViewBandwidth));
        showIfConnected.add(findViewById(R.id.buttonDisconnect));
		showIfConnected.add(findViewById(R.id.chatLobbiesClickContainer));
		showIfConnected.add(findViewById(R.id.peersClickContainer));
		showIfConnected.add(findViewById(R.id.filesClickContainer));
		showIfConnected.add(findViewById(R.id.searchClickContainer));

        showIfNotConnected = new ArrayList<View>();
		showIfNotConnected.add(findViewById(R.id.buttonConnect));
		showIfNotConnected.add(findViewById(R.id.buttonEdit));

        showIfConnectionError = new ArrayList<View>();
		showIfConnectionError.add(findViewById(R.id.buttonEdit));

        if(mHandler == null) mHandler = new Handler();
        mHandler.postAtTime(new requestSystemStatusRunnable(), SystemClock.uptimeMillis()+ UPDATE_INTERVAL);
    }
    
    @Override
    protected void onServiceConnected()
    {
		if (rsProxy.getSavedServers().size() < 1) showAddServerActivity();

		ArrayList<String> rsAvailableServers = new ArrayList<String>();
		rsAvailableServers.addAll(rsProxy.getSavedServers().keySet());
		try { serverName = rsAvailableServers.get(0); } catch (IndexOutOfBoundsException e) {}
		rsAvailableServers.add(getString(R.string.add_server));

		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, rsAvailableServers);
		Spinner serverSpinner = (Spinner) findViewById(R.id.serverSpinner);
		serverSpinner.setAdapter(spinnerAdapter);

    	updateViews();
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	updateViews();
    }

    private void setVisibility(List<View> views, int visibility) { for (View v : views) v.setVisibility(visibility); }

    private void updateViews()
    {
        boolean actualServerConnected = false;

    	if(isBound())
    	{
			RsCtrlService server = rsProxy.getActiveServers().get(serverName);
			if (server != null && server.isOnline()) actualServerConnected = true;
        }
        else { Log.e(TAG,"Error: MainActivity.updateViews(): not bound"); }

        if (actualServerConnected)
        {
            setVisibility(showIfConnectionError, View.GONE);
            setVisibility(showIfNotConnected, View.GONE);
            setVisibility(showIfConnected, View.VISIBLE);
        }
        else
        {
            requestSystemStatus();
            setVisibility(showIfConnectionError, View.GONE);
            setVisibility(showIfConnected, View.GONE);
            setVisibility(showIfNotConnected, View.VISIBLE);
        }
    }
    
	public void showPeers(View v)
	{
		Intent i = new Intent();
		i.putExtra(PeersActivity.SHOW_ADD_FRIEND_BUTTON_EXTRA, true);
		startActivity(PeersActivity.class, i);
	};
    public void showChatLobbies(View v) { startActivity(LobbiesListFragmentActivity.class); }
    public void showFilesActivity(View v) { startActivity(FilesActivity.class); }
    public void showSearchActivity(View v) { startActivity(ListSearchesActivity.class); }
	private void showAddServerActivity() { startActivity(AddServerActivity.class); }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		showDialog(DIALOG_TERMINATE_APP);
		return true;
	}

	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce)
	{
		if(connectButtonRecentlyPressed)
		{
			if(ce.kind == RsCtrlService.ConnectionEventKind.CONNECTED) dismissDialog(DIALOG_CONNECT);

			if(ce.kind == RsCtrlService.ConnectionEventKind.ERROR_WHILE_CONNECTING)
			{
				dismissDialog(DIALOG_CONNECT);
				showDialog(DIALOG_CONNECT_ERROR);
			}
		}

		updateViews();
	}
	
	private void requestSystemStatus()
	{
    	if(isBound())
    	{
			RsCtrlService server = rsProxy.getActiveServers().get(serverName);
			if(server != null && server.isOnline())
			{
				RsMessage msg=new RsMessage();
				msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.SYSTEM_VALUE<<8)|rsctrl.system.System.RequestMsgIds.MsgId_RequestSystemStatus_VALUE;
				msg.body=RequestSystemStatus.newBuilder().build().toByteArray();
				server.sendMsg(msg, new SystemStatusHandler());
			}
    	}
	}

	private class requestSystemStatusRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if(isForeground()) { requestSystemStatus(); }
			mHandler.postAtTime(new requestSystemStatusRunnable(), SystemClock.uptimeMillis()+ UPDATE_INTERVAL);
		}
	}
	
	private class SystemStatusHandler extends RsMessageHandler
	{
		public String TAG() { return "SystemStatusHandler"; }

		private String serverName;

		public SystemStatusHandler() { this.serverName = MainActivity.this.serverName; }

		@Override
		protected void rsHandleMsg(RsMessage msg)
		{
			if(MainActivity.this.serverName.equals(this.serverName))
			{
				try
				{
					ResponseSystemStatus resp = ResponseSystemStatus.parseFrom(msg.body);

					TextView textViewNetStatus = (TextView) findViewById(R.id.textViewNetStatus);
					TextView textViewBandwidth = (TextView) findViewById(R.id.textViewBandwidth);
					TextView peersTextView     = (TextView) findViewById(R.id.peersTextView);

					textViewNetStatus.setText( getResources().getText(R.string.network_status) + ": " + resp.getNetStatus().toString() );
					peersTextView.setText( getResources().getText(R.string.peers) + " (" + Integer.toString(resp.getNoConnected())+ "/" +Integer.toString(resp.getNoPeers()) + ")" );
					DecimalFormat df = new DecimalFormat("#.##");
					textViewBandwidth.setText(getResources().getText( R.string.bandwidth_up_down) + ": " + df.format(resp.getBwTotal().getUp()) + "/" + df.format(resp.getBwTotal().getDown()) + " (kB/s)");
				}
				catch (InvalidProtocolBufferException e) { e.printStackTrace(); }
			}
			else Log.d(TAG(), "rsHandleMsg: ignoring system status message update, user is looking at another server");
		}
	}

	// Those two handles the server spinner
	@Override public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
	{
		String si = ((Spinner) adapterView).getSelectedItem().toString();
		Log.d(TAG, "onItemSelected(..., " + si + ", ...)");

		if(si.equals(getString(R.string.add_server))) showAddServerActivity();
		else
		{
			serverName = si;
			updateViews();
		}
	}
	@Override public void onNothingSelected(AdapterView<?> adapterView) {}

    private static final int DIALOG_PASSWORD = 0;
    private static final int DIALOG_CONNECT = 1;
    private static final int DIALOG_CONNECT_ERROR = 2;
	private static final int DIALOG_TERMINATE_APP = 3;
    @Override
    protected Dialog onCreateDialog(int id)
    {
        final RsServerData serverData = rsProxy.getSavedServers().get(serverName);
        switch(id)
        {
            case DIALOG_PASSWORD:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.activity_login_dialog, null);


                final EditText et = (EditText) view.findViewById(R.id.editTextPassword);
                final CheckBox cbvp = (CheckBox) view.findViewById(R.id.checkBoxShowPassword);
                cbvp.setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override public void onClick(View v)
                            {
                                if(cbvp.isChecked()) et.setTransformationMethod(null);
                                else et.setTransformationMethod(new PasswordTransformationMethod());
                            }
                        });
                final CheckBox cbsp = (CheckBox) view.findViewById(R.id.checkBoxSavePassword);
                builder.setView(view)
                        .setTitle(R.string.enter_ssh_password)
                        .setPositiveButton(
                                getText(R.string.login),
                                new DialogInterface.OnClickListener()
                                {
                                    @Override public void onClick(DialogInterface dialog, int which)
                                    {
                                        serverData.password = et.getText().toString();
										boolean savePwd = cbsp.isChecked();
										if(savePwd ^ serverData.savePassword)
										{
											serverData.savePassword = savePwd;
											rsProxy.addServer(serverData);
											rsProxy.saveData();
										}
                                        _connect();
                                    }
                                });
                return builder.create();

            case DIALOG_CONNECT:
                ProgressDialog pd = new ProgressDialog(MainActivity.this);
                pd.setMessage( getText(R.string.connecting_to) + serverData.hostname + ":" + serverData.port);
                return pd;

            case DIALOG_CONNECT_ERROR:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setTitle(R.string.connection_error)
                        .setMessage(rsProxy.getActiveServers().get(serverData.name).getLastConnectionErrorString())
                        .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {} });
                return builder2.create();

			case DIALOG_TERMINATE_APP:
				AlertDialog.Builder termDialog = new AlertDialog.Builder(this);
				termDialog.setTitle(R.string.are_you_sure)
						.setMessage(R.string.terminate_really)
						.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
						{
							@Override public void onClick(DialogInterface dialogInterface, int i)
							{
								_unBindRsService();
								Intent intent = new Intent(MainActivity.this, RetroShareAndroidProxy.class);
								stopService(intent);
								MainActivity.this.finish();
							}
						})
						.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener(){@Override public void onClick(DialogInterface dialogInterface, int i){}});
				return termDialog.create();
        }

        return null;
    }

    private void _connect()
    {
		showDialog(DIALOG_CONNECT);
        rsProxy.activateServer(serverName).registerListener(this);
    }

    private void _disconnect() { rsProxy.deactivateServer(serverName); }

    public void onConnectButtonPressed(View v)
    {
        Log.d(TAG,"onConnectButtonPressed(View v) for server Server: " + serverName );

        connectButtonRecentlyPressed = true;

        if(isBound())
        {
            if(rsProxy.getSavedServers().get(serverName).password == null) showDialog(DIALOG_PASSWORD);
            else _connect();

            v.setVisibility(View.GONE);
        }
        else { Log.e(TAG(), "onConnectButtonPressed(View v) why i am not bound?"); }

        updateViews();
    }

    public void onDisconnectButtonPressed(View v) { _disconnect(); updateViews(); }
    public void onEditButtonPressed(View v)
	{
		Intent intent = new Intent();
		intent.putExtra(AddServerActivity.EDIT_SERVER_EXTRA, true);
		startActivity(AddServerActivity.class, intent);
	}
}

                /*

                ConnectionError conErr = ConnectionError.UNKNOWN;
				boolean showStatus = false;
				if(server != null)
				{
					conErr = server.getLastConnectionError();
					showStatus = true;
				}

    			switch(conErr)
    			{
    			case NONE:
    				textViewConnectionState.setVisibility(View.GONE);
    				showStatus = false;
    				break;
				case AuthenticationFailedException:
					textViewConnectionState.setText(getResources().getText(R.string.error)+": "+getResources().getText(R.string.err_auth_failed));
					break;
				case BadSignatureException:
					textViewConnectionState.setText(getResources().getText(R.string.error)+": "+getResources().getText(R.string.err_bad_signature));
					break;
				case ConnectException:
					textViewConnectionState.setText(getResources().getText(R.string.error)+": "+getResources().getText(R.string.err_connection_refused));
					break;
				case NoRouteToHostException:
					textViewConnectionState.setText(getResources().getText(R.string.error)+": "+getResources().getText(R.string.err_no_route_to_host));
					break;
				case RECEIVE_ERROR:
					textViewConnectionState.setText(getResources().getText(R.string.error)+": "+getResources().getText(R.string.err_receive));
					break;
				case SEND_ERROR:
					textViewConnectionState.setText(getResources().getText(R.string.error)+": "+getResources().getText(R.string.err_send));
					break;
				case UnknownHostException:
					textViewConnectionState.setText(getResources().getText(R.string.error)+": "+getResources().getText(R.string.err_unknown_host));
					break;
				case UNKNOWN:
					textViewConnectionState.setText(getResources().getText(R.string.error)+ ": "+ getResources().getText(R.string.unknown_error));
					break;
				default:
					textViewConnectionState.setText("default reached, this should not happen");
					break;

    			}

    			if(showStatus)
    			{
    				textViewConnectionState.setTextColor(Color.RED);
    				textViewConnectionState.setVisibility(View.VISIBLE);
    			}
    			*/