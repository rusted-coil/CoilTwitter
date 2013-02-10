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
    
    //OAuth�f�[�^�ۑ��p
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    
    TextView tv;
    int a;
    
    String postImagePath = "";
    
    //twitter���n���h��
    Handler mHandler = new Handler();
    
    boolean postable = true;
    boolean refreshable = true;
    
    //�����̃A�J�E���g���
		String userid;
		
    //View
    ListView timeline;
    //���X�g�p
    LinkedList<PostArrayItem> tlitems = new LinkedList<PostArrayItem>();
    ArrayAdapter<PostArrayItem> timeline_adapter;
    long lastpostid;
    
    //���[�U�[�X�g���[��
    TwitterStream userStream;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        // �v���O���X�\��
        requestWindowFeature(Window.FEATURE_PROGRESS);
      	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_main);

        initTabs();
        
        setTitle("�A�J�E���g�F�ؒ�");

        //OAuth�F�؃f�[�^�ǂݍ���
        pref = getSharedPreferences("t4jdata", Activity.MODE_PRIVATE);
        token=pref.getString("token", "");
        token_secret=pref.getString("token_secret", "");
 
        //OAuth�f�[�^�������F�؋N��
        if(token.length()==0)
        {
	        Intent intent = new Intent(this, OAuthActivity.class);
	        intent.putExtra(OAuthActivity.CALLBACK, CALLBACK);
	        intent.putExtra(OAuthActivity.CONSUMER_KEY, CONSUMER_KEY);
	        intent.putExtra(OAuthActivity.CONSUMER_SECRET, CONSUMER_SECRET);
	        startActivityForResult(intent, REQUEST_OAUTH);
        }
        
        //Intent�̎擾
        Intent cameraIntent = getIntent();
        //�摜��Uri�擾
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
        
        //���[�U�[�X�g���[���̗p��
  			userStream = getTwitterStream();
  			userStream.addListener(new myUserStreamAdapter());
    }
    
    protected void ResetTimeline()
    {
      tlitems.clear();
      
      //TL�̎擾
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
      //TL�̎擾
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
    
    //�摜
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
        // OAuth����
        if (requestCode == REQUEST_OAUTH && resultCode == RESULT_OK) 
        {
            user_id=intent.getLongExtra(OAuthActivity.USER_ID, 0L);
            screen_name=intent.getStringExtra(OAuthActivity.SCREEN_NAME);
            token=intent.getStringExtra(OAuthActivity.TOKEN);
            token_secret=intent.getStringExtra(OAuthActivity.TOKEN_SECRET);
             
//            tv.setText(token_secret);
             
            //�F�؃f�[�^�ۑ�
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
    	menu.add(Menu.NONE, 1, Menu.NONE, "�F�؂̎�����");
    	menu.add(Menu.NONE, 2, Menu.NONE, "TL�̍X�V");
    	menu.add(Menu.NONE, 3, Menu.NONE, "���[�U�[�X�g���[���̊J�n");
    	menu.add(Menu.NONE, 4, Menu.NONE, "���[�U�[�X�g���[���̒�~");
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch(item.getItemId())
    	{
    		case 1:
          //OAuth�F�؃f�[�^�ǂݍ���
    			pref = getSharedPreferences("t4jdata", Activity.MODE_PRIVATE);
    			editor = pref.edit();
    			editor.clear().commit();
   
          //OAuth�f�[�^�������F�؋N��
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
  	   		
        	//post���e�̎擾
					posttext = (EditText)findViewById(R.id.posttext);
					statusup = new StatusUpdate(posttext.getText().toString());
      }

      @Override
      protected Void doInBackground(String... params) 
      {
      	AccessToken accessToken = null;
        if(params != null)
        {					
					//�摜���A�b�v���[�h���邩
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
					//�I����
				  posttext.setText("");
					
				  TabHost tabHost = getTabHost();
				  tabHost.setCurrentTab(0);
				  // �\�t�g�L�[�{�[�h���\���ɂ���
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
          //TL�̎擾
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
    			
					//�I����
				  TabHost tabHost = getTabHost();
				  tabHost.setCurrentTab(0);
					setProgressBarVisibility(false);
					setProgressBarIndeterminateVisibility(false);
					refreshable = true;
      }
    }
    
    class myUserStreamAdapter extends UserStreamAdapter
    {
    	//�V�����c�C�[�g���擾����x�ɌĂ΂��
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
			// TODO �����������ꂽ���\�b�h�E�X�^�u
			
		}
}
