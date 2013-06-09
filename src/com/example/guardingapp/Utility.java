package com.example.guardingapp;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class Utility {
	
	/*
	 * Function to select Application
	 */
	public static byte[] selectApplication(IsoDep nfc, byte[] AID){
		try{
			byte[] cmdApdu = new byte[6 + AID.length];
			cmdApdu[0] = (byte)0x90;
			cmdApdu[1] = (byte)0x5a;
			cmdApdu[2] = (byte)0x00;
			cmdApdu[3] = (byte)0x00;
			cmdApdu[4] = (byte)AID.length;
			System.arraycopy(AID, 0, cmdApdu, 5, AID.length);
			cmdApdu[5 + AID.length] = (byte)0x00;
			Log.i("Debug: select application cmd: ", toHexString(cmdApdu));
			return nfc.transceive(cmdApdu);
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}
	}
	/*
	 * Send authentication command. The parameter is the key number
	 */
	public static byte[] sendAuthentication(IsoDep nfc, byte keyNo){
		//byte[] authen_data = {(byte)0x90, (byte)0x0a, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00};
		try{
			byte[] cmdApdu = new byte[7];
			cmdApdu[0] = (byte)0x90;
			cmdApdu[1] = (byte)0x0a;
			cmdApdu[2] = (byte)0x00;
			cmdApdu[3] = (byte)0x00;
			cmdApdu[4] = (byte)0x01;
			cmdApdu[5] = keyNo;
			cmdApdu[6] = (byte)0x00;
			Log.i("Debug: send authentication cmd: ", toHexString(cmdApdu));
			return nfc.transceive(cmdApdu);
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * Send request to server and get resonse
	 * need to rewrite this
	 */
	public static String sendToServer(String requestedURL){
		String returnedStr = "";
		Log.i("requested_url", requestedURL);
		HttpGet request = new HttpGet(requestedURL);
		
		request.setHeader("Accept", "text/plain");
		request.setHeader("Content-type", "text/plain");
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		try{
			HttpResponse response = httpclient.execute(request);
			HttpEntity responseEntity = response.getEntity();
			String value = EntityUtils.toString(responseEntity);
			returnedStr = value;
			Log.i("Value of returned Str = ", returnedStr);
			return returnedStr;
		}
		catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}
	
	/*
	 * send decrypted_RndA_RndB' to tag
	 */
	
	public static byte[] send_decrypted_RndA_rotatedRndB(IsoDep nfc, byte[] data){
		try{
			byte[] cmdApdu = new byte[data.length + 6];
			cmdApdu[0] = (byte)0x90;
			cmdApdu[1] = (byte)0xaf;
			cmdApdu[2] = (byte)0x00;
			cmdApdu[3] = (byte)0x00;
			cmdApdu[4] = (byte)data.length;
			System.arraycopy(data, 0, cmdApdu, 5, data.length);
			cmdApdu[5 + data.length] = (byte)0x00;
		
			byte[] encRndA;			
			encRndA = nfc.transceive(cmdApdu);
			Log.i("Debug: send decrypted rndA-rotated rndb: ", toHexString(cmdApdu));
			return encRndA;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	/*
	 * 
	 */
	public static byte[] writeDataToTag(IsoDep nfc){
		byte[] selectCcFile_response = selectCCFile(nfc);
		
		if (isCorrectExecution(selectCcFile_response)){
		
			byte[] readCCFile_response = readCCFile(nfc);
			
			if (isCorrectExecution(readCCFile_response)){
			
				byte[] selectNdef_response = NdefSelect(nfc);
				
				return selectNdef_response;
			}
		}
		return null;
	}
	
	/*
	 * check if a command is executed correctly
	 */
	public static boolean isCorrectExecution(byte[] resApdu){
		if (resApdu.length >= 2) {
			byte sw1 = resApdu[resApdu.length - 2];
			byte sw2 = resApdu[resApdu.length - 1];
			if (sw1 != (byte) 0x90 || sw2 != (byte) 0x00) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	/*
	 * Select CC file
	 */
	public static byte[] selectCCFile(IsoDep nfc){
		try{
			byte[] cmdApdu = new byte[]{(byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x0C, (byte)0x02, (byte)0xE1, (byte)0x03};
			byte[] selectCcFile_response = nfc.transceive(cmdApdu);
			return selectCcFile_response;
		}
		catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	/*
	 * Read from CC file
	 */
	
	public static byte[] readCCFile(IsoDep nfc){
		try{
			byte[] cmdApdu = new byte[]{(byte)0x00, (byte)0xB0, (byte)0x00, (byte)0x00, (byte)0x0F};
			byte[] CcFile = nfc.transceive(cmdApdu);
			return CcFile;
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}
	}
	
	/*
	 * NDEF select
	 */
	public static byte[] NdefSelect(IsoDep nfc){
		try{
			byte[] cmdApdu = new byte[]{(byte)0x00, (byte)0xA4, (byte)0x00, (byte)0x0C, (byte)0x02, (byte)0xE1, (byte)0x04};
			byte[] response = nfc.transceive(cmdApdu);
			return response;
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		}
	}
	
	/*
	 * 
	 */
	public static boolean checkResponse(byte[] resApdu){
		if (resApdu == null)
			return false;
		if (resApdu.length >= 2) {
			byte sw1 = resApdu[resApdu.length - 2];
			byte sw2 = resApdu[resApdu.length - 1];
			if (sw1 != (byte) 0x91 || sw2 != (byte) 0x00)
				return false;
		}
		return true;
	}
	
	/*
	 * Check if an additional frame is requested
	 */
	public static boolean isAdditionalFrameRequested(byte[] resApdu){
		if (resApdu == null)
			return false;
		if (resApdu.length >= 2) {
			byte sw1 = resApdu[resApdu.length - 2];
			byte sw2 = resApdu[resApdu.length - 1];
			if (sw1 != (byte) 0x91 || sw2 != (byte) 0xaf)
				return false;
		}
		return true;
	}
	//convert bytes to a string of hex
	public static String toHexString(byte[] b) {
	    StringBuffer sb = new StringBuffer();
	    for (int i = 0; i < b.length; i++){
	    	sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
	    }
	    return sb.toString();
	}
		
	//convert a string of hex to bytes
	public static byte[] toByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
}
