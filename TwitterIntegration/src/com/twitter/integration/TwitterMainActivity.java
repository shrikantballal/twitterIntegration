package com.twitter.integration;

import java.util.ArrayList;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.imageloader.ImageLoader;
import com.google.android.imageloader.ImageLoader.Callback;

/**
 * The MainActivity launcher class.
 * 
 * @author Shrikant Ballal
 */
public class TwitterMainActivity extends Activity {

	/**
	 * The consumer key obtained on registering app on Twitter website
	 */
	private final String CONSUMER_KEY = "DCUUG2gWUX51X46S9UYLg";

	/**
	 * The consumer secret key obtained on registering app on Twitter website
	 */
	private final String CONSUMER_SECRET = "rs6NeFi1rza03LvpZalBwyQ1aFf5LRec0mag6zM5ok";
	private final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	private final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	private final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
	private final String TWITTER_CALLBACK_URL = "oauth://t4jsample";
	private final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";

	private static Twitter sTwitter;
	private static RequestToken sRequestToken;
	private ProgressDialog mProgressDialog;
	private SharedPreferences mSharedPreferences;
	private Connection mConnectionDetector;
	private AlertDialogManager mAlertDialogManager;
	private EditText mEditText;
	private ListView mListView;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mAlertDialogManager = new AlertDialogManager();
		mConnectionDetector = new Connection(getApplicationContext());
		if (!mConnectionDetector.isConnectedToInternet()) {
			mAlertDialogManager.showAlertDialog(TwitterMainActivity.this,
					"Internet Connection Error",
					"Please connect to working Internet connection");
			return;
		}
		// Check if twitter keys are set
		if (CONSUMER_KEY.trim().length() == 0
				|| CONSUMER_SECRET.trim().length() == 0) {
			mAlertDialogManager.showAlertDialog(TwitterMainActivity.this,
					"Twitter oAuth tokens",
					"Please set your twitter oauth tokens first!");
			return;
		}
		mSharedPreferences = getApplicationContext().getSharedPreferences(
				"MyPref", 0);

		mListView = (ListView) findViewById(R.id.listView);

		findViewById(R.id.button1).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						loginToTwitter();
					}
				});
		findViewById(R.id.button2).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						mEditText = (EditText) findViewById(R.id.editText1);
						String status = mEditText.getText().toString();
						if (status.trim().length() > 0) {
							new GetSearchResultFromTwitterAsyncTask()
									.execute(status);
						} else {
							Toast.makeText(getApplicationContext(),
									"Please enter search text",
									Toast.LENGTH_SHORT).show();
						}

					}
				});

		Uri uri = getIntent().getData();
		if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
			String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
			try {
				AccessToken accessToken = sTwitter.getOAuthAccessToken(
						sRequestToken, verifier);
				// Shared Preferences
				Editor edit = mSharedPreferences.edit();
				edit.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
				edit.putString(PREF_KEY_OAUTH_SECRET,
						accessToken.getTokenSecret());
				edit.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
				edit.commit();

				Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

				findViewById(R.id.button1).setVisibility(View.GONE);
				findViewById(R.id.editText1).setVisibility(View.VISIBLE);
				findViewById(R.id.button2).setVisibility(View.VISIBLE);

				Toast.makeText(getApplicationContext(),
						"Welcome " + sTwitter.getScreenName(),
						Toast.LENGTH_SHORT).show();

				long userID = accessToken.getUserId();
				User user = sTwitter.showUser(userID);
				String username = user.getName();
				Log.e("UserID: ", "userID: " + userID + "" + username);
				Log.v("Welcome:",
						"Thanks:"
								+ Html.fromHtml("<b>Welcome " + username
										+ "</b>"));
			} catch (Exception ex) {
				Log.e("Twitter Login Error", "> " + ex.getMessage());
			}
		}
	}

	/**
	 * Opens Login window for user to login into Twitter.
	 */
	private void loginToTwitter() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(CONSUMER_KEY);
		builder.setOAuthConsumerSecret(CONSUMER_SECRET);
		twitter4j.conf.Configuration configuration = builder.build();

		TwitterFactory factory = new TwitterFactory(configuration);
		sTwitter = factory.getInstance();

		try {
			sRequestToken = sTwitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
					.parse(sRequestToken.getAuthenticationURL())));
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

	/**
	 * AsyncTask to fetch search result from Twitter based on search query.
	 * 
	 * @author Shrikant Ballal
	 * 
	 */
	class GetSearchResultFromTwitterAsyncTask extends
			AsyncTask<String, String, ArrayList<twitter4j.Status>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(TwitterMainActivity.this);
			mProgressDialog.setMessage("Searching in twitter...");
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		protected ArrayList<twitter4j.Status> doInBackground(String... args) {
			ArrayList<twitter4j.Status> tweets = new ArrayList<twitter4j.Status>();
			Log.d("Tweet Text", "> " + args[0]);
			String status = args[0];
			try {
				ConfigurationBuilder builder = new ConfigurationBuilder();
				builder.setOAuthConsumerKey(CONSUMER_KEY);
				builder.setOAuthConsumerSecret(CONSUMER_SECRET);
				// Access Token
				String access_token = mSharedPreferences.getString(
						PREF_KEY_OAUTH_TOKEN, "");
				// Access Token Secret
				String access_token_secret = mSharedPreferences.getString(
						PREF_KEY_OAUTH_SECRET, "");

				AccessToken accessToken = new AccessToken(access_token,
						access_token_secret);
				Twitter twitter = new TwitterFactory(builder.build())
						.getInstance(accessToken);

				Query query = new Query(status);
				QueryResult result;
				result = twitter.search(query);
				tweets = (ArrayList<twitter4j.Status>) result.getTweets();
			} catch (TwitterException e) {
				// Error in updating status
				Log.d("Twitter Update Error", e.getMessage());
			}
			return tweets;
		}

		protected void onPostExecute(final ArrayList<twitter4j.Status> tweets) {
			// dismiss the dialog after getting all products
			mProgressDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setListAdapter(tweets);
				}
			});
		}
	}

	protected void setListAdapter(ArrayList<twitter4j.Status> tweets) {
		SearchResultListAdapter adapter = new SearchResultListAdapter(this,
				tweets);
		mListView.setVisibility(View.VISIBLE);
		mListView.setAdapter(adapter);
	}

	/**
	 * The BaseListAdapter to show search result in listView.
	 * 
	 * @author Shrikant Ballal
	 * 
	 */
	class SearchResultListAdapter extends BaseAdapter {

		private Context context;
		private ArrayList<twitter4j.Status> tweets;
		private ViewHolder holder;
		private ImageLoader loader;

		public SearchResultListAdapter(Context context,
				ArrayList<twitter4j.Status> tweets) {
			this.context = context;
			this.tweets = tweets;
		}

		@Override
		public int getCount() {
			return tweets.size();
		}

		@Override
		public Object getItem(int arg0) {
			return tweets.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		class ViewHolder {
			ImageView imageView;
			TextView userName, tweet;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(context).inflate(
						R.layout.list_item, null);
				holder = new ViewHolder();
				holder.imageView = (ImageView) convertView
						.findViewById(R.id.userImage);
				holder.userName = (TextView) convertView
						.findViewById(R.id.userNameTV);
				holder.tweet = (TextView) convertView
						.findViewById(R.id.tweetTV);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			twitter4j.Status tweet = (Status) getItem(pos);
			User user = (User) tweet.getUser();
			if (loader == null) {
				loader = new ImageLoader();
			}
			loader.bind(holder.imageView, user.getOriginalProfileImageURL(), 
					new Callback() {

						@Override
						public void onImageLoaded(ImageView view, String url) {
							Uri uri = Uri.parse(url);
							Bitmap bitmap = BitmapFactory.decodeFile(uri
									.getPath());
							if (bitmap != null)
								view.setImageBitmap(bitmap);
						}

						@Override
						public void onImageError(ImageView view, String url,
								Throwable error) {
							view.setImageResource(R.drawable.noimage_small);
						}
					});
			holder.userName.setText(user.getScreenName());
			holder.tweet.setText(tweet.getText().trim() + "\non "
					+ user.getCreatedAt().toString().trim());
			return convertView;
		}
	}
}