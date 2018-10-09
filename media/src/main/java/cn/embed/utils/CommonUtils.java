package cn.embed.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by User on 2015/12/14.
 */
public class CommonUtils {
    public final static int EXTERNAL_STORAGE_REQ_CODE = 10;
    private static Gson mGson;
    private static final String GSON_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public static SimpleDateFormat formatDay = new SimpleDateFormat("d", Locale.getDefault());
    public static SimpleDateFormat formatMonthDay = new SimpleDateFormat("M-d", Locale.getDefault());
    public static SimpleDateFormat formatDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    /**
     * 设置每个阶段时间
     */
    private static final int seconds_of_1minute = 60;
    private static final int seconds_of_30minutes = 30 * 60;
    private static final int seconds_of_1hour = 60 * 60;
    private static final int seconds_of_1day = 24 * 60 * 60;
    private static final int seconds_of_15days = seconds_of_1day * 15;
    private static final int seconds_of_30days = seconds_of_1day * 30;
    private static final int seconds_of_6months = seconds_of_30days * 6;
    private static final int seconds_of_1year = seconds_of_30days * 12;


    /**
     * 中国移动拥有号码段为:134 135 136 137 138 139 147 150 151 152 157 158 159 178 182 183 184 187 188
     * 19个号段 中国联通拥有号码段为:130 131 132 145 155 156 175 176 185 186;10个号段
     * 中国电信拥有号码段为:133 153 177 180 181 189;6个号码段
     * 虚拟运营商:170
     */
    private static String regMobileStr = "^1(([3][456789])|([4][7])|([5][012789])|([7][8])|([8][23478]))[0-9]{8}$";
    private static String regUnicomStr = "^1(([3][012])|([4][5])|([5][56])|([7][5])|([8][56]))[0-9]{8}$";
    private static String regTelecomStr = "^1(([3][3])|([5][3])|([7][07])|([8][019]))[0-9]{8}$";

    private CommonUtils() {
    }


    /**
     * return if str is empty
     *
     * @param str
     * @return
     */
    public static boolean isEmpty(@Nullable String str) {
        if (str == null || str.length() == 0 || str.equalsIgnoreCase("null") || str.isEmpty() || str.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * return if str is empty
     *
     * @param str
     * @return
     */
    public static boolean isNotEmpty(@Nullable String str) {
        return !isEmpty(str);
    }

    /**
     * 判断是否为空
     *
     * @param str
     * @return
     */
    public static boolean isEmpty(@Nullable CharSequence str) {
        if (str == null || str.length() == 0 || "null".equals(str))
            return true;
        else
            return false;
    }

    public static boolean isNotNull(@Nullable Object o) {
        if (o != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 验证手机号是否合法
     *
     * @param mobile
     * @return
     */
    public static boolean isMobileLawful(String mobile) {
        if (!isEmpty(mobile)) {
            /** */
            /** 第一步判断中国移动 */
            if (mobile.matches(CommonUtils.regMobileStr)) {
                return true;
            }
            /** */
            /** 第二步判断中国联通 */
            if (mobile.matches(CommonUtils.regUnicomStr)) {
                return true;
            }
            /** */
            /** 第三步判断中国电信 */
            if (mobile.matches(CommonUtils.regTelecomStr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证json合法性
     *
     * @param jsonContent
     * @return
     */
    public static boolean isJsonFormat(String jsonContent) {
        try {
            new JsonParser().parse(jsonContent);
            return true;
        } catch (JsonParseException e) {
            return false;
        }
    }

    /**
     * 格式化日期
     *
     * @param date
     * @return 年月日
     */
    public static String formatDate(Date date) {
        return formatDate.format(date);
    }

    /**
     * 格式化日期
     *
     * @param date
     * @return 年月日 时分秒
     */
    public static String formatDateTime(Date date) {
        return formatDateTime.format(date);
    }

    /**
     * 将时间戳解析成日期
     *
     * @param timeInMillis
     * @return 年月日
     */
    public static String parseDate(long timeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        Date date = calendar.getTime();
        return formatDate(date);
    }

    /**
     * 将时间戳解析成日期
     *
     * @param timeInMillis
     * @return 年月日 时分秒
     */
    public static String parseDateTime(long timeInMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeInMillis);
        Date date = calendar.getTime();
        return formatDateTime(date);
    }

    /**
     * HH:mm:ss
     *
     * @param time
     * @return
     */
    public static String formatLong(Long time) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        String d = format.format(time);
        return d;
    }

    public static String secToTime(int time) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (time <= 0)
            return "00:00";
        else {
            minute = time / 60;
            if (minute < 60) {
                second = time % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = time - hour * 3600 - minute * 60;
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    public static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }

    /**
     * 解析日期
     *
     * @param date
     * @return
     */
    public static Date parseDate(String date) {
        Date mDate = null;
        try {
            mDate = formatDate.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return mDate;
    }

    /**
     * 解析日期
     *
     * @param datetime
     * @return
     */
    public static Date parseDateTime(String datetime) {
        Date mDate = null;
        try {
            mDate = formatDateTime.parse(datetime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return mDate;
    }

    /**
     * 对指定字符串进行md5加密
     *
     * @param s
     * @return 加密后的数据
     */
    public static String EncryptMD5(String s) {
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            byte[] btInput = s.getBytes();
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 判断email格式是否正确
     *
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {
        String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);
        return m.matches();
    }

    /**
     * 判断是否是纯数字
     *
     * @return true 是纯数字  false 不是纯数字
     */
    public static boolean isDigit(String str) {
        Pattern p = Pattern.compile("[0-9]*");
        Matcher m = p.matcher(str);
        return m.matches();
    }


    /**
     * Try to return the absolute file path from the given Uri
     *
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /**
     * 获取gson对象
     *
     * @return
     */
    public static Gson getGson() {
        if (mGson == null) {
            mGson = new GsonBuilder().setDateFormat(GSON_FORMAT).create(); // 创建gson对象，并设置日期格式
        }

        return mGson;
    }



    /**
     * 播放音乐
     *
     * @param context
     */
    public static void playMusic(Context context, int id) {
        MediaPlayer mp = MediaPlayer.create(context, id);
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
    }

    /**
     * 获取联系人电话
     *
     * @param cursor
     * @return
     */
    private String getContactPhone(Context context, Cursor cursor) {

        int phoneColumn = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
        int phoneNum = cursor.getInt(phoneColumn);
        String phoneResult = "";
        //System.out.print(phoneNum);
        if (phoneNum > 0) {
            // 获得联系人的ID号
            int idColumn = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            String contactId = cursor.getString(idColumn);
            // 获得联系人的电话号码的cursor;
            Cursor phones = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                    null, null);
            //int phoneCount = phones.getCount();
            //allPhoneNum = new ArrayList<String>(phoneCount);
            if (phones.moveToFirst()) {
                // 遍历所有的电话号码
                for (; !phones.isAfterLast(); phones.moveToNext()) {
                    int index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int typeindex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                    int phone_type = phones.getInt(typeindex);
                    String phoneNumber = phones.getString(index);
                    switch (phone_type) {
                        case 2:
                            phoneResult = phoneNumber;
                            break;
                    }
                    //allPhoneNum.add(phoneNumber);
                }
                if (!phones.isClosed()) {
                    phones.close();
                }
            }
        }
        return phoneResult;
    }

    public static void toggleInputKeyboard(Context context) {
        InputMethodManager m = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        m.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    public static boolean isInputKeboardOpen(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm.isActive();
    }

    /**
     * Hides the soft keyboard
     */
    public static void hideSoftKeyboard(Context context) {
        Activity activity = (Activity) context;
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Shows the soft keyboard
     */
    public static void showSoftKeyboard(Context context, View view) {
        Activity activity = (Activity) context;
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(activity.INPUT_METHOD_SERVICE);
        view.requestFocus();
        inputMethodManager.showSoftInput(view, 0);
    }

    /**
     * 判断email格式是否正确
     *
     * @param s
     * @return
     */
    public static boolean digital(CharSequence s) {
        String str = "^[0-9]*$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(s);
        return m.matches();
    }


    public static String formatNumber(String num) {
        int number = Integer.parseInt(num);
        if (number < 10000) {
            return number + "";
        } else if (number < 1000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "万";
        } else if (number < 10000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.0");
            String result = df.format(x);
            return result + "万";
        } else if (number < 100000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0");
            String result = df.format(x);
            return result + "万";
        } else {
            Double x = ((double) number) / 100000000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "亿";
        }
    }

    public static String formatNumber(double number) {
        if (number == 0) {
            return "0";
        }
        if (number < 10000) {
            return number + "";
        } else if (number < 1000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "万";
        } else if (number < 10000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.0");
            String result = df.format(x);
            return result + "万";
        } else if (number < 100000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0");
            String result = df.format(x);
            return result + "万";
        } else {
            Double x = ((double) number) / 100000000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "亿";
        }
    }

    public static String formatNumber(int number) {
        if (number < 10000) {
            return number + "";
        } else if (number < 1000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "万";
        } else if (number < 10000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.0");
            String result = df.format(x);
            return result + "万";
        } else if (number < 100000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0");
            String result = df.format(x);
            return result + "万";
        } else {
            Double x = ((double) number) / 100000000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "亿";
        }
    }


    public static String formatNumber(long number) {
        if (number < 10000) {
            return number + "";
        } else if (number < 1000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "万";
        } else if (number < 10000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0.0");
            String result = df.format(x);
            return result + "万";
        } else if (number < 100000000) {
            Double x = ((double) number) / 10000D;
            DecimalFormat df = new DecimalFormat("0");
            String result = df.format(x);
            return result + "万";
        } else {
            Double x = ((double) number) / 100000000D;
            DecimalFormat df = new DecimalFormat("0.00");
            String result = df.format(x);
            return result + "亿";
        }
    }

    public static final void openGPS(Context context) {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings",
                "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    /**
     * 格式化钱的数量
     *
     * @param money
     * @return
     */
    public static String formatMoney(double money) {
        DecimalFormat df;
        df = (DecimalFormat) DecimalFormat.getInstance();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        df.setGroupingSize(3);
        return df.format(money);
    }

    /**
     * 格式化钱的数量
     *
     * @param money
     * @return
     */
    public static String formatMoney(String money) {
        double sub = Double.parseDouble(money);
        DecimalFormat df;
        df = (DecimalFormat) DecimalFormat.getInstance();
        df.setMaximumFractionDigits(2);
        df.setMinimumFractionDigits(2);
        df.setGroupingSize(3);
        return df.format(sub);
    }

    /**
     * 限制文本输入框的内容，小数点后2位，小数点前7位
     *
     * @param s       //editText 改变后的的状态
     * @param etText  //需要判断的edittext
     * @param context //activity 的上下文
     */
    public static void checkEditText(Editable s, EditText etText, Context context) {
        int mSelStart = etText.getSelectionStart();
        int mSelEnd = etText.getSelectionEnd();
        try {
            if (!CommonUtils.isEmpty(s)) {
                String price = s.toString();
                if (price.contains(".")) {
                    String[] split = price.split("\\.");
                    if (split[0].length() > 7) {
                        Toast.makeText(context, "小数点前面最多7位", Toast.LENGTH_SHORT).show();
                        s.delete(mSelStart - 1, mSelEnd);
                        etText.setTextKeepState(s);
                    }
                    if (split.length < 2)
                        return;
                    if (split[1].length() > 2) {
                        Toast.makeText(context, "小数后面最多2位", Toast.LENGTH_SHORT).show();
                        s.delete(mSelStart - 1, mSelEnd);
                        etText.setTextKeepState(s);
                    }
                } else {
                    if (price.length() > 7) {
                        Toast.makeText(context, "最多7位", Toast.LENGTH_SHORT).show();
                        s.delete(mSelStart - 1, mSelEnd);
                        etText.setTextKeepState(s);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param str
     * @return
     */
    public static int parseColorString2Int(String str) {
        if (str != null) {
            str = str.trim();
        }
        String[] rgba = str.split(",");
        return Color.argb((int) (Float.parseFloat(rgba[3]) * 255), Integer.parseInt(rgba[0]), Integer.parseInt(rgba[1]), Integer.parseInt(rgba[2]));
    }


    /**
     * 杂志详情 从字符串 获取颜色
     */
    public static int[] parseColorFromString(String str) {
        if (str != null) {
            str = str.trim();
        }
        if (str.equals("")) {
            return new int[]{0, 0, 0, 0};
        }
        String[] distances = str.split(",");
        int[] distances1 = new int[4];
        for (int i = 0; i < 4; i++) {
            distances1[i] = (int) Float.parseFloat(distances[i]);
            //            distances1[i] = (int) Float.parseFloat(distances[i]);
        }
        return distances1;
    }

    /**
     * 杂志详情 颜色与透明度拼接
     */
    public static String parseColorAndAlph(String color, String alph) {
        if (color != null) {
            color = color.trim();
        }
        String[] strings = color.split(",");
        strings[3] = alph;
        return strings[0] + "," + strings[1] + "," + strings[2] + "," + strings[3];
    }

    /**
     * @param str    要设置的字符串
     * @param length 要设置的长度
     * @return
     */
    public static String setStringLength(String str, int length) {
        if (str.length() <= length)
            return str;
        return str.substring(0, length) + "...";
    }

    /**
     * 判断杂志详情文字是否为默认
     */
    public static boolean isDefaultTextColor(String str) {
        if (str.equals("1,1,1,1") || str.equals("0,0,0,1") || str.equals("254,254,254,1") || str.equals("0,0,0,1") || str.equals("1,1,1,1") || str.equals("1," +
                "1,1,1.0") || str.equals("0,0,0,1.0") || str.equals("254,254,254,1.0")) {
            return true;
        } else {
            return false;
        }

    }


    /**
     * 截图view
     */
    public static Bitmap getBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.RGB_565);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        // Draw background
        Drawable bgDrawable = v.getBackground();
        if (bgDrawable != null)
            bgDrawable.draw(c);
        else
            c.drawColor(Color.WHITE);
        // Draw view to canvas
        v.draw(c);
        return b;
    }


    /**
     * 格式化时间
     *
     * @param mTime
     * @return
     */
    public static String getTimeRange(long mTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //获取当前时间
        long currentTime = System.currentTimeMillis();
        /**除以1000是为了转换成秒*/
        long between = (currentTime - mTime) / 1000;
        long elapsedTime = (between);
        if (elapsedTime < 0) {
            return "未来";
        }
        if (elapsedTime < seconds_of_1minute) {
            return "刚刚";
        }
        if (elapsedTime < seconds_of_30minutes) {
            return elapsedTime / seconds_of_1minute + "分钟前";
        }
        if (elapsedTime < seconds_of_1hour) {
            return "30分钟前";//半小时前
        }
        if (elapsedTime < seconds_of_1day) {
            return elapsedTime / seconds_of_1hour + "小时前";
        }
        if (elapsedTime < seconds_of_15days) {
            return elapsedTime / seconds_of_1day + "天前";
        }
        if (elapsedTime < seconds_of_30days) {
            return "15天前";//半个月前
        }
        if (elapsedTime < seconds_of_6months) {
            return elapsedTime / seconds_of_30days + "月前";
        }
        if (elapsedTime < seconds_of_1year) {
            return "6个月前";//半年前
        }
        if (elapsedTime >= seconds_of_1year) {
            Date d1 = new Date(elapsedTime);
            String t1 = sdf.format(d1);
            return elapsedTime / seconds_of_1year + "年前";
        }
        return "";
    }

    /**
     * 格式化时间
     *
     * @param mTime
     * @return
     */
    public static String getTimeRange(String mTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        /**获取当前时间*/
        Date curDate = new Date(System.currentTimeMillis());
        String dataStrNew = sdf.format(curDate);
        Date startTime = null;
        try {
            /**将时间转化成Date*/
            curDate = sdf.parse(dataStrNew);
            startTime = sdf.parse(mTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        /**除以1000是为了转换成秒*/
        long between = (curDate.getTime() - startTime.getTime()) / 1000;
        int elapsedTime = (int) (between);
        if (elapsedTime < seconds_of_1minute) {
            return "刚刚";
        }
        if (elapsedTime < seconds_of_30minutes) {
            return elapsedTime / seconds_of_1minute + "分钟前";
        }
        if (elapsedTime < seconds_of_1hour) {
            return "30分钟前";
        }
        if (elapsedTime < seconds_of_1day) {
            return elapsedTime / seconds_of_1hour + "小时前";
        }
        if (elapsedTime < seconds_of_15days) {
            return elapsedTime / seconds_of_1day + "天前";
        }
        if (elapsedTime < seconds_of_30days) {
            return "15天前";
        }
        if (elapsedTime < seconds_of_6months) {
            return elapsedTime / seconds_of_30days + "月前";
        }
        if (elapsedTime < seconds_of_1year) {
            return "6个月前";
        }
        if (elapsedTime >= seconds_of_1year) {
            return elapsedTime / seconds_of_1year + "年前";
        }
        return "";
    }

    /**
     * 复制粘贴板
     */
    public static void copyString(Context context, String string) {
        ClipboardManager cmb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cmb.setText(string);
    }

    public static String stampToDate(String s) {
        String res;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long lt = new Long(s);
        Date date = new Date(lt);
        res = simpleDateFormat.format(date);
        return res;
    }

    /**
     * 解析html语言
     *
     * @param html
     * @return
     */
    public static Spanned parseHtml(String html) {
        if (CommonUtils.isEmpty(html)) {
            html = "";
        }
        return Html.fromHtml(html);
    }


    public static void setColor(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 设置状态栏透明
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 生成一个状态栏大小的矩形
            View statusView = createStatusView(activity, color);
            // 添加 statusView 到布局中
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            decorView.addView(statusView);
            // 设置根布局的参数
            ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            rootView.setFitsSystemWindows(true);
            rootView.setClipToPadding(true);
        }
    }

    private static View createStatusView(Activity activity, int color) {
        // 获得状态栏高度
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = activity.getResources().getDimensionPixelSize(resourceId);

        // 绘制一个和状态栏一样高的矩形
        View statusView = new View(activity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                statusBarHeight);
        statusView.setLayoutParams(params);
        statusView.setBackgroundColor(color);
        return statusView;
    }

    public static void setTranslucent(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 设置状态栏透明
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 设置根布局的参数
            ViewGroup rootView = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            rootView.setFitsSystemWindows(false);
            rootView.setClipToPadding(false);
        }
    }

    /**
     * 几月几号
     *
     * @param time
     */
    public static String setFormatData(long time) {
        try {
            Date date = new Date(time);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM月dd日");
            String timeString = dateFormat.format(date);
            return timeString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 切换时间格式
     *
     * @param oldTime
     * @param oldFormat
     * @param newFormat
     */
    public static String changeFormat(String oldTime, String oldFormat, String newFormat) {
        try {
            SimpleDateFormat oldformat = new SimpleDateFormat(oldFormat);
            Date oldDate = oldformat.parse(oldTime);
            SimpleDateFormat newformat = new SimpleDateFormat(newFormat);
            String timeString = newformat.format(oldDate);
            return timeString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取文件扩展名
     *
     * @param url
     * @return
     */
    public static String getFilenameExtension(String url) {
        String[] names = url.split("\\.");
        if (names.length > 0) {
            return names[names.length - 1];
        }
        return "";
    }

    /**
     * 获取文件扩展名
     *
     * @param url
     * @return
     */
    public static String getFilename(String url) {
        String[] names = url.split("\\.");
        if (names.length > 0) {
            return names[0];
        }
        return "";
    }

    public static boolean writeTxtFile(String content, File fileName) throws Exception {
        RandomAccessFile mm = null;
        boolean flag = false;
        FileOutputStream o = null;
        try {
            o = new FileOutputStream(fileName);
            o.write(content.getBytes("GBK"));
            o.close();
            //   mm=new RandomAccessFile(fileName,"rw");
            //   mm.writeBytes(content);
            flag = true;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            if (mm != null) {
                mm.close();
            }
        }
        return flag;
    }

    //删除文件
    public static void delFile(String fileName) {
        File file = new File(fileName);
        if (file.isFile()) {
            file.delete();
        }
        file.exists();
    }

    /**
     * ffmpeg cmd 命令
     */
    public static void ffmpegCMD(Context context, String cmd, ExecuteBinaryResponseHandler executeBinaryResponseHandler) {
        if (executeBinaryResponseHandler == null) {
            executeBinaryResponseHandler = new ExecuteBinaryResponseHandler();
        }
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            String[] command = cmd.split(" ");

            ffmpeg.execute(command, executeBinaryResponseHandler);
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }
    }


}
