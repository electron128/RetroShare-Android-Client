package org.retroshare.android;

import java.text.DecimalFormat;
import java.util.ArrayList;

import rsctrl.core.Core;
import rsctrl.system.System.RequestSystemStatus;
import rsctrl.system.System.ResponseSystemStatus;

import org.retroshare.android.RsCtrlService.ConnectionError;
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
import android.graphics.Color;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
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
	
	boolean isInForeground = false;

    boolean connectButtonRecentlyPressed = false;
	
    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
    {
        setContentView(R.layout.activity_main_ng);

		((Spinner) findViewById(R.id.serverSpinner)).setOnItemSelectedListener(this);

		findViewById(R.id.textViewNetStatus).setVisibility(View.GONE);
		findViewById(R.id.textViewNoPeers).setVisibility(View.GONE);
		findViewById(R.id.textViewBandwidth).setVisibility(View.GONE);

    	mHandler = new Handler();
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

		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.text_view, rsAvailableServers);
		Spinner serverSpinner = (Spinner) findViewById(R.id.serverSpinner);
		serverSpinner.setAdapter(spinnerAdapter);

    	updateViews();
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	updateViews();
    	isInForeground = true;
    }

    @Override
    public void onPause()
    {
    	super.onPause();
    	isInForeground = false;
    }
    
    private void updateViews()
    {
    	if(mBound)
    	{
			boolean actualServerConnected = false;
			RsCtrlService server = rsProxy.getActiveServers().get(serverName);
			if (server != null && server.isOnline()) actualServerConnected = true;


			View connectButton = findViewById(R.id.buttonConnect);
			TextView textViewServerKey = (TextView) findViewById(R.id.textViewServerKey);
			TextView textViewConnectionState = (TextView) findViewById(R.id.textViewConnectionState);


			if (actualServerConnected)
			{
				connectButton.setVisibility(View.GONE);
				textViewServerKey.setText("Server Key: " + server.getServerData().getHostkeyFingerprint()); // TODO HARDCODED string

				textViewConnectionState.setTextColor(Color.GREEN);
				textViewConnectionState.setText("  connected"); // TODO HARDCODED string
				textViewConnectionState.setVisibility(View.VISIBLE);
			}
			else
			{
				connectButton.setVisibility(View.VISIBLE);
				textViewServerKey.setVisibility(View.GONE);

    			requestSystemStatus();

				findViewById(R.id.textViewNetStatus).setVisibility(View.GONE);
				findViewById(R.id.textViewNoPeers).setVisibility(View.GONE);
				findViewById(R.id.textViewBandwidth).setVisibility(View.GONE);

				ConnectionError conErr = ConnectionError.UNKNOWN;
				boolean showStatus = false;
				if(server != null)
				{
					conErr = server.getLastConnectionError();
					showStatus = true;
				}

    			Log.v(TAG,"updateViews(): conErr: " + conErr);
    			
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
					textViewConnectionState.setText(getResources().getText(R.string.error)+ "Unknown Error"); //TODO HARDCODED string
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
    		}
    	}
    	else
    	{
    		Log.e(TAG,"Error: MainActivity.updateViews(): not bound");
    	}
    }
    
    public void deleteServerKey(View v)
    {
		RsCtrlService server = rsProxy.getActiveServers().get(serverName);
    	RsServerData sd = server.getServerData();
		sd.hostkey = null;
		server.setServerData(sd);
    }
    
    public void onConnectButtonPressed(View v)
    {
        connectButtonRecentlyPressed = true;

    	Log.d(TAG, "onConnectButtonPressed(View v)");
        if(mBound)
        {
        	Log.d(TAG,"onConnectButtonPressed(View v) connecting to Server: " + serverName );

            if(rsProxy.getSavedServers().get(serverName).password == null) showDialog(DIALOG_PASSWORD);
            else connect();

			v.setVisibility(View.GONE);
        	
        	TextView tv = (TextView) findViewById(R.id.textViewConnectionState);
			tv.setTextColor(Color.BLACK);
			tv.setText("connecting..."); // TODO HARDCODED string
			tv.setVisibility(View.VISIBLE);
        }
        else
        {
        	EditText text = (EditText) findViewById(R.id.editText1);
        	text.setText("Error: not bound"); // TODO HARDCODED string
        }
    }

    public void showPeers(View v) { startActivity(PeersActivity.class); };
    public void showChatLobbies(View v) { startActivity(ChatlobbyActivity.class); }
    public void onShowQrCode(View v) { Intent intent = new Intent(); intent.putExtra("Description", "just a test"); intent.putExtra("Data", "just a test"); startActivity(ShowQrCodeActivity.class, intent ); }
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
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce)
    {
        // TODO lock for a better place for this code if there is one... ////////
        if(connectButtonRecentlyPressed)
        {
            if(ce.kind == RsCtrlService.ConnectionEventKind.CONNECTED)
            {
                dismissDialog(DIALOG_CONNECT);
            }
            if(ce.kind == RsCtrlService.ConnectionEventKind.ERROR_WHILE_CONNECTING)
            {
                dismissDialog(DIALOG_CONNECT);
                showDialog(DIALOG_CONNECT_ERROR);
            }
        }
        ////////////////////////////////////////////////////////////////////////

        updateViews();
    }
	
	private void requestSystemStatus()
	{
    	if(mBound)
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
			if(isInForeground) { requestSystemStatus(); }
			mHandler.postAtTime(new requestSystemStatusRunnable(), SystemClock.uptimeMillis()+ UPDATE_INTERVAL);
		}
	}
	
	private class SystemStatusHandler extends RsMessageHandler
	{
		@Override
		protected void rsHandleMsg(RsMessage msg)
		{
			ResponseSystemStatus resp;
			try
			{
				resp = ResponseSystemStatus.parseFrom(msg.body);

				TextView textViewNetStatus = (TextView) findViewById(R.id.textViewNetStatus);
				TextView textViewNoPeers   = (TextView) findViewById(R.id.textViewNoPeers);
				TextView textViewBandwidth = (TextView) findViewById(R.id.textViewBandwidth);

		    	textViewNetStatus.setText(getResources().getText(R.string.network_status)+":\n"+resp.getNetStatus().toString()); //TODO HARDCODED string
		    	textViewNoPeers.setText(getResources().getText(R.string.peers)+": "+Integer.toString(resp.getNoConnected())+"/"+Integer.toString(resp.getNoPeers())); //TODO HARDCODED string
		    	DecimalFormat df = new DecimalFormat("#.##");
		    	textViewBandwidth.setText(getResources().getText(R.string.bandwidth_up_down)+":\n"+df.format(resp.getBwTotal().getUp())+"/"+df.format(resp.getBwTotal().getDown())+" (kB/s)");
		    	
		    	textViewNetStatus.setVisibility(View.VISIBLE);
		    	textViewNoPeers.setVisibility(View.VISIBLE);
		    	textViewBandwidth.setVisibility(View.VISIBLE);
			} catch (InvalidProtocolBufferException e) { e.printStackTrace(); } // TODO Auto-generated catch block
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

    private static final int DIALOG_PASSWORD=0;
    private static final int DIALOG_CONNECT=1;
    private static final int DIALOG_CONNECT_ERROR=2;
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
                                "login",
                                new DialogInterface.OnClickListener()
                                {
                                    @Override public void onClick(DialogInterface dialog, int which)
                                    {
                                        serverData.password = et.getText().toString();
                                        serverData.savePassword = cbsp.isChecked();
                                        connect();
                                    }
                                });
                return builder.create();

            case DIALOG_CONNECT:
                ProgressDialog pd = new ProgressDialog(MainActivity.this);
                pd.setMessage("connecting to "+serverData.hostname+":"+serverData.port); // TODO HARDCODED string
                return pd;

            case DIALOG_CONNECT_ERROR:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setTitle(R.string.connection_error)
                        // TODO solve the connection error thing
                        .setMessage(rsProxy.getActiveServers().get(serverData.name).getLastConnectionErrorString())
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {} }); // TODO HARDCODED string
                return builder2.create();
        }

        return null;
    }

    private void connect()
    {
        rsProxy.activateServer(serverName).registerListener(this);
        showDialog(DIALOG_CONNECT);
    }
}
