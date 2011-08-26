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

package com.dubsar_dictionary.Dubsar.test;

import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.Button;

import com.dubsar_dictionary.Dubsar.DubsarContentProvider;
import com.dubsar_dictionary.Dubsar.DubsarService;
import com.dubsar_dictionary.Dubsar.MainActivity;
import com.dubsar_dictionary.Dubsar.R;
import com.dubsar_dictionary.Dubsar.model.Model;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
	
	private int mId = 0;
	private String mWotdText = null;

	public MainActivityTest() {
		super("com.dubsar_dictionary.Dubsar", MainActivity.class);
	}
	
	protected void setUp() {
		Model.addMock("/wotd", "[25441,\"resourcefully\",\"adv\",0,\"\"]");
		/*
		 * Get the last sticky broadcast and mock whatever its ID is. It will
		 * usually not be generated by the mock above.
		 * 
		 * DEBT: If this test is run when the device is not connected to the
		 * network, this will blow up. (Because there will be no ID in the 
		 * broadcast, and the wotdWord button will be disabled.)
		 */
		TestReceiver receiver = new TestReceiver();
		IntentFilter filter = new IntentFilter(DubsarService.ACTION_WOTD);

		Intent broadcast = getActivity().registerReceiver(receiver, filter);
		if (broadcast != null) {
			mId = broadcast.getExtras().getInt(BaseColumns._ID);
			mWotdText = broadcast.getExtras().getString(DubsarService.WOTD_TEXT);
		}
		else {
			mId = 25441;
			mWotdText = "resourcefully (adv.)";
		}
		
		getActivity().unregisterReceiver(receiver);
	}
	
	/* not sure why this hangs at the moment
	public void testWotd() {
		Button wotdWord = (Button)getActivity().findViewById(R.id.wotd_word);
		
		assertEquals(mWotdText, wotdWord.getText());
	}
	 */
	
	@UiThreadTest
	public void testBackButton() {
		Model.addMock("/words/" + mId,
				"[" + mId + ",\"resourcefully\",\"adv\",\"\",[[34828,[],\"in a resourceful manner  \",\"adv.all\",null,0]],0]");

		Instrumentation instr = getInstrumentation();

		Button wotdWord = (Button)getActivity().findViewById(R.id.wotd_word);

		wotdWord.performClick();
		
		Button backButton = (Button)getActivity().findViewById(R.id.left_arrow);
		assertNotNull(backButton);
		backButton.performClick();
		
		// cheat?
		instr.callActivityOnResume(getActivity());
		
		Button fwdButton = (Button)getActivity().findViewById(R.id.right_arrow);
		assertNotNull(fwdButton);
		assertTrue(fwdButton.isEnabled());	
	}
	
	protected static class TestReceiver extends BroadcastReceiver {
		
		public int id=0;
		public String text=null;
		public String nameAndPos=null;
		public String errorMessage=null;

		@Override
		public void onReceive(Context context, Intent intent) {
			assertEquals(intent.getAction(), DubsarService.ACTION_WOTD);
			
			Bundle extras = intent.getExtras();
			assertNotNull(extras);
			
			id = extras.getInt(BaseColumns._ID);
			text = extras.getString(DubsarService.WOTD_TEXT);
			nameAndPos = extras.getString(DubsarContentProvider.WORD_NAME_AND_POS);
			errorMessage = extras.getString(DubsarService.ERROR_MESSAGE);
		}
		
	}
}
