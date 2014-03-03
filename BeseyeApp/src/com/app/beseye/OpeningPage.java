package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;


import com.app.beseye.util.BeseyeUtils;
import com.codebutler.android_websockets.WebSocketClient;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class OpeningPage extends BeseyeBaseActivity {
	private static boolean sbFirstLaunch = true;
	private static final long TIME_TO_CLOSE_OPENING_PAGE = 3000L;
	private ImageView[] mIvHeartList = new ImageView[22];
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		getSupportActionBar().hide();
		if(!sbFirstLaunch){
			launchNextPage();
		}
//		if(SPECIAL_MODE){
//			mIvHeartList[0] = (ImageView)findViewById(R.id.iv_heart1);
//			mIvHeartList[1] = (ImageView)findViewById(R.id.iv_heart2_1);
//			mIvHeartList[2] = (ImageView)findViewById(R.id.iv_heart3_1);
//			mIvHeartList[3] = (ImageView)findViewById(R.id.iv_heart4_1);
//			mIvHeartList[4] = (ImageView)findViewById(R.id.iv_heart5_1);
//			mIvHeartList[5] = (ImageView)findViewById(R.id.iv_heart6_1);
//			mIvHeartList[6] = (ImageView)findViewById(R.id.iv_heart7_1);
//			mIvHeartList[7] = (ImageView)findViewById(R.id.iv_heart8_1);
//			mIvHeartList[8] = (ImageView)findViewById(R.id.iv_heart9_1);
//			mIvHeartList[9] = (ImageView)findViewById(R.id.iv_heart10_1);
//			mIvHeartList[10] = (ImageView)findViewById(R.id.iv_heart11_1);
//			mIvHeartList[11] = (ImageView)findViewById(R.id.iv_heart12_1);
//			mIvHeartList[12] = (ImageView)findViewById(R.id.iv_heart11_2);
//			mIvHeartList[13] = (ImageView)findViewById(R.id.iv_heart10_2);
//			mIvHeartList[14] = (ImageView)findViewById(R.id.iv_heart9_2);
//			mIvHeartList[15] = (ImageView)findViewById(R.id.iv_heart8_2);
//			mIvHeartList[16] = (ImageView)findViewById(R.id.iv_heart7_2);
//			mIvHeartList[17] = (ImageView)findViewById(R.id.iv_heart6_2);
//			mIvHeartList[18] = (ImageView)findViewById(R.id.iv_heart5_2);
//			mIvHeartList[19] = (ImageView)findViewById(R.id.iv_heart4_2);
//			mIvHeartList[20] = (ImageView)findViewById(R.id.iv_heart3_2);
//			mIvHeartList[21] = (ImageView)findViewById(R.id.iv_heart2_2);
//			for(int i = 0; i< mIvHeartList.length;i++){
//				mIvHeartList[i].setVisibility(View.INVISIBLE);
//			}	
//		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_opening;
		//return !SPECIAL_MODE?R.layout.layout_opening:R.layout.layout_opening_heart;
	}

	@Override
	protected void onResume() {
		super.onResume();
//		if(SPECIAL_MODE){
//			mHeartRunnable = new HeartRunnable();
//			mIvHeartList[0].postDelayed(mHeartRunnable, 2000);
//		}else{
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					launchNextPage();
				}
			}, TIME_TO_CLOSE_OPENING_PAGE);
			sbFirstLaunch = false;
//		}
	}
	
	@Override
	protected void onPause() {
//		if(mHeartRunnable!= null){
//			mIvHeartList[0].removeCallbacks(mHeartRunnable);
//			mHeartRunnable = null;
//			for(int i = 0; i< mIvHeartList.length;i++){
//				mIvHeartList[i].setVisibility(View.INVISIBLE);
//			}
//		}
		super.onPause();
	}
	
//	//private static boolean SPECIAL_MODE = false;
//	private class HeartRunnable implements Runnable{
//		int idx = 0;
//		int iExtraStep = 0;
//		boolean bFirstRound = true;
//		@Override
//		public void run() {
//			if(bFirstRound){
//				if(idx > mIvHeartList.length/2){
//					if(0 == iExtraStep || 2 == iExtraStep || 4 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						}
//						iExtraStep++;
//					}else if(1 == iExtraStep || 3 == iExtraStep ){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						iExtraStep++;
//						
//					}else if(5 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						idx = 0;
//						iExtraStep = 0;
//						bFirstRound = false;
//						mIvHeartList[0].postDelayed(this, 2000);
//						return;
//					}
//					mIvHeartList[0].postDelayed(this, 500);
//				}else{
//					if(idx == 0){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						}
//					}
//					if(idx == 0)
//						mIvHeartList[idx].setVisibility(View.VISIBLE);
//					else{
//						mIvHeartList[idx].setVisibility(View.VISIBLE);
//						mIvHeartList[mIvHeartList.length-idx].setVisibility(View.VISIBLE);
//					}
//					mIvHeartList[0].postDelayed(this, idx == mIvHeartList.length/2?500:100);
//					idx+=1;
//				}
//			}else{
//				if(idx > mIvHeartList.length/2){
//					if(0 == iExtraStep || 2 == iExtraStep || 4 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						}
//						iExtraStep++;
//					}else if(1 == iExtraStep || 3 == iExtraStep ){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						iExtraStep++;
//						
//					}else if(5 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						idx = 0;
//						iExtraStep = 0;
//						bFirstRound = true;
//						mIvHeartList[0].postDelayed(this, 2000);
//						return;
//					}
//					mIvHeartList[0].postDelayed(this, 500);
//				}else{
//					for(int i = 0; i< mIvHeartList.length;i++){
//						if((idx == i || (mIvHeartList.length - idx ) == i) && i != mIvHeartList.length/2)
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						else{
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//					}
//					mIvHeartList[0].postDelayed(this, idx == mIvHeartList.length/2?500:100);
//					idx+=1;
//				}
//			}
//		}
//	}
//	
//	private HeartRunnable mHeartRunnable = null;
	WebSocketClient client;
	private void launchNextPage(){
		Intent intent = new Intent();
		intent.setClass(OpeningPage.this, CameraViewActivity.class);//WifiListActivity.class);
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
