package cbosoft.shaun;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Scene;
import android.util.Log;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class Home extends Activity {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// ATTRIBUTES ///////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // UI
    final String defaultFont = "fonts/dosvga.ttf";
    // Main
    TextView shInfo, shSuggested;
    TextClock shClock, shDate;
    InputBuffer shSTDIN;

    // Scenes
    ViewGroup rootScene;
    Scene hiddenScene;

    // Shell
    String shPrompt, shBlank;
    String pCommand;
    Typeface tf;

    List<String> appNameList;
    List<String> appNameList_searchable;
    Map<String, String> ALIASES;
    Map<String, Integer> input2type;
    Map<String, String> input2andrapp;
    Map<String, String> ignoreapps;

    SharedPreferences shPref;
    SharedPreferences.Editor shPrefEditor;

    TextWatcher tWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            Log.d(TAG, "afterTextChanged: " + s);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            inputTextChanged(s, start, before, count);
        }
    };

    class AppFetcher extends Thread {
        AppFetcher() { }

        public void run() {
            setupAppMaps();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// INHERITED METHODS ////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rootlayout);
        this.hideStatusBar();


        setupPreferences();
        setupLayout();

        setupAppMaps();
        // or async:
        //AppFetcher appFetcher = new AppFetcher();
        //appFetcher.start();


        setupScenes();
        setSubInfo();

    }

    @Override
    protected void onResume() {
        super.onResume();
        this.shPrompt = getPrompt();
        this.resetStdin();
    }

    @Override
    protected void onPause(){
        super.onPause();
        // anything else to do?
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// (RE) INIT ////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    void setupPreferences() {
        shPref = getPreferences(MODE_PRIVATE);
        shPrefEditor = shPref.edit();
        defaultiseShPref("uname", "user");
        defaultiseShPref("hname", "droid");
        defaultiseShPref("font", defaultFont);
        shPrefEditor.apply();
        shPrefEditor = null;
    }

    void setupLayout() {
        tf = Typeface.createFromAsset(getAssets(), shPref.getString("font", defaultFont));
        shSTDIN = findViewById(R.id.shSTDIN);
        shInfo = findViewById(R.id.subinf);
        shClock = findViewById(R.id.shlock);
        shDate = findViewById(R.id.shdate);
        shSuggested = findViewById(R.id.op);

        shSTDIN.setTypeface(tf);
        shInfo.setTypeface(tf);
        shClock.setTypeface(tf);
        shDate.setTypeface(tf);
        shSuggested.setTypeface(tf);

        shPrompt = getPrompt();
        shSTDIN.setText(shPrompt);
        try {
            shSTDIN.removeTextChangedListener(tWatcher);
        } catch (Exception e) {/* Pass */}
        shSTDIN.addTextChangedListener(tWatcher);
        shSTDIN.requestFocus();
    }

    void setupAppMaps() {
        Log.i(TAG, "SETTING UP APP MAPS");
        List<ApplicationInfo> lApps;
        lApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        ignoreapps = new HashMap<>();
        ignoreapps.put("google", "");

        input2type = new HashMap<>();
        input2type.put("url", 2);
        input2type.put("shset", 2);
        input2type.put("apud", 2);

        ALIASES = new HashMap<>();
        ALIASES.put("play", "google play store");
        ALIASES.put("amz", "amazon shopping");
        ALIASES.put("apls", "app drawer");
        ALIASES.put("fm", "file manager");
        ALIASES.put("cal", "calendar");
        ALIASES.put("ukp", "url https://www.reddit.com/r/ukpolitics");
        ALIASES.put("cam", "blackberry camera");

        input2andrapp = new HashMap<>();
        appNameList = new ArrayList<>();
        for (int i = 0; i < lApps.size(); i++) {
            ApplicationInfo ai = lApps.get(i);
            PackageManager pm = getPackageManager();
            String label = ai.loadLabel(pm).toString().toLowerCase();
            if (ignoreapps.get(label) == null && ai.enabled && pm.getLaunchIntentForPackage(ai.packageName) != null) {
                appNameList.add(label);
                input2andrapp.put(label, ai.packageName);
                input2type.put(label, 1);
            }
        }
        appNameList.addAll(ALIASES.keySet());
    }

    void setupScenes() {
        Log.i(TAG, "SETTING UP SCENES");
        // init scenes
        rootScene = findViewById(R.id.root);
        hiddenScene = Scene.getSceneForLayout(rootScene, R.layout.hidden, this);
    }

    void defaultiseShPref(String key, String defval){
        String res = shPref.getString(key, "unset");
        if (res.equals("unset")) {
            if (shPrefEditor != null) {
                // editor already made: bulk changes
                shPrefEditor.putString(key, defval);
            }
            else {
                // no editor: remake here
                shPrefEditor = shPref.edit();
                shPrefEditor.putString(key, defval);
                shPrefEditor.apply();
            }
        }
    }

    String[] trimArr(String[] toTrim) {
        if (toTrim == null) return null;
        ArrayList<String> tt = new ArrayList<>(Arrays.asList(toTrim));

        for (int i = 0; i < toTrim.length; i++){
            int j = toTrim.length - i - 1;
            if (toTrim[j] == null || toTrim[j].equals("")){
                tt.remove(j);
            }
        }

        return tt.toArray(new String[tt.size()]);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// UI & CONTROL /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    void alert() {
        vibe(500);
    }

//    void notif() {
//        vibe(100);
//    }

    void vibe(int mil) {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            v.vibrate(mil);
        }
        catch (java.lang.NullPointerException ex) {/* Pass */}
    }

    void inputTextChanged(CharSequence s, int start, int before, int count) {
        suggestApps("");
        
        if (s.length() < 1) {
            // somehow, there are no contents in the stdin buffer...
            return;
        }

        if (s.length() < shPrompt.length() || !s.toString().startsWith(shPrompt)){
            // trying to delete characters? naughty naughty...
            setStdin(shPrompt);
            return;
        }

        if (count < before){
            suggestApps(s.toString().substring(shPrompt.length()));
            // otherwise deleting chars is fine...
            return;
        }
        // Woo! Typed a character!

        char c = s.charAt(start);
        if (c == '\n') {
            String ss = s.toString().replaceAll("\n", "");
            setStdin(ss);

            // do something with input
            String entered = ss.substring(shPrompt.length());
            if (shSuggested.getVisibility() == View.VISIBLE && appNameList_searchable.size() > 0) {
                entered = appNameList_searchable.get(appNameList_searchable.size() - 1);
                setStdin(shPrompt + entered);
            }
            dealWithInput(entered);
        }
        else {
            suggestApps(s.toString().substring(shPrompt.length()));
        }
    }

    void dealWithInput(String entered) {
        int rv = shTabComplete(entered);
        if (rv == 2 || rv == 3) {
            pCommand = entered;
        }
        switch (rv) {
            case 0: // no match
                alert();
                resetStdin();
                break;
            case 1: // partial match
                break;
            case 2: // total match
                if (!entered.contains(" ")) {
                    shExec(entered, null);
                }
                else {
                    String command = entered.substring(0, entered.indexOf(" "));
                    String[] args = entered.substring(entered.indexOf(" ")).split(" ");
                    shExec(command, args);
                }
                break;
            case 3:
                shExec(entered, null);
                break;
        }
    }

    int shTabComplete(String entered) {
        // takes "entered" text and compares to shApps
        // gets most complete app name
        // returns an int indicating how the completion went:
        //      0: did not match anything
        //      1: partially complete
        //      3: complete + unique

        String command;
        if (entered.contains(" ")){
            command = entered.substring(0, entered.indexOf(" "));
        }
        else {
            command = entered;
        }

        String bestMatchName;
        int bestMatchLength;

        Log.d(TAG, "shTabComplete: pre alias");
        String aliasres = ALIASES.get(command);
        if (aliasres != null) {
            // setStdin(shPrompt + aliasres);
            dealWithInput(aliasres);
            return 1;
        }

        Log.d(TAG, "shTabComplete: pre ain2ty");
        Object res = input2type.get(entered.toLowerCase());
        if (res != null) {
            // android app or built in app, no args, command contains " "
            return 3;
        }

        res = input2type.get(command.toLowerCase());
        if (res != null) {
            // android app or built in app, with args, command contains no " "
            return 2;
        }


        Tuple<String, Integer> match = getBestMatch(entered, input2andrapp.keySet());
        // do other autocompletion stuff

        bestMatchName = match.x;
        bestMatchLength = match.y;

        if (bestMatchLength > 0) {
            // setStdin(shPrompt + bestMatchName);
            shExec(bestMatchName, null);
            return 1;
        }

        return 0;
    }

    Tuple<String, Integer> getBestMatch(String toCheck, Set<String> against){
        int bestMatchLength = 0;
        String bestMatchName = "";

        if (against.contains(toCheck)) return new Tuple<>(toCheck, -1);

        for (String k: against) {
            String sub_check;
            for (int j = 0; j < toCheck.length(); j++) {
                sub_check = toCheck.substring(0, toCheck.length() - j).toLowerCase();
                if (k.toLowerCase().startsWith(sub_check)) {
                    if (toCheck.length() - j > bestMatchLength) {
                        bestMatchName = k;
                        bestMatchLength = toCheck.length() - j;
                        Log.d(TAG, "shTabComplete: potential match: " + k);
                        break;
                    }
                }
            }
        }

        if (bestMatchLength > 0) {
            return new Tuple<>(bestMatchName, bestMatchLength);
        }
        else {
            return new Tuple<>(toCheck, 0);
        }
    }

    void setSubInfo(){
        String version = "vXX", text = "";
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {
            // pass
        }
        text += "[" + version + "]";
        shInfo.setText(text);
    }

    void hideStatusBar(){
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    void refreshUI(){
        shPrompt = getPrompt();
        tf = Typeface.createFromAsset(getAssets(), shPref.getString("font", "fonts/inconsolata.ttf"));
        setupLayout();
    }

    public void clockClicked(View v) {
        shExec("clock", null);
    }

    public void calClicked(View v) {
        shExec("calendar", null);
    }

    void suggestApps(String entered) {

        if (entered.length() == 0) {
            shSuggested.setVisibility(View.INVISIBLE);
            return;
        }
        else{
            shSuggested.setVisibility(View.VISIBLE);
        }

        if (entered.length() == 1) {
            appNameList_searchable = new ArrayList<>();
            for (String appName: appNameList) {
                if (appName.startsWith(entered)) {
                    appNameList_searchable.add(appName);
                }
            }
        }
        else {
            List<String> temp = new ArrayList<>();
            for (String appName: appNameList_searchable) {
                if (appName.startsWith(entered)) {
                    temp.add(appName);
                }
            }
            appNameList_searchable = temp;
        }
        StringBuilder sb = new StringBuilder();
        for (String appName: appNameList_searchable) {
            sb.append("\n");
            sb.append(shBlank);
            sb.append(appName);
        }
        sb.append("\n");
        shSuggested.setText(sb.toString());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// SHELL STUFF //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public String getPrompt(){
        String shprompt = shPref.getString("uname", getResources().getString(R.string.uname)) + "@" + shPref.getString("hname", getResources().getString(R.string.hname)) + "> ";
        shSTDIN.minSel = shprompt.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shprompt.length(); i++) {
            sb.append(" ");
        }
        shBlank = sb.toString();
        return shprompt;
    }

    public void resetStdin(){
        this.shSTDIN.setText(shPrompt, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(shPrompt.length());
    }

    public void setStdin(String ss) {
        this.shSTDIN.setText(ss, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(ss.length());
    }

    public void shExec(String command, String[] args) {
        Log.d(TAG, "shExec: " + command);
        resetStdin();

        if (args != null) args = trimArr(args);

        if (input2type.get(command) == 1) {
            Intent li = getPackageManager().getLaunchIntentForPackage(input2andrapp.get(command));
            startActivity(li);
        }
        else {

            try {
                if (args != null) {
                    for (int i = 0; i < args.length; i++) {
                        if (args[i].equals("|")) {
                            if (args[i + 1].equals("grep")) {
                                String nc = "grep";
                                String[] na = new String[args.length - 1];
                                na[0] = args[i + 2]; // pattern
                                na[1] = command; // command
                                System.arraycopy(args, 0, na, 2, args.length - 3);

                                command = nc;
                                args = na;

                                Log.d(TAG, "shExec: NC: " + nc);
                                for (String s : na) {
                                    Log.d(TAG, "shExec:  ARGS:  " + s);
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {/*Pass*/}

            if (args == null) args = new String[0];

            switch (command) {
                case "url":
                    shb_url(args);
                    break;
                case "shset":
                    shb_shset(args);
                    break;
                case "apud":
                    shb_apmud();
                    break;
                default:
                    alert();
                    break;

            }
        }
    }

    void shb_url(String[] args) {
        boolean fine = true;

        if (args == null) { fine = false; }
        else if (args.length < 1){ fine = false; }

        if (!fine) {
            alert();
            return;
        }

        if (!args[0].startsWith("https://") && !args[0].startsWith("http://")){
            args[0] = "http://" + args[0];
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(args[0]));
        startActivity(i);
    }

    void shb_shset(String[] args) {

        boolean fine = true;

        if (args == null){fine = false;}
        else if (args.length == 0){fine = false;}
        else if (args.length == 1 && !args[0].equals("help")){fine = false;}
        else if (args.length > 2){fine = false;}

        if (!fine){
            alert();
        }
        else {
            shPref = getPreferences(MODE_PRIVATE);
            shPrefEditor = shPref.edit();
            if (args[0].equals("font")) {
                if (!args[1].endsWith(".ttf")) {
                    args[1] = args[1] + ".ttf";
                }
                if (!args[1].startsWith("fonts/")) {
                    args[1] = "fonts/" + args[1];
                }
                try {
                    Typeface.createFromAsset(getAssets(), args[1]);
                }
                catch (RuntimeException ex) {
                    Log.d(TAG, "shb_shset: " + ex.getMessage());
                    alert();
                    return;
                }
            }
            shPrefEditor.putString(args[0], args[1]);
            shPrefEditor.apply();
            refreshUI();
        }
    }

    void shb_apmud() {
        AppFetcher appFetcher = new AppFetcher();
        appFetcher.start();

    }


}
