package com.mp.unityandroid;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

/**
 * Created by garyNoh on 2017. 6. 12..
 */

public class JasonBrain {

    Context context;

    public JasonBrain(Context context){
        this.context = context;
    }

    public String searchContact(String target){
        //대상을 주소록에서 찾는다
        String targetContact;
        targetContact = getPhoneNumber(target, context);

        //주소록에서 찾을 수 없음
        if(targetContact.equals("Unsaved")){
            return "I can't find " + target + " on your list";

        }
        //주소록에서 찾을 수 있음
        else{
            return "OK" + targetContact;
        }
    }

    /**
     * 연락처 검색
     * @param name : 검색어
     * @param context
     * @return : 결과값 (찾으면 전화번호를 string 으로, 못 찾으면 Unsaved )
     */
    public String getPhoneNumber(String name, Context context) {
        String searchResult = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" like'%" + name +"%'";
        String[] projection = new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if(c == null) return null;

        if (c.moveToFirst()) {
            searchResult = c.getString(0);
        }
        c.close();
        if(searchResult==null)
            searchResult = "Unsaved";
        return searchResult;
    }
}
