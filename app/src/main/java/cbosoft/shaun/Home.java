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
import android.os.Environment;
import android.renderscript.ScriptGroup;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.transition.AutoTransition;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

public class Home extends Activity {

    // UI
    TextView devInfo;
    TextClock Clock;
    ViewGroup rootScene;
    Scene mainScene, hiddenScene;
    Transition t = new AutoTransition();
    boolean hidden = true, devmode = false;

    // Shell
    BufferView shSTDOUT;
    InputBuffer shSTDIN;
    String shPrompt;
    String shCurrentDirectory = Environment.getRootDirectory().toString();
    Boolean redirect = false;
    ArrayList<String> reBuff = new ArrayList<>(0);
    String prevCommand = "";

    Map<String, String> ALIASES;
    Map<String, Integer> input2type;
    Map<String, String> input2andrapp;

    SharedPreferences shPref;
    SharedPreferences.Editor shPrefEditor;

    TextWatcher tWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

            if (s.length() < 1) {
                return;
            }
            else if (s.length() < shPrompt.length()){
                setStdin(shPrompt + prevCommand);
                return;
            }
            else if (count < before){
                return;
            }
            char c = s.charAt(start);
            if (c == '\n') {
                String ss = s.toString().replaceAll("\n", "");
                setStdin(ss);

                // do something with input
                String entered = ss.substring(shPrompt.length());
                prevCommand = entered;

                if (entered.startsWith("!devc_") && devmode) {
                    switch (entered) {
                        default:
                            shPrint("Unrecognised dev command: " + entered);
                            break;
                    }
                    return;
                }
                int rv = shTabComplete(entered);
                if (rv != 1) shUnimPrint(ss);
                switch (rv) {
                    case 0: // no match
                        shPrint(entered + ": command not found");
                        resetStdin();
                        break;
                    case 1: // partial match
                        // stdin is updated in 'shTabComplete'
                        // do nothing here
                        break;
                    case 2: // total match
                        // execute!
                        if (!entered.contains(" ")) {
                            shExec(entered, null);
                        }
                        else {
                            String command = entered.substring(0, entered.indexOf(" "));
                            String[] args = entered.substring(entered.indexOf(" ")).split(" ");
                            shExec(command, args);
                        }
                        break;
                    case 3: // total match
                        // execute!
                        shExec(entered, null);
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rootlayout);
        this.hideStatusBar();

        View v = findViewById(R.id.shSTDOUT);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        v.getLayoutParams().height = height / 2;

        setupPreferences();
        setupLayout();
        setupAppMaps();
        setupScenes();
        setSubInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.shPrompt = getPrompt();
        this.resetStdin();
        setSubInfo();
    }

    @Override
    protected void onPause(){
        super.onPause();
        setHidden(true);
    }

    void setHidden(boolean state) {

        if (state) {
            Log.d(TAG, "setHidden: HIDING");
            TransitionManager.go(hiddenScene, t);
        }
        else {
            Log.d(TAG, "setHidden: SHOWING");
            TransitionManager.go(mainScene, t);
        }


        hidden = state;
        setupLayout();
        setSubInfo();
        shSTDOUT.invalidate();
    }

    void setupPreferences() {
        Log.i(TAG, "SETTING UP PREFERENCES");
        shPref = getPreferences(MODE_PRIVATE);
        shPrefEditor = shPref.edit();
        defaultiseShPref("uname", "user");
        defaultiseShPref("hname", "droid");
        shPrefEditor.apply();
        shPrefEditor = null;
    }

    void setupLayout() {
        Log.i(TAG, "SETTING UP LAYOUT");
        shSTDOUT = findViewById(R.id.shSTDOUT);
        shSTDIN = findViewById(R.id.shSTDIN);
        devInfo = findViewById(R.id.subinf);
        Clock = findViewById(R.id.shlock);
        shSTDOUT.setMovementMethod(new ScrollingMovementMethod());
        shPrompt = getPrompt();
        shSTDIN.setText(shPrompt);
        shSTDIN.addTextChangedListener(tWatcher);

        shSTDIN.requestFocus();

        // Set font
        Typeface inc = Typeface.createFromAsset(getAssets(), "fonts/inconsolata.ttf");
        setDefaultTypeface(inc);
    }

    void setupAppMaps() {
        Log.i(TAG, "SETTING UP APP MAPS");
        List<ApplicationInfo> lApps;
        lApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        Map<String, String> ignoreapps = new HashMap();
        ignoreapps.put("google", "");

        input2type = new HashMap<>();
        input2type.put("ls", 2);
        input2type.put("url", 2);
        input2type.put("help", 2);
        input2type.put("cd", 2);
        input2type.put("clear", 2);
        input2type.put("hide", 2);
        input2type.put("show", 2);
        input2type.put("shset", 2);
        input2type.put("shget", 2);
        input2type.put("apm-list", 2);
        input2type.put("grep", 2);

        ALIASES = new HashMap<>();
        ALIASES.put("play", "google play store");
        ALIASES.put("amazon", "amazon shopping");
        ALIASES.put("fm", "file manager");
        ALIASES.put("cal", "calendar");
        ALIASES.put("ukp", "url https://www.reddit.com/r/ukpolitics");
        ALIASES.put("cls", "clear");

        input2andrapp = new HashMap<>();
        for (int i = 0; i < lApps.size(); i++) {
            ApplicationInfo ai = lApps.get(i);
            String label = ai.loadLabel(getPackageManager()).toString().toLowerCase();
            if (ignoreapps.get(label) == null && ai.enabled) {
                input2andrapp.put(label, ai.packageName);
                input2type.put(label, 1);
            }
        }
    }

    void setupScenes() {
        Log.i(TAG, "SETTING UP SCENES");
        // init scenes
        rootScene = findViewById(R.id.root);
        hiddenScene = Scene.getSceneForLayout(rootScene, R.layout.hidden, this);
        mainScene = Scene.getSceneForLayout(rootScene, R.layout.main, this);
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

        String bestMatchName = "";
        int bestMatchLength = 0;

        String aliasres = ALIASES.get(command);
        if (aliasres != null) {
            setStdin(shPrompt + aliasres);
            return 1;
        }

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


        for (String k: input2type.keySet()) {
            Log.d(TAG, "shTabComplete: " + command + " " + k);
            if (entered.toLowerCase().equals(k)) {
                // this should never be called: should be caught earlier
                return 2;
            }
            else {
                String sub_entered;
                for (int j = 0; j < entered.length(); j++) {
                    sub_entered = entered.substring(0, entered.length() - j).toLowerCase();
                    Log.d(TAG, "shTabComplete: " + sub_entered);
                    if (k.toLowerCase().startsWith(sub_entered)) {
                        if (entered.length() - j > bestMatchLength) {
                            bestMatchName = k;
                            bestMatchLength = entered.length() - j;
                            Log.d(TAG, "shTabComplete: potential match: " + k);
                            break;
                        }
                    }
                }
            }
        }

        if (bestMatchLength > 0) {
            //shPrint("non-unique, best match found");
            setStdin(shPrompt + bestMatchName);
            return 1;
        }

        return 0;
    }

    void hideStatusBar(){
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    void refreshUI(){
        shPrompt = getPrompt();
        setStdin(shPrompt);
        setSubInfo();
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

    void setSubInfo(){
        PackageInfo pInfo;
        String version = "vXX";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        }
        catch (Exception e) {
            // pass
        }
        String text = "";
        text += "shaun v" + version + "\n";
        text += "android: " + Build.VERSION.RELEASE + "\n";
        String[] dirs = shCurrentDirectory.split("/");
        if (dirs.length > 2) {
            text += "[.../" + dirs[dirs.length - 1] + "]";
        }
        else {
            text += "[" + shCurrentDirectory + "]";
        }
        devInfo.setText(text);
    }

    void setDefaultTypeface(Typeface tf){
        ViewGroup viewgroup = findViewById(R.id.parv);
        for (int i = 0; i < viewgroup.getChildCount(); i++) {
            View v1 = viewgroup.getChildAt(i);
            try{
                TextView tv = (TextView) v1;
                tv.setTypeface(tf);
            }
            catch (Exception e) {
                try{
                    InputBuffer et = (InputBuffer) v1;
                    et.setTypeface(tf);
                }
                catch (Exception ee){
                    try{
                        TextClock tc = (TextClock) v1;
                        tc.setTypeface(tf);
                    }
                    catch (Exception eee){
                        // not a textview, edittext, or textclock
                    }
                }
            }
        }
    }

    public void resetStdin(){
        this.shSTDIN.setText(shPrompt, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(shPrompt.length());
    }

    public void setStdin(String ss) {
        this.shSTDIN.setText(ss, TextView.BufferType.EDITABLE);
        this.shSTDIN.setSelection(ss.length());
    }

    public String getPrompt(){
        String shprompt = shPref.getString("uname", getResources().getString(R.string.uname)) + "@" + shPref.getString("hname", getResources().getString(R.string.hname)) + "> ";
        shSTDIN.minSel = shprompt.length();
        return shprompt;
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

    public void clockClicked(View v) {
        shExec("clock", null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// SHELL STUFF //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    void shPrint(String toPrint) {
        shPrint(toPrint, true);
    }

    void shUnimPrint(String toPrint) {
        shPrint(toPrint, false);
    }

    void shPrint(String toPrint, boolean isImportant) {
        if (redirect) {
            reBuff.add(toPrint);
        }
        else {
            if (hidden && isImportant) setHidden(false);
            shSTDOUT.addToBuffer(toPrint);
        }
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
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals("|")) {
                        if (args[i + 1].equals("grep")) {
                            // pipe output
                            // <cmnd> <cargs> | grep <pattern>
                            // to
                            // grep <pattern> <cmnd> <cargs>
                            String nc = "grep";
                            String[] na = new String[args.length - 1];
                            na[0] = args[i + 2]; // pattern
                            na[1] = command; // command
                            System.arraycopy(args, 0, na, 2, args.length - 3);
//                            for (int j = 1; j < i; j++) {
//                                Log.d(TAG, "shExec: argsj: " + args[j]);
//                                na[j + 1] = args[j];
//                            }

                            command = nc;
                            args = na;

                            Log.d(TAG, "shExec: NC: " + nc);
                            for (String s: na) {
                                Log.d(TAG, "shExec:  ARGS:  " + s);
                            }
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "shExec: NOT GREP");
            }

            switch (command) {
                case "ls":
                    shb_ls(args);
                    break;
                case "url":
                    shb_url(args[0]);
                    break;
                case "help":
                    shb_help();
                    break;
                case "clear":
                    shb_clear();
                    break;
                case "hide":
                    setHidden(true);
                    break;
                case "show":
                    setHidden(false);
                    break;
                case "cd":
                    shb_cd(args);
                    break;
                case "shset":
                    shb_shset(args);
                    break;
                case "shget":
                    shb_shget(args);
                    break;
                case "grep":
                    shb_grep(args);
                    break;
                case "apm-list":
                    shb_apm_list(args);
                    break;
                default:
                    shPrint("Command " + command + " not found.");
                    break;

            }
        }
    }

    void shb_url(String url) {
        if (!url.startsWith("https://") && !url.startsWith("http://")){
            url = "http://" + url;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    void shb_ls(String[] args) {

        File directory;

        if (args != null){
            directory = new File(args[1]);
        }
        else {
            directory = new File(shCurrentDirectory);
        }

        File[] files = directory.listFiles();

        String prefx = "";
        for (File f: files){

            if (f.isDirectory()){
                prefx = " D ";
            }
            else {
                prefx = " F ";
            }

            shPrint(prefx + f.getName());
        }

    }

    void shb_cd(String[] args) {
        // change directory
        if (args.length < 1){
            // Changing to current directory... ?
            return;
        }

        String newpath = shCurrentDirectory + "/" + args[0];
        Log.d(TAG, "shb_cd: CHANGING DIR TO " + newpath);
        String[] npspl = newpath.split("/");
        newpath = npspl[0];
        for (int i = 1; i < npspl.length - 1; i++){
            if (!npspl[i + 1].equals("..")){
                newpath += "/" + npspl[i];
            }
        }
        if (!npspl[npspl.length - 1].equals("..")){newpath += "/" + npspl[npspl.length - 1]; }
        if (newpath.equals("")){
            shPrint("Lack permissions to access root directory.");
            return;
        }
        File newdirectory = new File(newpath);

        if (newdirectory.isDirectory()) {
            shCurrentDirectory = newdirectory.getAbsolutePath();
            setSubInfo();
        }
        else {
            shPrint(args[1] + " is not a directory.");
        }
    }

    void shb_clear() {
        shSTDOUT.buffer.clear();
        shSTDOUT.refreshFromBuffer();
    }

    void shb_help() {
        shPrint("SHAUN is a trimmed-down implementation of a terminal emulator. Given the security limitations (sensibly) enforced on Android apps, this is not a fully featured terminal.");
        shPrint("The goal is to have as close to a POSIX complete terminal as is possible, as well as an extension API for the user to add their own apps to the \"shell's\" path.");

    }

    void shb_shset(String[] args) {

        boolean fine = true;

        if (args == null){fine = false;}
        else if (args.length == 0){fine = false;}
        else if (args.length == 1 && !args[0].equals("help")){fine = false;}
        else if (args.length > 2){fine = false;}

        if (!fine){
            shPrint("Syntax error. Correct syntax is:");
            shPrint("  shset <key> <value>");
            shPrint("  shset help");
        }
        else if (args.length == 1 && args[0].equals("help")) {
            shPrint("shset: shaun launcher settings manager.");
            shPrint("\nUsage:");
            shPrint("  shset <key> <value>");
            shPrint("  shset help");
        }
        else {
            shPrint("Set:");
            shPrint("  " + args[0] + " : " + args[1]);
            shPrefEditor.putString(args[0], args[1]);
            shPrefEditor.commit();
            refreshUI();
        }
    }

    void shb_shget(String[] args) {

        boolean fine = true;

        if (args == null){fine = false;}
        else if (args.length == 0){fine = false;}
        else if (args.length > 1){fine = false;}

        if (!fine){
            shPrint("Syntax error. Correct syntax is:");
            shPrint("  shget <key>");
            shPrint("  shget help");
        }
        else if (args.length == 1 && args[0].equals("help")) {
            shPrint("shget: shaun launcher settings manager.");
            shPrint("\nUsage:");
            shPrint("  shget <key>");
            shPrint("  shget help");
        }
        else {
            shPrint("Get:");
            shPrint("  " + args[0] + " : " + shPref.getString(args[0], "NOT_FOUND"));
        }
    }

    void shb_grep(String[] args) {

        // grep <pattern> <command> <*args>

        String[] sArgs;

        if (args.length > 2) {
            sArgs = new String[args.length - 2];
            System.arraycopy(args, 2, sArgs, 0, args.length - 2);
        }
        else {
            sArgs = null;
        }

        redirect = true;
        shExec(args[1], sArgs);
        redirect = false;

        ArrayList<String> oBuff = new ArrayList<>(0);

        for (String s: reBuff) {
            if (Pattern.compile(args[0]).matcher(s).find()) {
                oBuff.add(s);
            }
        }

        reBuff.clear();

        for (String s: oBuff) {
            shPrint(s);
        }
    }

    void shb_apm_list(String[] args) {
        List<ApplicationInfo> lApps;
        lApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        boolean lowercasify = false, orderify = true, detailed = false;

        if (args != null){

            for (String a: args){
                if (a.contains("l")) {
                    lowercasify = true;
                }

                if (a.contains("u")) {
                    orderify = true;
                }

                if (a.contains("d")) {
                    detailed = true;
                }
            }
        }

        shPrint("Installed packages:");
        ArrayList<String> appNameList = new ArrayList<>(0);

        for (ApplicationInfo ai: lApps) {
            String appName = ai.loadLabel(getPackageManager()).toString();
            if (lowercasify) appName = appName.toLowerCase();

            if (detailed){
                String enabledIndicator = "";
                if (!ai.enabled) enabledIndicator = " [DISABLED]";
                appName = appName + enabledIndicator;
            }
            appNameList.add(appName);
        }

        //appNameList.sort(String::compareToIgnoreCase);
        if (orderify) {
            Collections.sort(appNameList, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    return s1.compareToIgnoreCase(s2);
                }
            });
        }

        for (String s: appNameList) {
            shPrint(s);
        }
    }

    /*
    * TODO:
    *
    *   - extend reach of settings apps:
    *       - theming
    *       - fontsize
    *       - stuff like that
    *   - Find better font for clock...
    * */


    /*
    *   KNOWN BUGS:
    *
    *   - When launching "PIXEL LAUNCHER" from SHAUN, bugs out
    *
    *       - "java.lang.NullPointerException: Attempt to invoke virtual method
    *         'boolean android.content.Intent.migrateExtraStreamToClipData()'
    *         on a null object reference"
    *
    *       - Just seems to be pixel launcher, not noticed it with other apps
    *         (maybe its just for homescreen apps?)
    *
    *   - When launching a verbose command (i.e. one that produces lots of shell output like "ls"),
    *     upon unhiding UI, the command (added to buffer via shUnimPrint(s)) is not in the buffer.
    *
    *       - Possibly due to different scenes having different instances of the objects?
    * */
}
