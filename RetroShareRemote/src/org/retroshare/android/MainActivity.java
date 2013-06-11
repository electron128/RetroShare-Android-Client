package org.retroshare.android;

import java.text.DecimalFormat;

import rsctrl.core.Core;
import rsctrl.system.System.RequestSystemStatus;
import rsctrl.system.System.ResponseSystemStatus;

import org.retroshare.java.RsCtrlService;
import org.retroshare.java.RsServerData;
import org.retroshare.java.RsCtrlService.ConnectionError;
import org.retroshare.java.RsCtrlService.RsCtrlServiceListener;
import org.retroshare.java.RsCtrlService.RsMessage;

import com.google.protobuf.InvalidProtocolBufferException;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends RsActivityBase implements RsCtrlServiceListener
{
	private static final String TAG="MainActivity";
	
	private static final int UPDATE_INTERVALL=1000;
	
   	EditText editTextHostname;
   	EditText editTextPort;
   	EditText editTextUser;
   	EditText editTextPassword;
	TextView textViewConnectionState;
	Button buttonConnect;
	
	TextView textViewNetStatus;
	TextView textViewNoPeers;
	TextView textViewBandwidth;
	
	Handler mHandler;
	
	boolean isInForeground = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        
        editTextHostname=(EditText) findViewById(R.id.editTextHostname);
        editTextPort=(EditText) findViewById(R.id.editTextPort);
    	editTextUser=(EditText) findViewById(R.id.editTextUser);
    	editTextPassword=(EditText) findViewById(R.id.editTextPassword);
        textViewConnectionState=(TextView) findViewById(R.id.textViewConnectionState);
        buttonConnect=(Button) findViewById(R.id.buttonConnect);
        
    	textViewNetStatus=(TextView) findViewById(R.id.textViewNetStatus);
    	textViewNoPeers=(TextView) findViewById(R.id.textViewNoPeers);
    	textViewBandwidth=(TextView) findViewById(R.id.textViewBandwidth);
        
        textViewConnectionState.setVisibility(View.GONE);
        
    	textViewNetStatus.setVisibility(View.GONE);
    	textViewNoPeers.setVisibility(View.GONE);
    	textViewBandwidth.setVisibility(View.GONE);
    	
    	mHandler=new Handler();
    	mHandler.postAtTime(new requestSystemStatusRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
    }
    
    private RsServerData mServerData;
    
    @Override
    protected void onServiceConnected()
    {
    	mServerData = mRsService.mRsCtrlService.getServerData();
    	mRsService.mRsCtrlService.registerListener(this);
    	updateViews();
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	if(mRsService != null)
    	{
        	mServerData = mRsService.mRsCtrlService.getServerData();
        	updateViews();
    	}
    	isInForeground=true;
    }
    @Override
    public void onPause()
    {
    	super.onPause();
    	isInForeground=false;
    }
    
    private void updateViews()
    {
    	editTextHostname.setText(mServerData.hostname);
    	if( mServerData.port != 0) editTextPort.setText(Integer.toString(mServerData.port));
    	else editTextPort.setText("");
    	
    	editTextUser.setText(mServerData.user);
    	editTextPassword.setText(mServerData.password);
    	
    	TextView textViewServerKey=(TextView) findViewById(R.id.textViewServerKey);
    	try { textViewServerKey.setText("Server Key: "+mServerData.getHostkeyFingerprint()); }
    	catch(NullPointerException e)
    	{
    		textViewServerKey.setText("Server Key: Error in sd.hostkey.toString");
    		e.printStackTrace();
    	}

    	
    	if(mBound)
    	{
    		if(mRsService.mRsCtrlService.isOnline())
    		{
    			editTextHostname.setVisibility(View.GONE);
    			editTextPort.setVisibility(View.GONE);
    			editTextUser.setVisibility(View.GONE);
    			editTextPassword.setVisibility(View.GONE);
    			
            	buttonConnect.setVisibility(View.GONE);
            	
            	textViewConnectionState.setTextColor(Color.GREEN);            	
            	textViewConnectionState.setText("  connected");
            	textViewConnectionState.setVisibility(View.VISIBLE);
    		}
    		else
    		{
    			requestSystemStatus();
    			
    			editTextHostname.setVisibility(View.VISIBLE);
    			editTextPort.setVisibility(View.VISIBLE);
    			editTextUser.setVisibility(View.VISIBLE);
    			editTextPassword.setVisibility(View.VISIBLE);
    			
    	    	textViewNetStatus.setVisibility(View.GONE);
    	    	textViewNoPeers.setVisibility(View.GONE);
    	    	textViewBandwidth.setVisibility(View.GONE);
    			
    			buttonConnect.setVisibility(View.VISIBLE);
    			
    			ConnectionError conErr = mRsService.mRsCtrlService.getLastConnectionError();
    			boolean showStatus = true;

    			Log.v(TAG,"updateViews(): conErr: "+conErr);
    			
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
					textViewConnectionState.setText(mRsService.mRsCtrlService.getLasConnectionErrorString());
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
    	mServerData.hostkey = null;
    	mRsService.mRsCtrlService.setServerData(mServerData);
    }
    
    public void connect(View v)
    {
    	Log.v(TAG, "connect");
        if(mBound)
        {
        	mServerData.hostname=editTextHostname.getText().toString();
        	mServerData.port=Integer.parseInt(editTextPort.getText().toString());
        	mServerData.user=editTextUser.getText().toString();
        	mServerData.password=editTextPassword.getText().toString();
        	
        	Log.v(TAG,"connecting to Server: "+mServerData);
        	
        	mRsService.mRsCtrlService.setServerData(mServerData);
        	mRsService.mRsCtrlService.connect();
        	
        	buttonConnect.setVisibility(View.GONE);
        	
        	textViewConnectionState.setTextColor(Color.BLACK);
        	textViewConnectionState.setText("connecting...");
        	textViewConnectionState.setVisibility(View.VISIBLE);
        }
        else
        {
        	EditText text = (EditText) findViewById(R.id.editText1);
        	text.setText("Error: not bound");
        }
    }
    
    public void showPeers(View v)
    {
    	Intent intent = new Intent(this, PeersActivity.class);
    	startActivity(intent);
    }
    
    public void showChatLobbies(View v)
    {
    	Intent intent = new Intent(this, ChatlobbyActivity.class);
    	startActivity(intent);
    }
    
    public void onShowQrCode(View v)
    {
    	Intent intent = new Intent(this, ShowQrCodeActivity.class);
    	intent.putExtra("Description", "just a test");
    	intent.putExtra("Data", "just a test");
    	startActivity(intent);
    }
    
    public void showFilesActivity(View v)
    {
    	Intent intent = new Intent(this, FilesActivity.class);
    	startActivity(intent);
    }
    
    public void showSearchActivity(View v)
    {
    	Intent intent = new Intent(this, ListSearchesActivity.class);
    	startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce) { updateViews(); }
	
	private void requestSystemStatus()
	{
    	if(mBound && mRsService.mRsCtrlService.isOnline())
    	{
			RsMessage msg=new RsMessage();
			msg.msgId=(Core.ExtensionId.CORE_VALUE<<24)|(Core.PackageId.SYSTEM_VALUE<<8)|rsctrl.system.System.RequestMsgIds.MsgId_RequestSystemStatus_VALUE;
			msg.body=RequestSystemStatus.newBuilder().build().toByteArray();
			mRsService.mRsCtrlService.sendMsg(msg, new SystemStatusHandler());
    	}
	}
	
	private class requestSystemStatusRunnable implements Runnable
	{
		@Override
		public void run()
		{
			if(isInForeground) { requestSystemStatus(); }
			mHandler.postAtTime(new requestSystemStatusRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
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
		    	textViewNetStatus.setText(getResources().getText(R.string.network_status)+":\n"+resp.getNetStatus().toString());
		    	textViewNoPeers.setText(getResources().getText(R.string.peers)+": "+Integer.toString(resp.getNoConnected())+"/"+Integer.toString(resp.getNoPeers()));
		    	DecimalFormat df = new DecimalFormat("#.##");
		    	textViewBandwidth.setText(getResources().getText(R.string.bandwidth_up_down)+":\n"+df.format(resp.getBwTotal().getUp())+"/"+df.format(resp.getBwTotal().getDown())+" (kB/s)");
		    	
		    	textViewNetStatus.setVisibility(View.VISIBLE);
		    	textViewNoPeers.setVisibility(View.VISIBLE);
		    	textViewBandwidth.setVisibility(View.VISIBLE);
			} catch (InvalidProtocolBufferException e) { e.printStackTrace(); } // TODO Auto-generated catch block
		}
	}
}
