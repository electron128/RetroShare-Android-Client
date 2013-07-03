package org.retroshare.android;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import rsctrl.core.Core;
import rsctrl.core.Core.Person;
import rsctrl.msgs.Msgs;
import rsctrl.peers.Peers;


public class PeerDetailsActivity extends ProxiedActivityBase
{
	public String TAG() { return "PeerDetailsActivity"; }

	public final static String PGP_ID_EXTRA = "pgpId";
	private String pgpId;

    @Override
    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
	{
		pgpId = getIntent().getStringExtra(PGP_ID_EXTRA);

		setContentView(R.layout.activity_peerdetails);
    }

	@Override
	public void onServiceConnected()
	{
		Person p = getConnectedServer().mRsPeersService.getPersonByPgpId(pgpId);

		if(p != null)
		{
			TextView nameTextView = (TextView) findViewById(R.id.peerNameTextView);
			TextView pgpIdTextView = (TextView) findViewById(R.id.pgpIdTextView);
			Button toggleFriendshipButton = (Button) findViewById(R.id.buttonToggleFriendship);

			nameTextView.setText(p.getName());
			pgpIdTextView.setText(pgpId);

			Person.Relationship r = p.getRelation();
			if ( r.equals(Person.Relationship.YOURSELF) ) toggleFriendshipButton.setVisibility(View.GONE);
			else if ( r.equals(Person.Relationship.FRIEND) ) toggleFriendshipButton.setText(R.string.block_friend);
			else toggleFriendshipButton.setText(R.string.add_as_friend);
		}
		else Log.wtf(TAG(), "onServiceConnected() p with pgpId = " + pgpId + " is null this shouldn't happen!!");
	}

	public void onToggleFriendshipButtonPressed(View v)
	{
		if(isBound())
		{
			RsCtrlService server = getConnectedServer();
			Person p = server.mRsPeersService.getPersonByPgpId(pgpId);
			Person.Relationship r = p.getRelation();

			RsCtrlService.RsMessage msg = new RsCtrlService.RsMessage();
			msg.msgId = ( (Core.ExtensionId.CORE_VALUE << 24) | (Core.PackageId.FILES_VALUE << 8) | Peers.RequestMsgIds.MsgId_RequestAddPeer_VALUE );

			if ( r.equals(Person.Relationship.FRIEND) ) msg.body = Msgs.RequestAddPeer.newBuilder().setCmd(Msgs.RequestAddPeer.AddCmd.ADD).setGpgId(pgpId).build().toByteArray();
			else msg.body = Msgs.RequestAddPeer.newBuilder().setCmd(Msgs.RequestAddPeer.AddCmd.REMOVE).setGpgId(pgpId).build().toByteArray();

			server.sendMsg( msg, null );

			finish();
		}
	}
}
