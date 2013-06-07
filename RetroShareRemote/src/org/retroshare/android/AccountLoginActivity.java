package org.retroshare.android;

import java.util.ArrayList;
import java.util.List;

import org.retroshare.android.RsCtrlService.RsCtrlServiceListener;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.text.method.PasswordTransformationMethod;


public class AccountLoginActivity extends RsActivityBase implements RsCtrlServiceListener
{

	private static final String TAG = "AccountLoginActivity";
	
	AccountAuthenticatorResponse response;
	
	private ListView listView;
	private ListAdapterListener lal;
	
	private RsServerData selectedServer;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        listView=(ListView) findViewById(R.id.listView1);
        lal=new ListAdapterListener(this);
        listView.setAdapter(lal);
        listView.setOnItemClickListener(lal);
        
        response = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
    }
    
    @Override
    protected void onServiceConnected()
    {
    	lal.update();
    	mRsService.mRsCtrlService.registerListener(AccountLoginActivity.this);
    }
    
    @Override
    public void onResume()
    {
    	super.onResume();
    	if(mBound) lal.update();
    }

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		if(mBound) mRsService.mRsCtrlService.unregisterListener(this);
	}
    
    public void onNewServerClick(View v)
    {
    	Intent intent = new Intent(this, AddServerActivity.class);
    	startActivity(intent);
    }
    
    // TODO: Maybe an enum is better ?
    private static final int DIALOG_PASSWORD=0;
    private static final int DIALOG_CONNECT=1;
    private static final int DIALOG_CONNECT_ERROR=2;
    
    
    //private ProgressDialog pd;
    
    @Override
    protected Dialog onCreateDialog(int id)
    {
    	switch(id)
    	{
    	case DIALOG_PASSWORD:
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    	View view = inflater.inflate(R.layout.activity_login_dialog, null);
	    	
	    	final EditText et=(EditText) view.findViewById(R.id.editTextPassword);
	    	final CheckBox cbvp=(CheckBox) view.findViewById(R.id.checkBoxShowPassword);
	    	cbvp.setOnClickListener(new View.OnClickListener()
	    	{
				@Override
				public void onClick(View v)
				{
					if(cbvp.isChecked()) et.setTransformationMethod(null);
					else et.setTransformationMethod(new PasswordTransformationMethod());
				}
	    	});
	    	final CheckBox cbsp=(CheckBox) view.findViewById(R.id.checkBoxSavePassword);
	    	
	    	builder
	    		.setView(view)
	    		.setTitle(R.string.enter_ssh_password)
	    		.setPositiveButton
	    		(
	    				"login",
	    				new DialogInterface.OnClickListener()
	    				{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								selectedServer.password=et.getText().toString();
								selectedServer.savePassword=cbsp.isChecked();
								connect();
								//pd=ProgressDialog.show(LoginActivity.this, "", "connecting...", true);
							}
						}
	    		);
	    	
	    	return builder.create();


    	case DIALOG_CONNECT:
    		ProgressDialog pd = new ProgressDialog(AccountLoginActivity.this);
    		pd.setMessage("Connecting to " + selectedServer.hostname + ":" + selectedServer.port );
    		return pd;


    	case DIALOG_CONNECT_ERROR:
    		AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
	    	builder2
	    		.setTitle(R.string.connection_error)
	    		.setMessage(mRsService.mRsCtrlService.getLasConnectionErrorString()) // TODO solve the connection error thing
	    		.setPositiveButton
	    		(
	    				"ok",
	    				new DialogInterface.OnClickListener()
	    				{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								//dismissDialog(DIALOG_CONNECT_ERROR);
							}
	    				}
	    		);
	    	
	    	return builder2.create();
    	}
    	
    	return null;
    }
    
    private void connect()
    {
		mRsService.mRsCtrlService.setServerData(selectedServer);
		mRsService.mRsCtrlService.connect();
		
		showDialog(DIALOG_CONNECT);
    }
    
	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce)
	{
		if( ce == RsCtrlService.ConnectionEvent.CONNECTED )
		{
			dismissDialog(DIALOG_CONNECT);
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}
		if( ce == RsCtrlService.ConnectionEvent.ERROR_WHILE_CONNECTING)
		{
			dismissDialog(DIALOG_CONNECT);
			showDialog(DIALOG_CONNECT_ERROR);
		}
	}
    
    private class ListAdapterListener implements ListAdapter, OnItemClickListener
    {
    	
    	private List<RsServerData> servers=new ArrayList<RsServerData>();
    	
    	private List<DataSetObserver> ObserverList=new ArrayList<DataSetObserver>();
    	private LayoutInflater mInflater;
    	
    	public ListAdapterListener(Context context)
    	{
    		 mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	public void update()
    	{
    		servers.clear();
    		for(RsServerData sd:mRsService.getServers().values()) servers.add(sd);
    		for(DataSetObserver obs:ObserverList) obs.onChanged();
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    	{
    		selectedServer=servers.get(position);
    		if(selectedServer.password==null) showDialog(DIALOG_PASSWORD);
    		else connect();
    	}

		@Override
		public int getCount() { return servers.size(); }

		@Override
		public Object getItem(int position) { return servers.get(position); }

		@Override
		public long getItemId(int position) { return 0; } // TODO Auto-generated method stub

		@Override
		public int getItemViewType(int position) { return 0; }

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
	        View view = mInflater.inflate(R.layout.activity_login_server_item, parent, false);
	        
	        TextView textView1 = (TextView) view.findViewById(R.id.textView1);
	        RsServerData sd=servers.get(position);
	        textView1.setText(sd.user+"@"+sd.name);
	        return view;
		}

		@Override
		public int getViewTypeCount() { return 1; }

		@Override
		public boolean hasStableIds() { return false; } // TODO Auto-generated method stub

		@Override
		public boolean isEmpty() { return servers.isEmpty(); }

		@Override
		public void registerDataSetObserver(DataSetObserver observer) { ObserverList.add(observer); }

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) { ObserverList.remove(observer); }

		@Override public boolean areAllItemsEnabled() { return true; }
		@Override public boolean isEnabled(int position) { return true; }
    	
    }
}
