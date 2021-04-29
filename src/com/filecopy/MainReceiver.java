
package com.filecopy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class MainReceiver extends BroadcastReceiver {

    private static final String ACTION_FILECOPY = "android.intent.filecopy";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_FILECOPY.equals(intent.getAction())) {
            Intent i = new Intent(context, MainActivity.class);
            context.startActivity(i);
        }
    }
}
