package org.retroshare.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import rsctrl.chat.Chat;

/**
 * XXX: da rifare completamente, e' solo un test
 * ah, trovare un intent migliore rispetto a android.intent.action.VIEW (esiste?)
 */
public class ChatActivityLauncher extends ProxiedActivityBase {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (getIntent().getData() != null) {
            Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
            if (cursor.moveToNext()) {
                String sslid = cursor.getString(cursor.getColumnIndex("DATA2"));
                Intent i=new Intent(ChatActivityLauncher.this,ChatActivity.class);
                i.putExtra("ChatId", Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId(sslid).build().toByteArray());
                // keine lobby info
                //i.putExtra("ChatLobbyInfo", lobbyInfo.toByteArray());
                startActivity(ChatActivity.class, i);
                //XXX: mi suicido subito dopo aver lanciato l'altra activity
                // tranquillo perche' ho il nohistory sul manifest (http://developer.android.com/guide/topics/manifest/activity-element.html#nohist)
                finish();


            }
        } else {
            // How did we get here without data?
            finish();
        }
    }

}