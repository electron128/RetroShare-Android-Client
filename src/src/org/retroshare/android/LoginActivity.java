package org.retroshare.android;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.retroshare.java.RsCtrlService;
import org.retroshare.java.RsServerData;
import org.retroshare.java.RsCtrlService.RsCtrlServiceListener;

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

public class LoginActivity extends ProxiedActivityBase implements RsCtrlServiceListener
{

	private static final String TAG="LoginActivity";
	
	private ListView listView;
	private ListAdapterListener lal;
	
	private RsServerData selectedServer;
	
	// thread to run bitdht test
	Thread testThread;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	// run test
    	// run in own thread to not get killed by activitymanager
    	testThread=new Thread(new Runnable()
		{
			@Override
			public void run()
			{
			    try
				{
			    	// copy bdboot.txt
			    	InputStream in = getResources().openRawResource(R.raw.bdboot);
					FileOutputStream out=openFileOutput("bdboot.txt", 0);
					int read=0;
					//int length=0;
					byte[] buffer=new byte[1000];
					while(read!=-1)
					{
						read=in.read(buffer);
						if(read!=-1)
						{
							out.write(buffer, 0, read);
							//length+=read;
						}
					}
					in.close();
					out.close();
				}
				catch (FileNotFoundException e) { e.printStackTrace(); } // TODO Auto-generated catch block
				catch (IOException e) { e.printStackTrace(); } // TODO Auto-generated catch block

				String path = getFilesDir().getAbsolutePath() + "/bdboot.txt";
			    Log.v(TAG, "java: calling native code");
				Log.v(TAG, "native code:" + bitdht.getIp(path));
				Log.v(TAG, "java: native code returned");
			}
		});
    	//testThread.start();
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        listView = (ListView) findViewById(R.id.listView1);
        lal = new ListAdapterListener(this);
        listView.setAdapter(lal);
        listView.setOnItemClickListener(lal);
    }
    
    @Override
    protected void onServiceConnected()
    {
    	lal.update();
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
		if(mBound) for (RsCtrlService server : rsProxy.getActiveServers().values()) server.unregisterListener(this);
	}
    
    public void onNewServerClick(View v)
    {
    	Intent intent = new Intent(this, AddServerActivity.class);
    	startActivity(intent);
    }
    
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
	    	cbvp.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					if(cbvp.isChecked()){
						//Log.v(TAG, "checked");
						et.setTransformationMethod(null);
					}else{
						//Log.v(TAG, "not checked");
						et.setTransformationMethod(new PasswordTransformationMethod());
					}
				}
	    	});
	    	final CheckBox cbsp=(CheckBox) view.findViewById(R.id.checkBoxSavePassword);
	    	
	    	builder.setView(view)
	    		.setTitle(R.string.enter_ssh_password)
	    		.setPositiveButton("login", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectedServer.password=et.getText().toString();
						selectedServer.savePassword=cbsp.isChecked();
						connect();
						//pd=ProgressDialog.show(LoginActivity.this, "", "connecting...", true);
					}
	    		});
	    	return builder.create();


    	case DIALOG_CONNECT:
    		ProgressDialog pd=new ProgressDialog(LoginActivity.this);
    		pd.setMessage("connecting to "+selectedServer.hostname+":"+selectedServer.port);
    		return pd;


    	case DIALOG_CONNECT_ERROR:
    		AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
	    	builder2.setTitle(R.string.connection_error)
	    		// TODO solve the connection error thing
	    		.setMessage(rsProxy.getActiveServers().get(selectedServer.name).getLastConnectionErrorString())
	    		.setPositiveButton(
						"ok", new DialogInterface.OnClickListener()
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
		rsProxy.activateServer(selectedServer.name).registerListener(this);
		showDialog(DIALOG_CONNECT);
    }
    
	@Override
	public void onConnectionStateChanged(RsCtrlService.ConnectionEvent ce)
	{
		Log.d(TAG, "onConnectionStateChanged(RsCtrlService.ConnectionEvent ce)");

		if(ce.kind == RsCtrlService.ConnectionEventKind.CONNECTED)
		{
			dismissDialog(DIALOG_CONNECT);
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}
		if(ce.kind == RsCtrlService.ConnectionEventKind.ERROR_WHILE_CONNECTING)
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
    		for(RsServerData sd : rsProxy.getSavedServers().values()) servers.add(sd);
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
