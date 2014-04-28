package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.websockets.BeseyeWebsocketsUtil;
import com.app.beseye.websockets.WebsocketsMgr;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class OpeningPage extends Activity {
	public static final String ACTION_BRING_FRONT 		= "BringFront";
	public static final String KEY_DELEGATE_INTENT 		= "KEY_DELEGATE_INTENT";
	public static final String FIRST_PAGE 				= CameraViewActivity.class.getName();
	
	private static boolean sbFirstLaunch = true;
	private static final long TIME_TO_CLOSE_OPENING_PAGE = 3000L;
	
	private boolean m_bLaunchForDelegate = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//if(sbFirstLaunch)
		setContentView(R.layout.layout_opening);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		if(getIntent().getBooleanExtra(ACTION_BRING_FRONT, false)){
			finish();
			return;
		}
		
		launchActivityByIntent(getIntent());
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onNewIntent(), intent "+intent.getDataString()+", this = "+this);
		super.onNewIntent(intent);
		if(null != intent && null == intent.getParcelableExtra(KEY_DELEGATE_INTENT)){
			String strCls = intent.getStringExtra("ClassName");
			if(null == strCls){
				if(DEBUG)
					Log.i(TAG, "OpeningPage::onNewIntent(), null == strCls ");
				finish();
				return;
			}

			if(intent.getBooleanExtra(ACTION_BRING_FRONT, false)){
				if(DEBUG)
					Log.i(TAG, "OpeningPage::onNewIntent(), ACTION_BRING_FRONT ");
				finish();
				return;
			}
		}
		
		launchActivityByIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(false == m_bLaunchForDelegate)
			finish();
		m_bLaunchForDelegate = false;
		
		sbFirstLaunch = false;
	}
	
	private void launchActivityByIntent(Intent intent){
		Intent intentLanuch = null;
		if(null == (intentLanuch = intent.getParcelableExtra(KEY_DELEGATE_INTENT))){
			intentLanuch = new Intent();
			String strCls = null;
			if(null != intent){
				strCls = intent.getStringExtra("ClassName");
			}
			
			if(null == strCls){
				strCls = FIRST_PAGE;
			}
			
			if(!SessionMgr.getInstance().isTokenValid()){
				strCls = BeseyeEntryActivity.class.getName();
			}
			
			if(null != intent.getExtras())
				intentLanuch.putExtras(intent.getExtras());
			
			intentLanuch.setClassName(this, strCls);
		}else{
			//Try to close push dialog when launch from status bar
			Intent intentBroadcast = new Intent(GCMIntentService.FORWARD_GCM_MSG_ACTION);
			intentBroadcast.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, GCMIntentService.FORWARD_ACTION_TYPE_CHECK_DIALOG);
	        sendBroadcast(intentBroadcast);
		}
		
		//intentLanuch.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		if(sbFirstLaunch){
			final Intent intentLanuchRunnable =intentLanuch;
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					startActivity(intentLanuchRunnable);
				}
			}, TIME_TO_CLOSE_OPENING_PAGE);
		}else{
			startActivity(intentLanuch);
		}

		m_bLaunchForDelegate = true;
	}
	
	

	//WebSocketClient client;
	private void launchNextPage(){
		Intent intent = new Intent();
		intent.setClass(OpeningPage.this, BeseyeEntryActivity.class);//WifiListActivity.class);, CameraSettingActivity.class
		startActivity(intent);
		finish();
		
//		if(null == client){
//			List<BasicNameValuePair> extraHeaders = Arrays.asList(
//				    new BasicNameValuePair("Cookie", "session=abcd")
//				);
//			
//			client = new WebSocketClient(URI.create("ws://54.199.196.101:3001/websocket"), new WebSocketClient.Listener() {
//			    @Override
//			    public void onConnect() {
//			        Log.d(TAG, "Connected!");
//					client.send("[\"wsc_connected\", {\"data\":\"\"}]");
//					Log.d(TAG, "Connected! 1");
//					//client.send("[\"wsc_keep_alive\", {\"data\":\"\"}]");
//					client.send("[\"wsc_loopback\", {\"data\":\"echo\"}]");
//					Log.d(TAG, "Connected! 2");
//					client.send("[\"wsc_keep_alive\", {\"data\":\"\"}]");
//			    }
//
//			    @Override
//			    public void onMessage(String message) {
//			        Log.d(TAG, String.format("Got string message! %s", message));
//			    }
//
//			    @Override
//			    public void onMessage(byte[] data) {
//			        //Log.d(TAG, String.format("Got binary message! %s", toHexString(data)));
//			    }
//
//			    @Override
//			    public void onDisconnect(int code, String reason) {
//			        Log.d(TAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
//			    }
//
//			    @Override
//			    public void onError(Exception error) {
//			        Log.e(TAG, "Error!", error);
//			    }
//
//			}, null);
//
//			client.connect();
//
//			// Later¡K 
//			//client.send("hello!");
//			
//			//client.send(new byte[] { 0xDE, 0xAD, 0xBE, 0xEF });
//			//client.disconnect();
//		}
		
//		Draft d = new Draft_17();
//		
//		AutobahnClientTest e = new AutobahnClientTest( d, URI.create("ws://54.199.196.101:3001/websocket") );
//		Thread t = new Thread( e );
//		t.start();
//		try {
//			t.join();
//		} catch ( InterruptedException e1 ) {
//			e1.printStackTrace();
//		} finally {
//			Log.i(TAG, "launchNextPage(), call close" );
//			//e.close();
//		}
	}
}


//class AutobahnClientTest extends WebSocketClient {
//
//	public AutobahnClientTest( Draft d , URI uri ) {
//		super( uri, d );
//	}
//	/**
//	 * @param args
//	 */
//	public static void main( String[] args ) {
//		Log.i(TAG, "Testutility to profile/test this implementation using the Autobahn suit.\n" );
//		Log.i(TAG, "Type 'r <casenumber>' to run a testcase. Example: r 1" );
//		Log.i(TAG, "Type 'r <first casenumber> <last casenumber>' to run a testcase. Example: r 1 295" );
//		Log.i(TAG, "Type 'u' to update the test results." );
//		Log.i(TAG, "Type 'ex' to terminate the program." );
//		Log.i(TAG, "During sequences of cases the debugoutput will be turned of." );
//
//		Log.i(TAG, "You can now enter in your commands:" );
//
//		try {
//			BufferedReader sysin = new BufferedReader( new InputStreamReader( System.in ) );
//
//			/*First of the thinks a programmer might want to change*/
//			Draft d = new Draft_17();
//			String clientname = "tootallnate/websocket";
//
//			String protocol = "ws";
//			String host = "localhost";
//			int port = 9001;
//
//			String serverlocation = protocol + "://" + host + ":" + port;
//			String line = "";
//			AutobahnClientTest e;
//			URI uri = null;
//			String perviousline = "";
//			String nextline = null;
//			Integer start = null;
//			Integer end = null;
//
//			while ( !line.contains( "ex" ) ) {
//				try {
//					if( nextline != null ) {
//						line = nextline;
//						nextline = null;
//						WebSocketImpl.DEBUG = false;
//					} else {
//						System.out.print( ">" );
//						line = sysin.readLine();
//						WebSocketImpl.DEBUG = true;
//					}
//					if( line.equals( "l" ) ) {
//						line = perviousline;
//					}
//					String[] spl = line.split( " " );
//					if( line.startsWith( "r" ) ) {
//						if( spl.length == 3 ) {
//							start = new Integer( spl[ 1 ] );
//							end = new Integer( spl[ 2 ] );
//						}
//						if( start != null && end != null ) {
//							if( start > end ) {
//								start = null;
//								end = null;
//							} else {
//								nextline = "r " + start;
//								start++;
//								if( spl.length == 3 )
//									continue;
//							}
//						}
//						uri = URI.create( serverlocation + "/runCase?case=" + spl[ 1 ] + "&agent=" + clientname );
//
//					} else if( line.startsWith( "u" ) ) {
//						WebSocketImpl.DEBUG = false;
//						uri = URI.create( serverlocation + "/updateReports?agent=" + clientname );
//					} else if( line.startsWith( "d" ) ) {
//						try {
//							d = (Draft) Class.forName( "Draft_" + spl[ 1 ] ).getConstructor().newInstance();
//						} catch ( Exception ex ) {
//							Log.i(TAG, "Could not change draft" + ex );
//						}
//					}
//					if( uri == null ) {
//						Log.i(TAG, "Do not understand the input." );
//						continue;
//					}
//					Log.i(TAG, "//////////////////////Exec: " + uri.getQuery() );
//					e = new AutobahnClientTest( d, uri );
//					Thread t = new Thread( e );
//					t.start();
//					try {
//						t.join();
//
//					} catch ( InterruptedException e1 ) {
//						e1.printStackTrace();
//					} finally {
//						e.close();
//					}
//				} catch ( ArrayIndexOutOfBoundsException e1 ) {
//					Log.i(TAG, "Bad Input r 1, u 1, d 10, ex" );
//				} catch ( IllegalArgumentException e2 ) {
//					e2.printStackTrace();
//				}
//
//			}
//		} catch ( ArrayIndexOutOfBoundsException e ) {
//			Log.i(TAG, "Missing server uri" );
//		} catch ( IllegalArgumentException e ) {
//			e.printStackTrace();
//			Log.i(TAG, "URI should look like ws://localhost:8887 or wss://echo.websocket.org" );
//		} catch ( IOException e ) {
//			e.printStackTrace(); // for System.in reader
//		}
//		System.exit( 0 );
//	}
//
//	@Override
//	public void onMessage( String message ) {
//		Log.i(TAG, "onMessage(), message= "+message );
//		send( message );
//	}
//
//	@Override
//	public void onMessage( ByteBuffer blob ) {
//		getConnection().send( blob );
//	}
//
//	@Override
//	public void onError( Exception ex ) {
//		Log.i(TAG, "Error: " );
//		ex.printStackTrace();
//	}
//
//	@Override
//	public void onOpen( ServerHandshake handshake ) {
//		Log.i(TAG, "onOpen: " );
//		//send("[\"wsc_connected\", {\"data\":\"\"}]");
//		//Log.w(TAG, "onOpen(), send wsc_loopback");
//		//send("[\"wsc_loopback\", {\"data\":\"Abner Test\"}]");
//	}
//
//	@Override

//	public void onClose( int code, String reason, boolean remote ) {
//		Log.i(TAG, "Closed: " + code + ", reason: " + reason );
//	}
//
//	//@Override
//	public void onWebsocketMessageFragment( WebSocket conn, Framedata frame ) {
//		FrameBuilder builder = (FrameBuilder) frame;
//		builder.setTransferemasked( true );
//		getConnection().sendFrame( frame );
//	}
//
//}
