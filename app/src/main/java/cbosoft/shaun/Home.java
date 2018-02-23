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
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.os.Build;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

public class Home extends Activity {

    // UI
    TextView devInfo;
    TextClock Clock;

    // Shell
    BufferView shSTDOUT;
    EditText shSTDIN;
    String shPrompt;
    String shCurrentDirectory = Environment.getRootDirectory().toString();
    Boolean redirect = false;
    ArrayList<String> reBuff = new ArrayList<>(0);
    String prevCommand = "";

    int shBufHist = 1000;
    Map<String, String> ALIASES;
    Map<String, Integer> input2type;
    Map<String, String> input2andrapp;

    SharedPreferences shPref;
    SharedPreferences.Editor shPrefEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        this.hideStatusBar();

        Log.d(TAG, "SETTING UP SHAREDPREFS");
        shPref = getPreferences(MODE_PRIVATE);
        shPrefEditor = shPref.edit();
        defaultiseShPref("uname", "user");
        defaultiseShPref("hname", "droid");
//        defaultiseShPref("uname", "user");
//        defaultiseShPref("uname", "user");
//        defaultiseShPref("uname", "user");
//        defaultiseShPref("uname", "user");
//        defaultiseShPref("uname", "user");

        this.shPrompt = getPrompt();

        shSTDOUT = findViewById(R.id.shSTDOUT);
        shSTDIN = findViewById(R.id.shSTDIN);
        devInfo = findViewById(R.id.subinf);
        Clock = findViewById(R.id.shlock);

        shSTDOUT.setMovementMethod(new ScrollingMovementMethod());

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
                    int rv = shTabComplete(entered);
                    switch (rv) {
                        case 0: // no match
                            shPrint(s.toString());
                            shPrint(entered + ": command not found");
                            resetStdin();
                            break;
                        case 1: // partial match
                            // stdin is updated in 'shTabComplete'
                            // do nothing here
                            break;
                        case 2: // total match
                            // execute!
                            shPrint(ss);
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
                            shPrint(ss);
                            shExec(entered, null);
                            break;
                    }
                }
            }
        };
        shSTDIN.addTextChangedListener(tWatcher);

        // Set font
        Typeface inc = Typeface.createFromAsset(getAssets(), "fonts/inconsolata.ttf");
        setDefaultTypeface(inc);

        // Get list of installed apps:
        List<ApplicationInfo> lApps;
        lApps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);

        Log.d(TAG, "SETTING UP APPMAPS");
        input2andrapp = new HashMap<>();
        input2type = new HashMap<>();
        // add builtins to type map
        // android apps will be added automatically later
        input2type.put("ls", 2);
        input2type.put("url", 2);
        input2type.put("help", 2);
        input2type.put("cd", 2);
        input2type.put("clear", 2);
        input2type.put("shset", 2);
        input2type.put("shget", 2);
        input2type.put("grep", 2);

        Log.d(TAG, "SETTING UP ALIASES");
        ALIASES = new HashMap<>();
        // android apps
        ALIASES.put("play", "google play store");
        ALIASES.put("amazon", "amazon shopping");
        ALIASES.put("ukp", "url https://www.reddit.com/r/ukpolitics");
        // builtins
        ALIASES.put("cls", "clear");

        for (int i = 0; i < lApps.size(); i++) {
            ApplicationInfo ai = lApps.get(i);
            input2andrapp.put(ai.loadLabel(getPackageManager()).toString().toLowerCase(), ai.packageName);
            input2type.put(ai.loadLabel(getPackageManager()).toString().toLowerCase(), 1);
        }



        setSubInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.shPrompt = getPrompt();
        this.resetStdin();
        setSubInfo();
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

        Object res = input2type.get(command.toLowerCase());
        if (res != null) {
            // android app or built in app, with args
            return 2;
        }

        res = input2type.get(entered.toLowerCase());
        if (res != null) {
            // android app or built in app, no args
            return 3;
        }


        for (String k: input2type.keySet()) {
            Log.d(TAG, "shTabComplete: " + command + " " + k);
            if (entered.toLowerCase().equals(k)) {
                // this should never be called: should be caught earlier
                return 2;
            }
            else {
                String sub_entered = "";
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
        if (res == "unset") {
            shPrefEditor.putString(key, defval);
            shPrefEditor.commit();
        }
    }

    void setSubInfo(){
        PackageInfo pInfo;
        String version = "vXX";
        int code = -1;
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
                    EditText et = (EditText) v1;
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
        return shPref.getString("uname", getResources().getString(R.string.uname)) + "@" +
                shPref.getString("hname", getResources().getString(R.string.hname)) + "> ";
    }

    String[] trimArr(String[] toTrim) {
        ArrayList<String> tt = new ArrayList<>(Arrays.asList(toTrim));

        for (int i = 0; i < toTrim.length; i++){
            int j = toTrim.length - i - 1;
            if (toTrim[j].equals("")){
                tt.remove(j);
            }
        }

        return tt.toArray(new String[tt.size()]);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///// SHELL STUFF //////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    void shPrint(String toPrint) {
        if (redirect) {
            reBuff.add(toPrint);
        }
        else {
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
                            String nc = "grep";
                            String[] na = new String[args.length - 1];
                            na[0] = args[i + 2]; // pattern
                            na[1] = command; // command

                            for (int j = 1; j < i; j++) {
                                Log.d(TAG, "shExec: argsj: " + args[j]);
                                na[j + 1] = args[j];
                            }

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

        if (args.length < 2){ return; }

        String newpath = shCurrentDirectory + "/" + args[1];
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
        shSTDOUT.setText("");
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

    /*
    * Features to implement:
    *
    *   - input history
    *       - as per usual, accessible via the up/down arrow keys
    *       - on mobile, or with limited keyboard, previous command accessible by pressing backspace at an empty prompt
    *   - extend reach of settings apps:
    *       - theming
    *       - fontsize
    *       - stuff like that
    * */


    /*
    *   BUGS:
    *
    *   - When launching "PIXEL LAUNCHER" from SHAUN, bugs out
    *       - "java.lang.NullPointerException: Attempt to invoke virtual method 'boolean android.content.Intent.migrateExtraStreamToClipData()' on a null object reference"
    *       - Just seems to be pixel launcher, not noticed it with other apps (maybe its just for homescreen apps?)
    * */
}
