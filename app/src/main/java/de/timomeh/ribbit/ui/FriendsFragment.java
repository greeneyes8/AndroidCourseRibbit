package de.timomeh.ribbit.ui;

import android.app.AlertDialog;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.List;

import de.timomeh.ribbit.adapters.UserAdapter;
import de.timomeh.ribbit.utils.ParseConstants;
import de.timomeh.ribbit.R;

/**
 * Created by timomaemecke on 18/01/15.
 */
public class FriendsFragment extends Fragment {

    private static final String TAG = FriendsFragment.class.getSimpleName();
    protected List<ParseUser> mFriends;
    protected ParseRelation<ParseUser> mFriendsRelation;
    protected ParseUser mCurrentUser;

    protected GridView mGridView;
    protected ProgressBar mProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.user_grid, container, false);
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar2);
        mGridView = (GridView) rootView.findViewById(R.id.friendsGrid);

        TextView emptyTextView = (TextView) rootView.findViewById(android.R.id.empty);
        mGridView.setEmptyView(emptyTextView);

        return rootView;
    }

    @Override
    public void onResume() {
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
                        UserAdapter adapter = new UserAdapter(getActivity(), mFriends);
                        mGridView.setAdapter(adapter);
                    } else {
                        ((UserAdapter) mGridView.getAdapter()).refill(mFriends);
                    }

                } else {
                    Log.e(TAG, e.getMessage());
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(e.getMessage())
                            .setTitle(getString(R.string.error_title))
                            .setPositiveButton(getString(android.R.string.ok), null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });
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
