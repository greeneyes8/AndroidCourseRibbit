package de.timomeh.ribbit.ui;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

import de.timomeh.ribbit.R;
import de.timomeh.ribbit.adapters.UserAdapter;
import de.timomeh.ribbit.utils.FileHelper;
import de.timomeh.ribbit.utils.ParseConstants;


public class RecipientsActivity extends ActionBarActivity {

    private static final String TAG = RecipientsActivity.class.getSimpleName();
    protected List<ParseUser> mFriends;
    protected ParseRelation<ParseUser> mFriendsRelation;
    protected ParseUser mCurrentUser;
    protected Uri mMediaUri;
    protected String mFileType;
    protected MenuItem mSendButton;

    protected ProgressBar mProgressBar;
    protected GridView mGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_grid);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
        mGridView = (GridView) findViewById(R.id.friendsGrid);

        TextView emptyTextView = (TextView) findViewById(android.R.id.empty);
        mGridView.setEmptyView(emptyTextView);

        mGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
        mGridView.setOnItemClickListener(mOnItemClickListener);

        mMediaUri = getIntent().getData();
        mFileType = getIntent().getExtras().getString(ParseConstants.KEY_FILE_TYPE);
    }

    private void send(ParseObject message) {
        message.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // success
                    Toast.makeText(RecipientsActivity.this, R.string.success_message, Toast.LENGTH_LONG).show();
                    sendPushNotifications();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                    builder.setMessage(R.string.error_sending_message)
                            .setTitle(R.string.error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });
    }

    protected void sendPushNotifications() {
        ParseQuery<ParseInstallation> query = ParseInstallation.getQuery();
        query.whereContainedIn(ParseConstants.KEY_USER_ID, getRecipientIds());

        // send push notification
        ParsePush push = new ParsePush();
        push.setQuery(query);
        push.setMessage(getString(R.string.push_message, ParseUser.getCurrentUser().getUsername()));
        push.sendInBackground();
    }

    private ParseObject createMessage() {
        ParseObject message = new ParseObject(ParseConstants.CLASS_MESSAGES);
        message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
        message.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
        message.put(ParseConstants.KEY_RECIPIENT_IDS, getRecipientIds());
        message.put(ParseConstants.KEY_FILE_TYPE, mFileType);

        byte[] fileBytes = FileHelper.getByteArrayFromFile(this, mMediaUri);

        if (fileBytes == null) {
            return null;
        } else {
            if (mFileType.equals(ParseConstants.TYPE_IMAGE)) {
                fileBytes = FileHelper.reduceImageForUpload(fileBytes);
            }

            String fileName = FileHelper.getFileName(this, mMediaUri, mFileType);
            ParseFile file = new ParseFile(fileName, fileBytes);
            message.put(ParseConstants.KEY_FILE, file);
            return message;
        }
    }

    private ArrayList<String> getRecipientIds() {
        ArrayList<String> recipientIds = new ArrayList<>();
        for (int i = 0; i < mGridView.getCount(); i++) {
            if (mGridView.isItemChecked(i)) {
                recipientIds.add(mFriends.get(i).getObjectId());
            }
        }

        return recipientIds;
    }

    @Override
    protected void onResume() {
        super.onResume();


        mCurrentUser = ParseUser.getCurrentUser();
        mFriendsRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);

        toggleProgressBar();

        ParseQuery<ParseUser> query =  mFriendsRelation.getQuery();
        query.addAscendingOrder(ParseConstants.KEY_USERNAME);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                toggleProgressBar();

                if (e == null) {
                    mFriends = parseUsers;

                    String[] usernames = new String[mFriends.size()];
                    int i = 0;
                    for (ParseUser user : mFriends) {
                        usernames[i] = user.getUsername();
                        i++;
                    }
                    if (mGridView.getAdapter() == null) {
                        UserAdapter adapter = new UserAdapter(RecipientsActivity.this, parseUsers);
                        mGridView.setAdapter(adapter);
                    } else {
                        ((UserAdapter) mGridView.getAdapter()).refill(parseUsers);
                    }


                } else {
                    Log.e(TAG, e.getMessage());
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                    builder.setMessage(e.getMessage())
                            .setTitle(getString(R.string.error_title))
                            .setPositiveButton(getString(android.R.string.ok), null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });
    }

    protected AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ImageView checkImageView = (ImageView) view.findViewById(R.id.checkedImageView);

            if(mGridView.getCheckedItemCount() > 0) {
                mSendButton.setVisible(true);
            } else {
                mSendButton.setVisible(false);
            }

            if (mGridView.isItemChecked(position)) {
                checkImageView.setVisibility(View.VISIBLE);
            }
            else {
                checkImageView.setVisibility(View.INVISIBLE);
            }
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recipients, menu);
        mSendButton = menu.getItem(0);
        mSendButton.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch(itemId) {
            case R.id.action_send:
                ParseObject message = createMessage();
                if (message == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
                    builder.setMessage(R.string.error_file)
                            .setTitle(R.string.error_title)
                            .setPositiveButton(android.R.string.ok, null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    send(message);
                    finish();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleProgressBar() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }
}
