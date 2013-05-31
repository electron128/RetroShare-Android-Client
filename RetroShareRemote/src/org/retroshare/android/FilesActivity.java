package org.retroshare.android;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.retroshare.android.FilesService.FilesServiceListener;

import rsctrl.core.Core;
import rsctrl.files.Files;
import rsctrl.files.Files.Direction;
import rsctrl.files.Files.FileTransfer;
import rsctrl.files.Files.RequestControlDownload;
import rsctrl.files.Files.RequestControlDownload.Action;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class FilesActivity extends RsActivityBase {
	private static final String TAG="FilesActivity";
	
	private static final int UPDATE_INTERVALL=1000;
	
	private FilesListAdapterListener adapter;
	
	private Files.Direction mDirection;
	
	Handler mHandler;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_peers);
        
        adapter=new FilesListAdapterListener(this);
        ListView lv=new ListView(this);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(adapter);
        lv.setOnItemLongClickListener(adapter);
        setContentView(lv);
        
        if(getIntent().getBooleanExtra("Download", true)){
        	mDirection=Direction.DIRECTION_DOWNLOAD;
        }else{
        	mDirection=Direction.DIRECTION_UPLOAD;
        }
        
    	mHandler=new Handler();
    	mHandler.postAtTime(new requestFilesRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
    }
    
    @Override
    protected void onServiceConnected(){
        mRsService.mRsCtrlService.filesService.registerListener(adapter);
        mRsService.mRsCtrlService.filesService.updateTransfers(mDirection);
    }
    
    boolean isInForeground=false;
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(mRsService!=null){
    		
    	}
    	isInForeground=true;
    }
    @Override
    public void onPause(){
    	super.onPause();
    	isInForeground=false;
    }
    
    private final static int FILE_ACTION_DIALOG=0;
    private Core.File clickedFile;
    
    @Override
    protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id) {
		case FILE_ACTION_DIALOG:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("action")
				   .setItems(R.array.file_actions, new  DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						RequestControlDownload.Action action=null;
						
						switch(arg1){
						// continue
						case 0:
							action=Action.ACTION_CONTINUE;
							break;
							
						// wait
						case 1:
							action=Action.ACTION_WAIT;
							break;
							
						// pause
						case 2:
							action=Action.ACTION_PAUSE;
							break;
							
						// restart
						case 3:
							action=Action.ACTION_RESTART;
							break;
							
						// check
						case 4:
							action=Action.ACTION_CHECK;
							break;
							
						// cancel
						case 5:
							action=Action.ACTION_CANCEL;
							break;
							
						// show link as qrcode
						case 6:
					    	Intent intent = new Intent(FilesActivity.this, ShowQrCodeActivity.class);
					    	intent.putExtra("Description", "link:"+clickedFile.getName() );
					    	String link="retroshare://file?name="+clickedFile.getName()+"&size="+Long.toString(clickedFile.getSize())+"&hash="+clickedFile.getHash();
					    	intent.putExtra("Data",link);
					    	startActivity(intent);
							break;
							
						default:
						}
						if(action!=null){
							mRsService.mRsCtrlService.filesService.sendRequestControlDownload(clickedFile,action);
						}
					}});
			dialog=builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
    }
    
	private class requestFilesRunnable implements Runnable{
		@Override
		public void run() {
			if(isInForeground && mBound && mRsService.mRsCtrlService.isOnline()){
				mRsService.mRsCtrlService.filesService.updateTransfers(mDirection);
			}
			mHandler.postAtTime(new requestFilesRunnable(), SystemClock.uptimeMillis()+UPDATE_INTERVALL);
		}
	}
    
    private class FilesListAdapterListener implements ListAdapter, OnItemClickListener, OnItemLongClickListener,FilesServiceListener{
    	
    	private List<FileTransfer> transferList=new ArrayList<FileTransfer>();
    	
    	private List<DataSetObserver> observerList=new ArrayList<DataSetObserver>();
    	
    	private LayoutInflater mInflater;
    	
    	public FilesListAdapterListener(Context context) {
    		 mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	}
    	
    	@Override
    	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    		//Log.v("ChatLobbyListAdapterListener","Clicked on Item No:"+Integer.toString(position));
    		//Location loc=locationList.get(position);
    		
    		//Intent i=new Intent(PeersActivity.this,ChatActivity.class);
    		//i.putExtra("ChatId", ChatId.newBuilder().setChatType(ChatType.TYPE_PRIVATE).setChatId(loc.getSslId()).build().toByteArray());
    		// keine lobby info
    		//i.putExtra("ChatLobbyInfo", lobbyInfo.toByteArray());
    		//startActivity(i);
    		
    		clickedFile=transferList.get(position).getFile();
    		showDialog(FILE_ACTION_DIALOG);
    	}
    	
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long id) {
			/*Location loc=locationList.get(position);
			Person p=mapLocationToPerson.get(loc);
    		Intent i=new Intent(PeersActivity.this,PeerDetailsActivity.class);
    		i.putExtra("GpgId", p.getGpgId());
    		i.putExtra("SslId", loc.getSslId());
    		startActivity(i);*/
			return true;
		}

		@Override
		public int getCount() {
			return transferList.size();
		}

		@Override
		public Object getItem(int position) {
			return transferList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getItemViewType(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			FileTransfer ft=transferList.get(position);
	        
	        View view = mInflater.inflate(R.layout.activity_files_file_item, parent, false);
	        
	        //ImageView imageViewMessage=(ImageView) view.findViewById(R.id.imageViewMessage);
	        //ImageView imageViewUserState=(ImageView) view.findViewById(R.id.imageViewUserState);
	        TextView textViewName = (TextView) view.findViewById(R.id.textViewName);
	        TextView textViewSize = (TextView) view.findViewById(R.id.textViewSize);
	        TextView textViewHash = (TextView) view.findViewById(R.id.textViewHash);
	        TextView textViewRate = (TextView) view.findViewById(R.id.textViewRate);
	        
	        ProgressBar progressBar1=(ProgressBar)view.findViewById(R.id.progressBar1);
	        
	        textViewName.setText(ft.getFile().getName());
	        textViewSize.setText(Long.toString(ft.getFile().getSize()));
	        textViewHash.setText(ft.getFile().getHash());
	        textViewRate.setText(ft.getRateKBs()+"kB/s");
	        
	        progressBar1.setMax(1000);
	        progressBar1.setProgress((int)(ft.getFraction()*1000));
	        
	        /*
	        DecimalFormat df = new DecimalFormat("#.##");
	    	String fraction=df.format(ft.getFraction());
	    	String rate=df.format(ft.getRateKBs());
	        
	        textView1.setText(ft.getFile().getName()+" "+fraction+" "+rate+"kB/s");
	        */
	        return view;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEmpty() {
			return transferList.isEmpty();
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			observerList.add(observer);
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			observerList.remove(observer);
		}

		@Override public boolean areAllItemsEnabled() {return true;}
		@Override public boolean isEnabled(int position) {return true;}
		
		@Override
		public void update() {
	        if(mDirection.equals(Direction.DIRECTION_DOWNLOAD)){
				transferList=mRsService.mRsCtrlService.filesService.transfersDown;
	        }else{
				transferList=mRsService.mRsCtrlService.filesService.transfersUp;
	        }
	        
    		for(DataSetObserver obs:observerList){
    			obs.onChanged();
    		}
		}
    	
    }

}
