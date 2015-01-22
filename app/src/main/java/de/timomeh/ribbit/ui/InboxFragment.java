package de.timomeh.ribbit.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import de.timomeh.ribbit.adapters.MessageAdapter;
import de.timomeh.ribbit.utils.ParseConstants;
import de.timomeh.ribbit.R;

/**
 * Created by timomaemecke on 18/01/15.
 */
public class InboxFragment extends ListFragment {
    protected ProgressBar mProgressBar;
    protected List<ParseObject> mMessages;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_inbox, container, false);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);
        mSwipeRefreshLayout.setColorSchemeColors(
                R.color.swipe_refresh_2,
                R.color.swipe_refresh_1,
                R.color.swipe_refresh_3,
                R.color.swipe_refresh_4
        );

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        retrieveMessages();
    }

    private void retrieveMessages() {
        ParseQuery<ParseObject> query = new ParseQuery<>(ParseConstants.CLASS_MESSAGES);
        query.whereEqualTo(ParseConstants.KEY_RECIPIENT_IDS, ParseUser.getCurrentUser().getObjectId());
        query.addDescendingOrder(ParseConstants.KEY_CREATED_AT);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> messages, ParseException e) {

                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(false);
                }

                if (e == null) {
                    mMessages = messages;


                    String[] usernames = new String[mMessages.size()];
                    int i = 0;
                    for (ParseObject message : mMessages) {
                        usernames[i] = message.getString(ParseConstants.KEY_SENDER_NAME);
                        i++;
                    }
                    if (getListView().getAdapter() == null) {
                        MessageAdapter adapter = new MessageAdapter(getListView().getContext(), mMessages);
                        setListAdapter(adapter);
                    } else {
                        ((MessageAdapter) getListView().getAdapter()).refill(mMessages);
                    }
                }
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        ParseObject message = mMessages.get(position);
        String messageType = message.getString(ParseConstants.KEY_FILE_TYPE);
        ParseFile file = message.getParseFile(ParseConstants.KEY_FILE);
        Uri fileUri = Uri.parse(file.getUrl());

        if (messageType.equals(ParseConstants.TYPE_IMAGE)) {
            // View Image
            Intent intent = new Intent(getActivity(), ViewImageActivity.class);
            intent.setData(fileUri);
            startActivity(intent);
        } else {
            // view video
            Intent intent = new Intent(Intent.ACTION_VIEW, fileUri);
            intent.setDataAndType(fileUri, "video/*");
            startActivity(intent);
        }

        // Delete message
        List<String> ids = message.getList(ParseConstants.KEY_RECIPIENT_IDS);

        if (ids.size() == 1) {
            // last recipient - delete the whole thing
            message.deleteInBackground();
        } else {
            // remove the recipient and save
            ids.remove(ParseUser.getCurrentUser().getObjectId());
            ArrayList<String> idsToRemove = new ArrayList<String>();
            idsToRemove.add(ParseUser.getCurrentUser().getObjectId());
            message.removeAll(ParseConstants.KEY_RECIPIENT_IDS, idsToRemove);
            message.saveInBackground();
        }
    }

    private void toggleProgressBar() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
        }
        else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    protected SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            retrieveMessages();
        }
    };
}
