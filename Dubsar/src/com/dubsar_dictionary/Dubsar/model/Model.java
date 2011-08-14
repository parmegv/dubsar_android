/*
 Dubsar Dictionary Project
 Copyright (C) 2010-11 Jimmy Dee
 
 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/**
 * Package for Dubsar model code.
 */
package com.dubsar_dictionary.Dubsar.model;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONTokener;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.dubsar_dictionary.Dubsar.R;

/**
 * Base class for all models. Provides communications
 * and a basic JSON parsing framework.
 */
public abstract class Model {

	/**
	 * Enumeration to represent parts of speech. 
	 */
	public enum PartOfSpeech {
		Unknown,
		Adjective,
		Adverb,
		Conjunction,
		Interjection,
		Noun,
		Preposition,
		Pronoun,
		Verb
	}
	
	/**
	 * Convenience method to convert the abbreviated pos name
	 * to an enumerated value.
	 * @param pos a string abbreviation (e.g. "adj" or "n")
	 * @return the corresponding enumerated part of speech (Unknown on failure)
	 */
	public static PartOfSpeech partOfSpeechFromPos(String pos) {
		if (pos.equals(getString(R.string.pos_adj))) {
			return PartOfSpeech.Adjective;
		}
		if (pos.equals(getString(R.string.pos_adv))) {
			return PartOfSpeech.Adverb;
		}
		if (pos.equals(getString(R.string.pos_conj))) {
			return PartOfSpeech.Conjunction;
		}
		if (pos.equals(getString(R.string.pos_interj))) {
			return PartOfSpeech.Interjection;
		}
		if (pos.equals(getString(R.string.pos_n))) {
			return PartOfSpeech.Noun;
		}
		if (pos.equals(getString(R.string.pos_prep))) {
			return PartOfSpeech.Preposition;
		}
		if (pos.equals(getString(R.string.pos_pron))) {
			return PartOfSpeech.Pronoun;
		}
		if (pos.equals(getString(R.string.pos_v))) {
			return PartOfSpeech.Verb;
		}
		
		return PartOfSpeech.Unknown;
	}
	
	/**
	 * Convenience function to return an abbreviated string (e.g., "adj" or
	 * "v") given the corresponding enumerated PartOfSpeech.
	 * @param partOfSpeech an enumerated value
	 * @return the corresponding string value ("unk" on failure)
	 */
	public static String posFromPartOfSpeech(PartOfSpeech partOfSpeech) {
		int id;
		
		switch (partOfSpeech) {
		case Adjective:
			id = R.string.pos_adj;
			break;
		case Adverb:
			id = R.string.pos_adv;
			break;
		case Conjunction:
			id = R.string.pos_conj;
			break;
		case Interjection:
			id = R.string.pos_interj;
			break;
		case Noun:
			id = R.string.pos_n;
			break;
		case Preposition:
			id = R.string.pos_prep;
			break;
		case Pronoun:
			id = R.string.pos_pron;
			break;
		case Verb:
			id = R.string.pos_v;
			break;
		default:
			id = R.string.pos_unk;
			break;
		}
		
		return getString(id);
	}
	
	private static HttpClient sClient=null;
	private static HashMap<String,String> sMocks=new HashMap<String,String>();

	/**
	 * This protected member variable is set by child classes.
	 * The full URL is generated by prepending 
	 * R.string.dubsar_base_url.
	 */
	protected String mPath;
	
	/**
	 * This protected member variable is populated by the base
	 * class for the child class to parse in parseData().
	 */
	protected String mData;
	
	private static WeakReference<Context> mContextReference=null;
	
	private String mUrl=null;
	private volatile boolean mComplete=false;
	private volatile boolean mError=false;
	private String mErrorMessage=null;
	
	/**
	 * Get the URL associated with this model.
	 * @return this model's full URL
	 */
	public final String getUrl() {
		if (mUrl == null) mUrl = getString(R.string.dubsar_base_url) + mPath;
		return mUrl;
	}
	
	/**
	 * Has this request finished receiving its response?
	 * @return true if response received; false otherwise
	 */
	public boolean isComplete() {
		return mComplete;
	}
	
	/**
	 * Did this request generate an error? This value is
	 * not meaningful unless isComplete().
	 * @return true if the request returned an error; false otherwise
	 */
	public boolean hasError() {
		return mError;
	}
	
	/**
	 * If hasError(), this will return a non-null error
	 * message for the end user.
	 * @return a human-readable error message
	 */
	public final String getErrorMessage() {
		return mErrorMessage;
	}
	
	/**
	 * Set the raw data buffer (useful for testing).
	 * @param data a raw JSON buffer to parse
	 */
	public void setData(String data) {
		mData = new String(data);
	}
	
	/**
	 * Set the context to use for all model instances when
	 * converting strings, e.g.
	 * @param context the context
	 */
	public static void setContext(Context context) {
		mContextReference = new WeakReference<Context>(context);
	}
	
	/**
	 * The context used for string lookups by the Model class
	 * @return the Context
	 */
	public static Context getContext() {
		return mContextReference != null ? mContextReference.get() : null;
	}
	
	/**
	 * Static convenience function
	 * @param id a numeric string ID (R.string.hello, e.g.)
	 * @return the string value
	 */
	public static final String getString(int id) {
		if (getContext() == null) return null;
		
		return getContext().getString(id);
	}
	
	/**
	 * Fetch data synchronously from the server.
	 */
	public void load() {
		try {
			/* simple HTTP mock for testing */
			mData = getMock();
			if (mData == null) mData = fetchData();
			
			JSONTokener tokener = new JSONTokener(mData);
			parseData(tokener.nextValue());			
		}
		/* JSONException, ClientProtocolException and IOException
		 * For now (and perhaps indefinitely), we handle them all alike.
		 */
		catch (Exception e) {
			mError = true;
			mErrorMessage = e.getMessage();
		}
		
		mComplete = true;
	}
	
	/**
	 * Must be overridden by child classes to parse the
	 * JSON payload in mData.
	 * @param jsonResponse an object, typically a JSONArray or a JSONObject
	 * @throws JSONException if the tokener encounters a parsing error
	 */
	public abstract void parseData(Object jsonResponse) throws JSONException;
	
	/**
	 * Add a JSON mock for testing. If a mock is present, its value is
	 * returned to the model instead of requesting data from the server.
	 * @param path a path to mock
	 * @param json the JSON response to return for this path instead of querying the server
	 */
	public static void addMock(String path, String json) {
		sMocks.put(path, json);
	}
	
	/**
	 * Get the JSON mock for this path (or null)
	 * @param path a path to check
	 * @return the associated JSON mock
	 */
	public static final String getMock(String path) {
		return sMocks.get(path);
	}
	
	/**
	 * Get this model's mock (if any)
	 * @return the JSON mock associated with this model's path (null if none)
	 */
	public final String getMock() {
		return getMock(mPath);
	}
	
	/**
	 * Create or return the common HttpClient used by all model requests.
	 * @return the HttpClient
	 */
	protected HttpClient getClient() {
		if (sClient == null) {
			String userAgent = getString(R.string.user_agent);
			userAgent += " (" + getContext().getString(R.string.android_version, 
					new Object[]{Build.VERSION.RELEASE});
			userAgent += "; " + getContext().getString(R.string.build, new Object[]{Build.DISPLAY});
			userAgent += ")";
			sClient = AndroidHttpClient.newInstance(userAgent);
		}
		return sClient;
	}
	
	/**
	 * Retrieve JSON data for this model instance from the server.
	 * @return the JSON payload
	 * @throws IOException in case of communication error
	 */
	protected String fetchData() throws IOException {
		ResponseHandler<String> handler = new BasicResponseHandler();
		
		// DEBT: Take from strings file
		Header header = new BasicHeader("Accept", "application/json");
		HttpGet request = new HttpGet(getUrl());
		request.addHeader(header);
		
		return getClient().execute(request, handler);
	}
}
