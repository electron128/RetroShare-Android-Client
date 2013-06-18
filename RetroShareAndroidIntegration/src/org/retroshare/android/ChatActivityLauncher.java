package org.retroshare.android;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

import rsctrl.chat.Chat;

/**
 * XXX: da rifare completamente, e' solo un test
 * ah, trovare un intent migliore rispetto a android.intent.action.VIEW (esiste?)
 */
public class ChatActivityLauncher extends ProxiedActivityBase
{
    private static final String TAG = "ChatActivityLauncher";

    public void onCreateBeforeConnectionInit(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreateBeforeConnectionInit(Bundle savedInstanceState)");

        if (getIntent().getData() != null)
        {
            Cursor cursor = managedQuery(getIntent().getData(), null, null, null, null);
            if (cursor.moveToNext())
            {
                serverName = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));
                String sslid = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA2));

                Intent i = new Intent();
                i.putExtra("ChatId", Chat.ChatId.newBuilder().setChatType(Chat.ChatType.TYPE_PRIVATE).setChatId(sslid).build().toByteArray());
                startActivity(ChatActivity.class, i);
                //XXX: mi suicido subito dopo aver lanciato l'altra activity
                // tranquillo perche' ho il nohistory sul manifest (http://developer.android.com/guide/topics/manifest/activity-element.html#nohist)
                finish();
            }
        }
        else finish(); // How did we get here without data?
    }

}