package android.example.com.squawker.fcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.example.com.squawker.MainActivity;
import android.example.com.squawker.R;
import android.example.com.squawker.provider.SquawkContract;
import android.example.com.squawker.provider.SquawkProvider;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by manvi on 23/8/17.
 */

// This service will be triggered from the incoming data messages. In case of notifications, this service will
// be called when app is in foreground.
public class SquawkFirebaseMessageService extends FirebaseMessagingService {

    private static final String JSON_KEY_AUTHOR = SquawkContract.COLUMN_AUTHOR;
    private static final String JSON_KEY_AUTHOR_KEY = SquawkContract.COLUMN_AUTHOR_KEY;
    private static final String JSON_KEY_MESSAGE = SquawkContract.COLUMN_MESSAGE;
    private static final String JSON_KEY_DATE = SquawkContract.COLUMN_DATE;

    private static final int NOTIFICATION_MAX_CHARACTERS = 30;

    private static String LOG_TAG = SquawkFirebaseMessageService.class.getSimpleName();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // There are two types of messages data messages and notification messages. Data messages
        // are handled here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type raditionally used with FCM. Notification messages are only received here in
        // onMessageReceived when the app is in the foreground.
        // When the app is in the background an automatically generated notification is displayed. When the user
        // taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification messages. The Squawk server always sends just *data* messages, meaning that
        // onMessageReceived when the app is both in the foreground AND the background
        Log.d(LOG_TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        if(data.size() > 0){
            sendNotification(data);
            insertSquawk(data);

        }
    }


    private void  insertSquawk(final Map<String,String > map){

        AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(SquawkContract.COLUMN_AUTHOR, map.get(JSON_KEY_AUTHOR));
                contentValues.put(SquawkContract.COLUMN_MESSAGE, map.get(JSON_KEY_MESSAGE));
                contentValues.put(SquawkContract.COLUMN_AUTHOR_KEY, map.get(JSON_KEY_AUTHOR_KEY));
                contentValues.put(SquawkContract.COLUMN_DATE, map.get(JSON_KEY_DATE));

                getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, contentValues);

                return null;
            }
        };
        asyncTask.execute();
    }

    private void sendNotification(Map<String, String> map){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_ONE_SHOT);

        String author = map.get(JSON_KEY_AUTHOR);
        String message = map.get(JSON_KEY_MESSAGE);

        //if message length is more than 30, truncate it and add the unicode character for ellipsis
        if(message.length() > NOTIFICATION_MAX_CHARACTERS){
            message = message.substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026";
        }

        Uri defaultRingTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                                                .setAutoCancel(true)
                                                .setContentIntent(pendingIntent)
                                                .setSound(defaultRingTone)
                                                .setContentTitle(String.format(getString(R.string.notification_message),author))
                                                .setSmallIcon(R.drawable.ic_duck)
                                                .setContentText(message);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0,builder.build());
    }
}
