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
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View;
import android.widget.TextClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class Home extends Activity {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// ATTRIBUTES ///////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // UI
    // defaults
    final String defaultFont = "fonts/dosvga.ttf";
    String clock = "com.android.clock";
    String calendar = "com.google.android.calendar";
    Boolean appsReady = Boolean.FALSE;
    
    // Main
    RelativeLayout shSuggestedContainer;
    TextView shInfo, shSuggested;
    TextClock shClock, shDate;
    InputBuffer shSTDIN;

    // Shell
    String shPrompt;
    Typeface tf;

    List<InputCommand> commandList;
    List<InputCommand> suggestionList;

    Map<String, String> ignoreapps;
    Map<String, Integer> appUsage;

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
	        appsReady = Boolean.FALSE;
            setupAppMaps();
	        appsReady = Boolean.TRUE;

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

        //setupAppMaps();
        // or async:
        AppFetcher appFetcher = new AppFetcher();
        appFetcher.start();

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
        shPref = getPreferences(MODE_PRIVATE);
        shPrefEditor = shPref.edit();
        for (InputCommand ic : commandList) {
            shPrefEditor.putInt(ic.appName, appUsage.get(ic.appName));
        }
        shPrefEditor.apply();
        shPrefEditor = null;
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
        // TypefaceUtil.overrideFont(getApplicationContext(), "SERIF", "fonts/your_font_file.ttf");
        tf = Typeface.createFromAsset(getAssets(), shPref.getString("font", defaultFont));
        shSTDIN = findViewById(R.id.shSTDIN);
        shInfo = findViewById(R.id.subinf);
        shClock = findViewById(R.id.shlock);
        shDate = findViewById(R.id.shdate);
        shSuggested = findViewById(R.id.op);
        shSuggestedContainer = findViewById(R.id.shsuggest_container);

        shSTDIN.setTypeface(tf);
        shInfo.setTypeface(tf);
        shClock.setTypeface(tf);
        shDate.setTypeface(tf);
        shSuggested.setTypeface(tf);

        shPrompt = getPrompt();
        shSTDIN.setText(shPrompt);
        TextView shSuggestedOffset = findViewById(R.id.suggest_offset);
        shSuggestedOffset.setText(shPrompt);
        shSuggestedOffset.setTypeface(tf);
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

        commandList = new ArrayList<>();

        // built ins
        commandList.add(
                new InputCommand(
                        "url",
                        "(url) (https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?:\\/\\/(?:www\\.|(?!www))[a-zA-Z0-9]\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})",
                        "url <url>",
                        Boolean.TRUE
                )
        );
        commandList.add(
                new InputCommand(
                        "shset",
                        "(shset) ((font ((runescape)|(inconsolata)|(drucifer)|(dosvga)))|(((uname)|(hname)) \\S+))",
                        "shset <option> <value>",
                        Boolean.TRUE
                )
        );
        commandList.add(
                new InputCommand(
                        "apud",
                        "apud",
                        "apud",
                        Boolean.FALSE
                )
        );

        //aliases
        commandList.add(new InputCommand("google play store", "play"));
        commandList.add(new InputCommand("amazon shopping", "amz"));
        commandList.add(new InputCommand("file manager", "fm"));
        commandList.add(new InputCommand("url https://www.reddit.com/r/ukpolitics", "ukp"));
        commandList.add(new InputCommand("blackberry camera", "cam"));

        //android
        appUsage = new HashMap<>();
        for (int i = 0; i < lApps.size(); i++) {
            ApplicationInfo ai = lApps.get(i);
            PackageManager pm = getPackageManager();
            String label = ai.loadLabel(pm).toString().toLowerCase();
            if (ignoreapps.get(label) == null && ai.enabled && pm.getLaunchIntentForPackage(ai.packageName) != null) {
                commandList.add(new InputCommand(label, label, ai.packageName));
                if (ai.packageName.contains("clock")) {
                    clock = ai.packageName;
                }

            }
        }
        suggestionList = commandList;

        for (InputCommand ic : commandList) {
            appUsage.put(ic.appName, shPref.getInt(ic.appName, 0));
        }
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

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// UI & CONTROL /////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    void alert() {
        Log.d(TAG, "alert: ALERT");
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            v.vibrate(500);
        }
        catch (java.lang.NullPointerException ex) {/* Pass */}
    }

    void inputTextChanged(CharSequence s, int start, int before, int count) {
        suggestApps("");
        if (!appsReady) return;
        
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

            Log.d(TAG, "inputTextChanged: punch it chewey!");
            String entered = ss.substring(shPrompt.length());
            if (shSuggestedContainer.getVisibility() == View.VISIBLE && suggestionList.size() > 0) {
                Log.d(TAG, "inputTextChanged: inputcommlaunch " + entered);
                launch(suggestionList.get(suggestionList.size() - 1));
            }
            else {
                Log.d(TAG, "inputTextChanged: stringlaunch" + entered);
                launch(entered);
            }
        }
        else {
            suggestApps(s.toString().substring(shPrompt.length()));
        }
    }

    InputCommand getCommand(String input) {
        /*
        * Searches for most likely match and returns it
        * */
        Log.d(TAG, "getCommand: Matching: " + input);
        List<List<Object>> likelihoods = new ArrayList<>();
        for (InputCommand ic : commandList) {
            List<Object> lo = new ArrayList<>();
            int rv = ic.matches(input);
            if (rv == 0) continue;
            lo.add(rv);
            lo.add(ic);
            likelihoods.add(lo);
            Log.d(TAG, "getCommand: " + ic.appName + " " + Integer.toString(rv));
        }

        Collections.sort(likelihoods, new Comparator<List<Object>>() {
            @Override
            public int compare(List<Object> lhs, List<Object> rhs) {
                return Integer.compare((int)rhs.get(0), (int)lhs.get(0));
            }
        });

        if (likelihoods.size() == 0){
            return null;
        }

        Log.d(TAG, "getCommand: " + Integer.toString((int)likelihoods.get(0).get(0)) + " :: " + ((InputCommand)likelihoods.get(0).get(1)).appName);


        return (InputCommand)likelihoods.get(0).get(1);
    }

    void launch(String input) {
        try {
            launch(getCommand(input));
        }
        catch (Exception ex) {
            resetStdin();
            alert();
        }
    }

    void launch(InputCommand ic) {
        /*
         * Given an input string, determines how to launch it, then does that
         * */
        setStdin(shPrompt);
        appUsage.put(ic.appName, appUsage.get(ic.appName) + 1);

        switch (ic.appType) {
            case BUILTIN:
                switch (ic.appName) {
                    case "url":
                        shb_url(ic.getLaunchArgs());
                        break;
                    case "shset":
                        Log.d(TAG, "launch: " + ic.userInput);
                        shb_shset(ic.getLaunchArgs());
                        break;
                    case "apud":
                        shb_apmud();
                        break;
                }
                break;
            case ANDROID:
                Intent li = getPackageManager().getLaunchIntentForPackage(ic.packageName);
                startActivity(li);
                break;
            case ALIAS:
                ic = getCommand(ic.appName);
                if (ic != null) {
                    launch(ic);
                }
                else {
                    alert();
                    resetStdin();
                }
                break;
        }
    }

    void setSubInfo(){
        String version = "X.X.XXX", text = "";
        PackageInfo pInfo;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {
            // pass
        }
        text += "v" + version + "";
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
        //shExec("clock", null);
        Intent li = getPackageManager().getLaunchIntentForPackage(clock);
        startActivity(li);
    }

    public void calClicked(View v) {
        //shExec("calendar", null);
        Intent li = getPackageManager().getLaunchIntentForPackage(calendar);
        startActivity(li);
}

    void suggestApps(String entered) {
        if (entered.length() == 0) {
            shSuggestedContainer.setVisibility(View.INVISIBLE);
            shSuggested.setText("");
            return;
        }
        else{
            shSuggestedContainer.setVisibility(View.VISIBLE);
        }

        suggestionList = commandList;

        // get matching suggestions
        List<List<Object>> likelihoods = new ArrayList<>();
        for (InputCommand ic : suggestionList) {
            List<Object> lo = new ArrayList<>();
            int rv = ic.matches(entered);
            if (rv == 0) continue;
            Log.d(TAG, "suggestApps: " + ic.appName + " :: " + Integer.toString(rv) + " :: " + Integer.toString(appUsage.get(ic.appName)));
            rv += appUsage.get(ic.appName);
            lo.add(rv);
            lo.add(ic);
            likelihoods.add(lo);
        }

        Collections.sort(likelihoods, new Comparator<List<Object>>() {
            @Override
            public int compare(List<Object> lhs, List<Object> rhs) {
                return Integer.compare((int)lhs.get(0), (int)rhs.get(0));
            }
        });

        suggestionList = new ArrayList<>();
        for (List<Object> tu: likelihoods){
            suggestionList.add((InputCommand)tu.get(1));
        }

        // display
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (InputCommand ic: suggestionList) {
            if (i != 0) sb.append("<br>");
            else i++;

            sb.append(ic.getDisplayString(appUsage.get(ic.appName)));
        }
        shSuggested.setText(Html.fromHtml(sb.toString(), 0));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// SHELL STUFF //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public String getPrompt(){
        String shprompt = shPref.getString("uname", getResources().getString(R.string.uname)) + "@" + shPref.getString("hname", getResources().getString(R.string.hname)) + "> ";
        shSTDIN.minSel = shprompt.length();
        return shprompt;
    }

    public String getUserInput() {
        String fullString = shSTDIN.getText().toString();
        return fullString.substring(getPrompt().length());
    }

    public void resetStdin(){
        this.shSTDIN.setText(shPrompt, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(shPrompt.length());
    }

    public void setStdin(String ss) {
        this.shSTDIN.setText(ss, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(ss.length());
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
        Log.d(TAG, "shb_shset: IN SHSET");
        boolean fine = true;

        if (args == null){
            fine = false;
            args = new String[0];
        }
        else if (args.length == 0){fine = false;}
        else if (args.length == 1 && !args[0].equals("help")){fine = false;}
        else if (args.length > 2){fine = false;}

        for (String s : args) {
            Log.d(TAG, "shb_shset: "+s);
        }

        if (!fine){
            Log.d(TAG, "shb_shset: args not fine!");
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
