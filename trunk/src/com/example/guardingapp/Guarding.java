

package com.example.guardingapp;

import java.net.URLEncoder;

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;
import android.nfc.Tag;
import android.os.*;
import android.nfc.tech.IsoDep;
import android.nfc.NdefMessage;

public class Guarding extends Activity{
	
	public final static String SERVER_IP = "192.168.0.104";
	public final static int SERVER_PORT = 8080; 
	public final static String URL = "http://" + SERVER_IP + ":" + SERVER_PORT + "/HelloWS/rest/server/";
	
	protected String tag_uid = "";
	
	protected byte[] AID = {(byte)0x01, (byte)0x00, (byte)0x00};
	
	private String tagContent = "";
	
	//protected byte[] AID = {(byte)0x00, (byte)0x00, (byte)0x00};
	protected byte KEY_NUM = 0x00;
	IsoDep nfc;
	NfcAdapter mNfcAdapter;
    TextView textView;
    
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.activity_guarding);
		textView = (TextView) findViewById(R.id.textView);
		
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_guarding, menu);
		return true;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		boolean action = NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction());
		action = action || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction());
		action = action || NfcAdapter.ACTION_TECH_DISCOVERED.equals(getIntent().getAction());
		
		if (action){
			TextView textView = (TextView) findViewById(R.id.textView);
			Intent intent = getIntent();
			
			Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			byte[] id = tagFromIntent.getId();
			tag_uid = Utility.toHexString(id);
			
			//send this message along with tag uid to the app
			try{
				Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
				if (rawMsgs != null){
					NdefMessage msg = (NdefMessage)rawMsgs[0];
					byte[] payload = msg.getRecords()[0].getPayload();
					tagContent = URLEncoder.encode(new String(payload).substring(3), "UTF-8");
				}
				
				nfc = IsoDep.get(tagFromIntent);
				nfc.setTimeout(5000);
				nfc.connect();
				
				new ExchangingMessages().execute();
			}
			catch(Exception e){
				e.printStackTrace();
			}	
		}
	}
	
	@Override
	public void onStop(){
		super.onStop();
		try{
			//socket.shutdownInput();
			//socket.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//the string here is the URL
	private class ExchangingMessages extends AsyncTask<String, Void, String>{
		@Override
	    protected String doInBackground(String...params) {
			try{
				//first, select app. the application id is 0x000001
				byte[] selectApp = Utility.selectApplication(nfc, AID);
				Log.i("Debug: response to select app: ", Utility.toHexString(selectApp));
				
				if (!Utility.checkResponse(selectApp))
					return "Error occured";
				
				//if the tag responds with OPERATION_OK
				//send authentication command with key number of the application
				//SystemClock.sleep(60000);
				byte[] encRndB = Utility.sendAuthentication(nfc, KEY_NUM);
				Log.i("Debug: response to send Auth cmd: ", Utility.toHexString(encRndB));
				if (!Utility.isAdditionalFrameRequested(encRndB))
					return "Error occured";
				
				//response looks like this: 68 80 78 a9 04 ad 6d ba 91 af
				//send this encrypted RndB to the server
				String encRndbURL = URL + "encryptedRndB" + "/" + tag_uid + "/" 
										+ Utility.toHexString(encRndB).substring(0, 16) 
										+ "/" + tagContent;
				
				//get the decrypted RndA_RndB'
				String dec_RndA_rotatedRndB = Utility.sendToServer(encRndbURL);	
				
				//make this response delay
				//SystemClock.sleep(100000);
				byte[] enc_rotatedRndA = Utility.send_decrypted_RndA_rotatedRndB(nfc, Utility.toByteArray(dec_RndA_rotatedRndB));
				Log.i("Debug: response to decrypted rnda-rotated rndb: ", Utility.toHexString(enc_rotatedRndA));
				//forward the encrypted_rotated RndA from the tag to the server
				//encryptedRndA/{uid}/{encryptedRndA}
				if (!Utility.checkResponse(enc_rotatedRndA)){
					//notify the server that the server's response is not correct
					String fail = URL + "encryptedRndA" + "/" + tag_uid + "/Failure" ;
					Utility.sendToServer(fail);
					return "Error occured";
				}
				
				String enc_rotated_RndA_URL = URL + "encryptedRndA" + "/" + tag_uid + "/" 
												   + Utility.toHexString(enc_rotatedRndA).substring(0, 16);
				String result = Utility.sendToServer(enc_rotated_RndA_URL);
				
				//if everything is fine, the server send a command to write data to the tag.
				if (result.equals("Failure"))
					return "Failure";
				
				byte[] writeCmdApdu = Utility.toByteArray(result);
				byte[] writeCmdResponse = nfc.transceive(writeCmdApdu);
				
				if (!Utility.isCorrectExecution(writeCmdResponse))
					return "Success";
				
				return "Writing data to tag fails";
			}
			catch(Exception e){
				e.printStackTrace();
				return "Error occured";
			}
		}		
		
		@Override
	    protected void onPostExecute(String result) {
			textView.setText(result);
			
		}
	}
}

