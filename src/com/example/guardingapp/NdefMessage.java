package com.example.guardingapp;

/**
 * Class for creating NDEF message
 * Note: It only support a single NDEF record.
 * 
 * @author Sandeep Tamrakar
 */
public class NdefMessage {
	/**
	 * Indicates the NDEF message
	 */
	public static final byte NDEF_MESSAGE_HEADER = 0x03;
	
	/**
	 * Indicates the end of NDEF message
	 */
	public static final byte NDEF_MESSAGE_END = (byte) 0xFE;
	
	/**
	 * an NDEF record
	 */
	private static NdefRecord mRecord;
	
	/**
	 * Constructor for the class NdefMessage. 
	 * It creates an NdefMessage with an empty
	 * NdefRecord object
	 */
	public NdefMessage() {
		mRecord = new NdefRecord();
	}
	
	/**
	 * Constructor for the class NdefMessage
	 * It creates an NdefMessage with a give NdefRecord
	 * @param record an NdefRecord
	 */
	public NdefMessage(NdefRecord record) {
		mRecord = record;
	}
	
	/**
	 * Returns an NdefMessage that contains an NdefRecord with supplied uri data
	 * @param uri String defining the URI string
	 * @return NdefMessage
	 */
	public static NdefMessage fromUriString(String uri) {
		NdefRecord record = NdefRecord.fromUriString(uri);
		return new NdefMessage(record);
	}
	
	/**
	 * Returns the NdefMessage byte array value
	 * @return byte array
	 */
	public byte[] toByteArray() {
		byte[] pl = mRecord.getPayload();
		for (int i = 0; i < pl.length; i++){
			System.out.printf("%02x   ", pl[i]);
		}
		byte[] type = mRecord.getType();
		
		int size = pl.length + 4;	
		//byte[] returnArray = {(byte) 0x03, (byte) 0x03, (byte) 0xD0,
			//				(byte) 0x00, (byte) 0x00, (byte) 0xFE};
		
		byte [] returnArray = new byte[size];
		
		returnArray[0] = (byte)0xD1;
		returnArray[1] = (byte)0x01;
		returnArray[2] = (byte)pl.length;
		returnArray[3] = (byte)type[0];
		
		for (int i = 0; i < pl.length; i++){
			returnArray[4 + i] = (byte)pl[i];
		}
		
		return returnArray;	
	}
	
	/**
	 * Creates an NdefMessage from the given byte array 
	 * @param data byte array of the NdefMessage
	 * @return NdefMessage
	 */
	public static NdefMessage fromByteArray(byte[] data) {
		
		/* 
		 * TODO: create an NDEF message from the given data. 	
		 */
		
		mRecord = NdefRecord.fromByteArray(null);
		NdefMessage message = new NdefMessage(mRecord);
		return message;
	}
}
