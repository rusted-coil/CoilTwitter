package sabikoi.app.coiltwitter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserStreamAdapter;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.TabActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends TabActivity  implements OnClickListener,OnItemClickListener
{
	  private static final String CALLBACK = ConsumerKeys.CALLBACK;
	  private static final String CONSUMER_KEY = ConsumerKeys.CONSUMER_KEY;
	  private static final String CONSUMER_SECRET = ConsumerKeys.CONSUMER_SECRET;

    private static final int REQUEST_OAUTH=0;
  	private static final int REQUEST_GALLERY = 1;
    
    private static long user_id=0L;
    private static String screen_name=null;
    private static String token=null;
    private static String token_secret=null;
    
    //OAuthデータ保存用
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    
    TextView tv;
    int a;
    
    String postImagePath = "";
    
    //twitterやるハンドラ
    Handler mHandler = new Handler();
    
    boolean postable = true;
    boolean refreshable = true;
    
    //自分のアカウント情報
		String userid;
		
    //View
    ListView timeline;
    //リスト用
    LinkedList<PostArrayItem> tlitems = new LinkedList<PostArrayItem>();
    ArrayAdapter<PostArrayItem> timeline_adapter;
    long lastpostid;
    
    //ユーザーストリーム
    TwitterStream userStream;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        // プログレス表示
        requestWindowFeature(Window.FEATURE_PROGRESS);
      	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_main);

        initTabs();
        
        setTitle("アカウント認証中");

        //OAuth認証データ読み込み
        pref = getSharedPreferences("t4jdata", Activity.MODE_PRIVATE);
        token=pref.getString("token", "");
        token_secret=pref.getString("token_secret", "");
 
        //OAuthデータ無し時認証起動
        if(token.length()==0)
        {
	        Intent intent = new Intent(this, OAuthActivity.class);
	        intent.putExtra(OAuthActivity.CALLBACK, CALLBACK);
	        intent.putExtra(OAuthActivity.CONSUMER_KEY, CONSUMER_KEY);
	        intent.putExtra(OAuthActivity.CONSUMER_SECRET, CONSUMER_SECRET);
	        startActivityForResult(intent, REQUEST_OAUTH);
        }
        
        //Intentの取得
        Intent cameraIntent = getIntent();
        //画像のUri取得
        Uri uri;
        if(Intent.ACTION_SEND.equals(cameraIntent.getAction())) 
        {
            uri = cameraIntent.getParcelableExtra(Intent.EXTRA_STREAM);
          	postImagePath = getPath(this,uri);
          	TextView tv = (TextView)findViewById(R.id.postimage);
          	tv.setText("with " + new File(postImagePath).getName());
            TabHost tabHost = getTabHost();
            tabHost.setCurrentTab(2);
        }
        
        //TL
        tlitems = new LinkedList<PostArrayItem>();
        
        ResetTimeline();
        
        timeline = (ListView)findViewById(R.id.timeline);
        timeline_adapter = new PostArrayAdapter(this,R.layout.timeline,tlitems);
        timeline.setAdapter(timeline_adapter);
        SetListParams(this,timeline);
        
        //ユーザーストリームの用意
  			userStream = getTwitterStream();
  			userStream.addListener(new myUserStreamAdapter());
    }
    
    protected void ResetTimeline()
    {
      tlitems.clear();
      
      //TLの取得
      Twitter twitter = getTwitter();
      try{
        ResponseList<Status> statuses = twitter.getHomeTimeline();
        if(statuses.size() > 0)
        	lastpostid = statuses.get(0).getId();
        for(Status status : statuses)
        {
          tlitems.add(new PostArrayItem(status.getUser().getScreenName(), status.getText(), status.getCreatedAt()));
        }
      }catch(TwitterException te)
      {
      	te.printStackTrace();
      }    	
    }
    
    protected void AddTimeline()
    {
      //TLの取得
      Twitter twitter = getTwitter();
      try{
      	Paging page = new Paging(lastpostid);
        ResponseList<Status> statuses = twitter.getHomeTimeline(page);
        for(int i = statuses.size()-1; i >= 0; --i)
        {
        	Status status = statuses.get(i);
          tlitems.addFirst(new PostArrayItem(status.getUser().getScreenName(), status.getText(), status.getCreatedAt()));
        }
        if(statuses.size() > 0)
        	lastpostid = statuses.get(0).getId();
      }catch(TwitterException te)
      {
      	te.printStackTrace();
      }    	    	
    }
    
    @Override
    public void onStart()
    {
    	super.onStart();
    	if(token.length() > 0)
    	{
    		Twitter twitter = getTwitter();
    		try {
					userid = twitter.showUser(twitter.getId()).getScreenName();
	    		setTitle("logged in as @" + userid);
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (TwitterException e) {
					e.printStackTrace();
				}
    	}
    	else{
    		setTitle("(not authorized)");
    	}
    }
    
    @Override
  	public void onClick(View v) 
    {
    	Twitter twitter;
    	Status status;
    	if(v == findViewById(R.id.postbutton1))
    	{
    		twitter = getTwitter();     
        status = null;
        EditText posttext = (EditText)findViewById(R.id.posttext);
        try {
            status = twitter.updateStatus(posttext.toString());
        } catch (TwitterException e) {
            e.printStackTrace();
        }    		
    	}
    	else if(v == findViewById(R.id.postbutton2))
    	{
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(intent,REQUEST_GALLERY);    		
    	}
  	}
    
    //post
    public void post1(View v)
    {
    	if(postable == true)
    	{  	  		
    		AsyncPost posttask = new AsyncPost();
    		posttask.execute();
    	}
    }
    
    //画像
    public void post2(View v)
    {
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(intent,REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) 
    {
        // OAuth完了
        if (requestCode == REQUEST_OAUTH && resultCode == RESULT_OK) 
        {
            user_id=intent.getLongExtra(OAuthActivity.USER_ID, 0L);
            screen_name=intent.getStringExtra(OAuthActivity.SCREEN_NAME);
            token=intent.getStringExtra(OAuthActivity.TOKEN);
            token_secret=intent.getStringExtra(OAuthActivity.TOKEN_SECRET);
             
//            tv.setText(token_secret);
             
            //認証データ保存
            editor = pref.edit();
            editor.putString("token",token);
            editor.putString("token_secret",token_secret);
            editor.commit(); 
        }
    		if(requestCode == REQUEST_GALLERY && resultCode == RESULT_OK)
    		{
        	postImagePath = getPath(this,Uri.parse(intent.getDataString()));
        	TextView tv = (TextView)findViewById(R.id.postimage);
        	tv.setText("with " + new File(postImagePath).getName());
    		}
    }
    
    private static Twitter getTwitter()
    {
    	AccessToken accessToken = new AccessToken(token,token_secret);
    	Configuration conf = getConfiguration();
    	TwitterFactory twitterfactory = new TwitterFactory(conf);
      return twitterfactory.getInstance(accessToken);
    }

    private static TwitterStream getTwitterStream()
    {
    	AccessToken accessToken = new AccessToken(token,token_secret);
    	Configuration conf = getConfiguration();
    	TwitterStreamFactory twitterstreamfactory = new TwitterStreamFactory(conf);
      return twitterstreamfactory.getInstance(accessToken);    	
    }
    
    private static Configuration getConfiguration() {
      ConfigurationBuilder confbuilder = new ConfigurationBuilder();
      confbuilder.setOAuthConsumerKey(CONSUMER_KEY);
      confbuilder.setOAuthConsumerSecret(CONSUMER_SECRET);
      return confbuilder.build();
  }    
    
   public static String getPath(Context context, Uri uri) {
      ContentResolver contentResolver = context.getContentResolver();
      String[] columns = { MediaStore.Images.Media.DATA };
      Cursor cursor = contentResolver.query(uri, columns, null, null, null);
      cursor.moveToFirst();
      String path = cursor.getString(0);
      cursor.close();
      return path;
  }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, 1, Menu.NONE, "認証の取り消し");
    	menu.add(Menu.NONE, 2, Menu.NONE, "TLの更新");
    	menu.add(Menu.NONE, 3, Menu.NONE, "ユーザーストリームの開始");
    	menu.add(Menu.NONE, 4, Menu.NONE, "ユーザーストリームの停止");
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{
    		case 1:
          //OAuth認証データ読み込み
    			pref = getSharedPreferences("t4jdata", Activity.MODE_PRIVATE);
    			editor = pref.edit();
    			editor.clear().commit();
   
          //OAuthデータ無し時認証起動
	        Intent intent = new Intent(this, OAuthActivity.class);
	        intent.putExtra(OAuthActivity.CALLBACK, CALLBACK);
	        intent.putExtra(OAuthActivity.CONSUMER_KEY, CONSUMER_KEY);
	        intent.putExtra(OAuthActivity.CONSUMER_SECRET, CONSUMER_SECRET);
	        startActivityForResult(intent, REQUEST_OAUTH);
    			return true;
    		case 2:
        	if(refreshable == true)
        	{  	  		
        		RefreshTL refreshtask = new RefreshTL();
        		refreshtask.execute();
        	}
    			return true;
    		case 3:
    			userStream.user();
	    		setTitle("logged in as @" + userid + " [UserStream]");    			
    			return true;
    		case 4:
    			userStream.shutdown();
	    		setTitle("logged in as @" + userid);    			
    			return true;
    	}
    	return true;
    }
    protected void initTabs()
    {
      Resources res = getResources();
      TabHost tabHost = getTabHost();
      
     	tabHost.addTab(tabHost.newTabSpec("Tab1").setIndicator("TL",res.getDrawable(android.R.drawable.ic_menu_recent_history)).setContent(R.id.timeline));
     	tabHost.addTab(tabHost.newTabSpec("Tab1").setIndicator("Reply",res.getDrawable(android.R.drawable.ic_menu_recent_history)).setContent(R.id.reply));
     	tabHost.addTab(tabHost.newTabSpec("Tab1").setIndicator("Post",res.getDrawable(android.R.drawable.ic_menu_recent_history)).setContent(R.id.post));

      // Set Default Tab - zero based index
      tabHost.setCurrentTab(0);
    }
  	protected void SetListParams(OnItemClickListener context, ListView lv)
  	{
  		lv.setFocusable(true);
  		lv.setScrollingCacheEnabled(false); 
  		lv.setOnItemClickListener(context);
  		lv.setVerticalFadingEdgeEnabled(false);
  	}
    
    class AsyncPost extends AsyncTask<String, Void, Void> 
    {
    	StatusUpdate statusup;
    	EditText posttext;
      @Override
      protected void onPreExecute() {
          super.onPreExecute();
  	    	postable = false;
  	  		setProgressBarVisibility(true);
  	  		setProgressBarIndeterminateVisibility(true);
  	   		setProgress(0);  	   	
  	   		
        	//post内容の取得
					posttext = (EditText)findViewById(R.id.posttext);
					statusup = new StatusUpdate(posttext.getText().toString());
      }

      @Override
      protected Void doInBackground(String... params) 
      {
      	AccessToken accessToken = null;
        if(params != null)
        {					
					//画像をアップロードするか
					if(postImagePath.compareTo("") != 0)
					  statusup.media(new File(postImagePath));      	
					
					twitter4j.Status tstatus = null;
					try {
						Twitter twitter = getTwitter();
					  tstatus = twitter.updateStatus(statusup);
					} catch (TwitterException e) {
					  e.printStackTrace();
					}
				}
				return null;
      }

      @Override
      protected void onPostExecute(Void result) {
          super.onPostExecute(result);
					//終了後
				  posttext.setText("");
					
				  TabHost tabHost = getTabHost();
				  tabHost.setCurrentTab(0);
				  // ソフトキーボードを非表示にする
				  InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				  imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					setProgressBarVisibility(false);
					setProgressBarIndeterminateVisibility(false);
					postable = true;
      }
    }

    class RefreshTL extends AsyncTask<String, Integer, Void> 
    {
      @Override
      protected void onPreExecute() {
          super.onPreExecute();
  	    	refreshable = false;
  	  		setProgressBarVisibility(true);
  	  		setProgressBarIndeterminateVisibility(true);
  	   		setProgress(0);  	   	
      }

      @Override
      protected Void doInBackground(String... params) 
      {
        if(params != null)
        {					
          //TLの取得
          Twitter twitter = getTwitter();
          try{
          	Paging page = new Paging(lastpostid);
            ResponseList<twitter4j.Status> statuses = twitter.getHomeTimeline(page);
            for(int i = statuses.size()-1; i >= 0; --i)
            {
            	twitter4j.Status status = statuses.get(i);
              tlitems.addFirst(new PostArrayItem(status.getUser().getScreenName(), status.getText(), status.getCreatedAt()));
              Integer contentValue = (statuses.size()-i)*10000/statuses.size();
              publishProgress(contentValue);
            }
            if(statuses.size() > 0)
            	lastpostid = statuses.get(0).getId();
          }catch(TwitterException te)
          {
          	te.printStackTrace();
          }    	    	
				}
				return null;
      }
      
      @Override
      protected void onProgressUpdate(Integer... progress) {
          setProgress(progress[0]);
      }

      @Override
      protected void onPostExecute(Void result) {
          super.onPostExecute(result);
          
    			timeline_adapter.notifyDataSetChanged();
    			
					//終了後
				  TabHost tabHost = getTabHost();
				  tabHost.setCurrentTab(0);
					setProgressBarVisibility(false);
					setProgressBarIndeterminateVisibility(false);
					refreshable = true;
      }
    }
    
    class myUserStreamAdapter extends UserStreamAdapter
    {
    	//新しいツイートを取得する度に呼ばれる
    	@Override
    	public void onStatus(Status status)
    	{
    		super.onStatus(status);
        tlitems.addFirst(new PostArrayItem(status.getUser().getScreenName(), status.getText(), status.getCreatedAt()));
        lastpostid = status.getId();
        mHandler.post(new Runnable(){

					@Override
					public void run() {
						timeline_adapter.notifyDataSetChanged();
					}
        	
        });
    	}
    }
    
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			// TODO 自動生成されたメソッド・スタブ
			
		}
}
