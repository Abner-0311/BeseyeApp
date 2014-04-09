package com.app.beseye;

import com.app.beseye.util.BeseyeUtils;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

public class OpeningPage extends BeseyeBaseActivity {
	private static boolean sbFirstLaunch = true;
	private static final long TIME_TO_CLOSE_OPENING_PAGE = 3000L;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		getSupportActionBar().hide();
		if(!sbFirstLaunch){
			launchNextPage();
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_opening;
	}

	@Override
	protected void onResume() {
		super.onResume();
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				launchNextPage();
			}
		}, TIME_TO_CLOSE_OPENING_PAGE);
		sbFirstLaunch = false;
	}

	//WebSocketClient client;
	private void launchNextPage(){
		Intent intent = new Intent();
		intent.setClass(OpeningPage.this, LoginActivity.class);//WifiListActivity.class);, CameraSettingActivity.class
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
